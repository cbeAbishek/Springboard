package org.automation.dashboard.controller;

import org.automation.analytics.model.ExecutionLog;
import org.automation.analytics.repo.ExecutionLogRepository;
import org.automation.reports.model.TestReport;
import org.automation.reports.repository.TestReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class DashboardRestController {

    @Autowired(required = false)
    private TestReportRepository testReportRepository;

    @Autowired(required = false)
    private ExecutionLogRepository executionLogRepository;

    // In-memory execution tracking
    private static final Map<String, ExecutionStatusTracker> activeExecutions = new ConcurrentHashMap<>();
    private static final Map<String, Process> executionProcesses = new ConcurrentHashMap<>();

    // Inner class to track execution status
    private static class ExecutionStatusTracker {
        String executionId;
        String status;
        String suite;
        int passed;
        int failed;
        int skipped;
        int total;
        long startTime;
        String currentTest;

        ExecutionStatusTracker(String executionId, String suite) {
            this.executionId = executionId;
            this.suite = suite;
            this.status = "RUNNING";
            this.startTime = System.currentTimeMillis();
            this.passed = 0;
            this.failed = 0;
            this.skipped = 0;
            this.total = 0;
            this.currentTest = "Initializing...";
        }

        int getProgress() {
            if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
                return 100;
            }
            int completed = passed + failed + skipped;
            return total > 0 ? (completed * 100) / total : 0;
        }

        double getDuration() {
            return (System.currentTimeMillis() - startTime) / 1000.0;
        }
    }

    @GetMapping("/generate-allure-report")
    public Map<String, Object> generateAllureReport() {
        Map<String, Object> response = new HashMap<>();

        try {
            File allureResults = new File("allure-results");
            if (!allureResults.exists() || allureResults.listFiles() == null || allureResults.listFiles().length == 0) {
                response.put("success", false);
                response.put("message", "No allure-results found. Run tests first to generate results.");
                return response;
            }

            // Check if allure command exists
            ProcessBuilder checkAllure = new ProcessBuilder("which", "allure");
            Process checkProcess = checkAllure.start();
            int checkResult = checkProcess.waitFor();

            if (checkResult != 0) {
                response.put("success", false);
                response.put("message", "Allure CLI not installed. Generating report using Maven plugin...");

                // Try using Maven allure plugin
                ProcessBuilder mavenAllure = new ProcessBuilder("mvn", "allure:report");
                mavenAllure.redirectErrorStream(true);
                Process mvnProcess = mavenAllure.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(mvnProcess.getInputStream()));
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }

                int mvnResult = mvnProcess.waitFor();
                if (mvnResult == 0) {
                    response.put("success", true);
                    response.put("message", "Allure report generated successfully using Maven");
                } else {
                    response.put("success", false);
                    response.put("message", "Failed to generate report. Please install Allure CLI.");
                    response.put("output", output.toString());
                }
                return response;
            }

            // Generate report using Allure CLI
            ProcessBuilder pb = new ProcessBuilder("allure", "generate", "allure-results", "-o", "allure-report", "--clean");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                response.put("success", true);
                response.put("message", "Allure report generated successfully");
                response.put("reportUrl", "/allure-report/index.html");
            } else {
                response.put("success", false);
                response.put("message", "Failed to generate Allure report");
                response.put("output", output.toString());
            }

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error generating report: " + e.getMessage());
            e.printStackTrace();
        }

        return response;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return response;
    }

    @GetMapping("/execute-tests")
    public Map<String, Object> executeTests(
            @RequestParam String suite,
            @RequestParam String environment,
            @RequestParam(required = false, defaultValue = "chrome") String browser,
            @RequestParam(required = false, defaultValue = "false") boolean parallel,
            @RequestParam(required = false, defaultValue = "1") int threads) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Generate unique report ID
            String reportId = generateReportId();

            // Create execution tracker
            ExecutionStatusTracker tracker = new ExecutionStatusTracker(reportId, suite);
            activeExecutions.put(reportId, tracker);

            // Create test report entry in database
            if (testReportRepository != null) {
                TestReport report = new TestReport(reportId, suite + " Test Execution", mapSuiteType(suite));
                report.setStatus("RUNNING");
                report.setBrowser(browser);
                report.setEnvironment(environment);
                report.setCreatedBy("UI");
                report.setTriggerType("MANUAL");
                testReportRepository.save(report);
            }

            // Build Maven command
            List<String> command = new ArrayList<>();
            command.add("mvn");
            command.add("clean");
            command.add("test");
            command.add("-Dsuite=" + suite);
            command.add("-Denvironment=" + environment);
            command.add("-Dbrowser=" + browser);
            command.add("-Dreport.created.by=UI");
            command.add("-Dreport.trigger.type=MANUAL");
            command.add("-Dreport.id=" + reportId);
            command.add("-Dspring.profiles.active=test");

            if (parallel) {
                command.add("-Dparallel=true");
                command.add("-Dthreads=" + threads);
            }

            // Execute tests in background
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(System.getProperty("user.dir")));
            pb.redirectErrorStream(true);
            Process process = pb.start();
            executionProcesses.put(reportId, process);

            // Monitor execution in background thread
            monitorExecution(reportId, process, tracker);

            response.put("success", true);
            response.put("message", "Test execution started");
            response.put("reportId", reportId);
            response.put("suite", suite);
            response.put("environment", environment);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to start test execution: " + e.getMessage());
            e.printStackTrace();
        }

        return response;
    }

    private void monitorExecution(String executionId, Process process, ExecutionStatusTracker tracker) {
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;

                while ((line = reader.readLine()) != null) {
                    System.out.println(line); // Print to console

                    // Parse TestNG results - look for the summary line
                    if (line.contains("Tests run:")) {
                        try {
                            String[] parts = line.split(",");
                            for (String part : parts) {
                                part = part.trim();
                                if (part.startsWith("Tests run:")) {
                                    tracker.total = Integer.parseInt(part.split(":")[1].trim());
                                } else if (part.startsWith("Failures:")) {
                                    tracker.failed = Integer.parseInt(part.split(":")[1].trim());
                                } else if (part.startsWith("Errors:")) {
                                    int errors = Integer.parseInt(part.split(":")[1].trim());
                                    tracker.failed += errors;
                                } else if (part.startsWith("Skipped:")) {
                                    tracker.skipped = Integer.parseInt(part.split(":")[1].trim());
                                }
                            }
                            tracker.passed = tracker.total - tracker.failed - tracker.skipped;
                        } catch (Exception e) {
                            System.err.println("Error parsing test results: " + e.getMessage());
                        }
                    }

                    // Detect current test
                    if (line.contains("Running") && line.contains("org.automation")) {
                        String testClass = line.substring(line.indexOf("org.automation"));
                        tracker.currentTest = testClass;
                    }
                }

                // Wait for process to complete
                int exitCode = process.waitFor();

                // Try to get final results from ReportManager's generated report
                updateTrackerFromReportFiles(executionId, tracker);

                // Update final status
                tracker.status = (exitCode == 0 && tracker.failed == 0) ? "COMPLETED" : "FAILED";

                // Update database if available
                updateDatabaseReport(executionId, tracker);

                // Clean up after 5 minutes
                scheduleCleanup(executionId);

            } catch (Exception e) {
                tracker.status = "FAILED";
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Update tracker from report files when database is not available
     */
    private void updateTrackerFromReportFiles(String executionId, ExecutionStatusTracker tracker) {
        try {
            // First check if we can find the report by scanning report directories
            File reportsDir = new File("artifacts/reports");
            if (reportsDir.exists()) {
                File[] reportDirs = reportsDir.listFiles(File::isDirectory);
                if (reportDirs != null) {
                    // Find the most recent report directory
                    File latestReport = null;
                    long latestTime = 0;

                    for (File dir : reportDirs) {
                        if (dir.lastModified() > latestTime) {
                            latestTime = dir.lastModified();
                            latestReport = dir;
                        }
                    }

                    if (latestReport != null) {
                        File summaryFile = new File(latestReport, "summary.html");
                        if (summaryFile.exists()) {
                            String content = new String(java.nio.file.Files.readAllBytes(summaryFile.toPath()));

                            // Parse HTML to extract statistics
                            if (content.contains("Total Tests")) {
                                // Extract total tests
                                String totalPattern = "<div class='stat-value'>";
                                int totalIndex = content.indexOf(totalPattern);
                                if (totalIndex > 0) {
                                    totalIndex += totalPattern.length();
                                    int endIndex = content.indexOf("</div>", totalIndex);
                                    String totalStr = content.substring(totalIndex, endIndex).trim();
                                    try {
                                        tracker.total = Integer.parseInt(totalStr);
                                    } catch (NumberFormatException e) {
                                        // Ignore
                                    }
                                }

                                // Extract passed tests
                                int passedIndex = content.indexOf("<div class='stat-value pass'>");
                                if (passedIndex > 0) {
                                    passedIndex += "<div class='stat-value pass'>".length();
                                    int endIndex = content.indexOf("</div>", passedIndex);
                                    String passedStr = content.substring(passedIndex, endIndex).trim();
                                    try {
                                        tracker.passed = Integer.parseInt(passedStr);
                                    } catch (NumberFormatException e) {
                                        // Ignore
                                    }
                                }

                                // Extract failed tests
                                int failedIndex = content.indexOf("<div class='stat-value fail'>");
                                if (failedIndex > 0) {
                                    failedIndex += "<div class='stat-value fail'>".length();
                                    int endIndex = content.indexOf("</div>", failedIndex);
                                    String failedStr = content.substring(failedIndex, endIndex).trim();
                                    try {
                                        tracker.failed = Integer.parseInt(failedStr);
                                    } catch (NumberFormatException e) {
                                        // Ignore
                                    }
                                }

                                // Extract skipped tests
                                int skippedIndex = content.indexOf("<div class='stat-value skip'>");
                                if (skippedIndex > 0) {
                                    skippedIndex += "<div class='stat-value skip'>".length();
                                    int endIndex = content.indexOf("</div>", skippedIndex);
                                    String skippedStr = content.substring(skippedIndex, endIndex).trim();
                                    try {
                                        tracker.skipped = Integer.parseInt(skippedStr);
                                    } catch (NumberFormatException e) {
                                        // Ignore
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading report files: " + e.getMessage());
        }
    }

    /**
     * Update database report with final results
     */
    private void updateDatabaseReport(String executionId, ExecutionStatusTracker tracker) {
        if (testReportRepository != null) {
            try {
                Optional<TestReport> reportOpt = testReportRepository.findByReportId(executionId);
                if (reportOpt.isPresent()) {
                    TestReport report = reportOpt.get();
                    report.setStatus(tracker.status);
                    report.setTotalTests(tracker.total);
                    report.setPassedTests(tracker.passed);
                    report.setFailedTests(tracker.failed);
                    report.setSkippedTests(tracker.skipped);
                    report.setDurationMs((long) (tracker.getDuration() * 1000));
                    report.setSuccessRate(tracker.total > 0 ? (tracker.passed * 100.0) / tracker.total : 0);
                    report.setUpdatedAt(LocalDateTime.now());
                    testReportRepository.save(report);
                }
            } catch (Exception e) {
                System.err.println("Error updating database report: " + e.getMessage());
            }
        }
    }

    /**
     * Schedule cleanup of execution tracker
     */
    private void scheduleCleanup(String executionId) {
        new Thread(() -> {
            try {
                Thread.sleep(5 * 60 * 1000); // 5 minutes
                activeExecutions.remove(executionId);
                executionProcesses.remove(executionId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    @GetMapping("/test-suites")
    public Map<String, Object> getTestSuites() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<Map<String, Object>> suites = new ArrayList<>();

            // Get real test statistics from database
            if (testReportRepository != null) {
                // API Test Suite
                List<TestReport> apiReports = testReportRepository.findBySuiteType("API");
                suites.add(createSuiteInfo("API Test Suite", "API", apiReports));

                // UI Test Suite
                List<TestReport> uiReports = testReportRepository.findBySuiteType("UI");
                suites.add(createSuiteInfo("UI Test Suite", "UI", uiReports));

                // Integration Tests
                List<TestReport> integrationReports = testReportRepository.findBySuiteType("Integration");
                suites.add(createSuiteInfo("Integration Tests", "Integration", integrationReports));

                // Regression Suite
                List<TestReport> regressionReports = testReportRepository.findBySuiteType("Regression");
                suites.add(createSuiteInfo("Regression Suite", "Regression", regressionReports));

                // Smoke Tests
                List<TestReport> smokeReports = testReportRepository.findBySuiteType("Smoke");
                suites.add(createSuiteInfo("Smoke Test Suite", "Smoke", smokeReports));
            } else {
                // Fallback: scan test files
                suites.add(createFallbackSuite("API Test Suite", "API", "org/automation/api"));
                suites.add(createFallbackSuite("UI Test Suite", "UI", "org/automation/ui"));
                suites.add(createFallbackSuite("Integration Tests", "Integration", "org/automation/integration"));
                suites.add(createFallbackSuite("Regression Suite", "Regression", "org/automation/regression"));
                suites.add(createFallbackSuite("Smoke Test Suite", "Smoke", "org/automation/smoke"));
            }

            response.put("success", true);
            response.put("suites", suites);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error fetching test suites: " + e.getMessage());
            e.printStackTrace();
        }

        return response;
    }

    @GetMapping("/recent-executions")
    public Map<String, Object> getRecentExecutions(@RequestParam(defaultValue = "10") int limit) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<Map<String, Object>> executions = new ArrayList<>();

            if (testReportRepository != null) {
                List<TestReport> reports = testReportRepository.findAll()
                        .stream()
                        .sorted((r1, r2) -> r2.getExecutionDate().compareTo(r1.getExecutionDate()))
                        .limit(limit)
                        .collect(Collectors.toList());

                for (TestReport report : reports) {
                    Map<String, Object> exec = new HashMap<>();
                    exec.put("id", report.getReportId());
                    exec.put("suite", report.getSuiteType() + " Test Suite");
                    exec.put("status", report.getStatus().toLowerCase());
                    exec.put("passed", report.getPassedTests());
                    exec.put("failed", report.getFailedTests());
                    exec.put("total", report.getTotalTests());
                    exec.put("duration", formatDuration(report.getDurationMs()));
                    exec.put("date", getRelativeTime(report.getExecutionDate()));
                    executions.add(exec);
                }
            }

            response.put("success", true);
            response.put("executions", executions);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error fetching executions: " + e.getMessage());
            e.printStackTrace();
        }

        return response;
    }

    @GetMapping("/execution-status")
    public Map<String, Object> getExecutionStatus(@RequestParam String executionId) {
        Map<String, Object> response = new HashMap<>();

        try {
            // First check in-memory tracker for active executions
            ExecutionStatusTracker tracker = activeExecutions.get(executionId);

            if (tracker != null) {
                // Active execution - return real-time data
                response.put("success", true);
                response.put("status", tracker.status);
                response.put("passed", tracker.passed);
                response.put("failed", tracker.failed);
                response.put("skipped", tracker.skipped);
                response.put("total", tracker.total);
                response.put("duration", tracker.getDuration());
                response.put("progress", tracker.getProgress());
                response.put("currentTest", tracker.currentTest);
            } else if (testReportRepository != null) {
                // Check database for completed executions
                Optional<TestReport> reportOptional = testReportRepository.findByReportId(executionId);

                if (reportOptional.isPresent()) {
                    TestReport report = reportOptional.get();
                    response.put("success", true);
                    response.put("status", report.getStatus());
                    response.put("passed", report.getPassedTests());
                    response.put("failed", report.getFailedTests());
                    response.put("skipped", report.getSkippedTests());
                    response.put("total", report.getTotalTests());
                    response.put("duration", report.getDurationMs() / 1000.0);
                    response.put("progress", calculateProgress(report));
                    response.put("currentTest", "Completed");
                } else {
                    response.put("success", false);
                    response.put("message", "Execution not found");
                }
            } else {
                response.put("success", false);
                response.put("message", "Execution not found");
            }

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error fetching status: " + e.getMessage());
        }

        return response;
    }

    @GetMapping("/stop-execution")
    public Map<String, Object> stopExecution(@RequestParam String executionId) {
        Map<String, Object> response = new HashMap<>();

        try {
            Process process = executionProcesses.get(executionId);
            ExecutionStatusTracker tracker = activeExecutions.get(executionId);

            if (process != null && process.isAlive()) {
                process.destroy();
                if (tracker != null) {
                    tracker.status = "STOPPED";
                }
                response.put("success", true);
                response.put("message", "Test execution stopped");
            } else {
                response.put("success", false);
                response.put("message", "Execution not found or already completed");
            }

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error stopping execution: " + e.getMessage());
        }

        return response;
    }

    private String generateReportId() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.format.DateTimeFormatter formatter =
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        return "RPT_" + now.format(formatter) + "_" + java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    private Map<String, Object> createSuiteInfo(String name, String type, List<TestReport> reports) {
        Map<String, Object> suite = new HashMap<>();
        suite.put("name", name);
        suite.put("type", type);

        if (reports.isEmpty()) {
            suite.put("count", 0);
            suite.put("lastRun", "Never");
            suite.put("status", "unknown");
            suite.put("successRate", 0.0);
        } else {
            TestReport latest = reports.get(0);
            suite.put("count", latest.getTotalTests());
            suite.put("lastRun", getRelativeTime(latest.getExecutionDate()));
            suite.put("status", latest.getFailedTests() == 0 ? "passed" : "failed");
            suite.put("successRate", latest.getSuccessRate());
        }

        return suite;
    }

    private Map<String, Object> createFallbackSuite(String name, String type, String packagePath) {
        Map<String, Object> suite = new HashMap<>();
        suite.put("name", name);
        suite.put("type", type);
        suite.put("count", countTestsInPackage(packagePath));
        suite.put("lastRun", "Unknown");
        suite.put("status", "unknown");
        suite.put("successRate", 0.0);
        return suite;
    }

    private int countTestsInPackage(String packagePath) {
        try {
            File testDir = new File("src/test/java/" + packagePath);
            if (testDir.exists() && testDir.isDirectory()) {
                File[] files = testDir.listFiles((dir, name) -> name.endsWith("Test.java") || name.endsWith("Tests.java"));
                return files != null ? files.length * 10 : 0; // Estimate 10 tests per file
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private String formatDuration(long durationMs) {
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;

        if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    private String getRelativeTime(LocalDateTime dateTime) {
        if (dateTime == null) return "Unknown";

        LocalDateTime now = LocalDateTime.now();
        long hours = java.time.Duration.between(dateTime, now).toHours();
        long days = java.time.Duration.between(dateTime, now).toDays();

        if (hours < 1) {
            long minutes = java.time.Duration.between(dateTime, now).toMinutes();
            return minutes + " minute" + (minutes != 1 ? "s" : "") + " ago";
        } else if (hours < 24) {
            return hours + " hour" + (hours != 1 ? "s" : "") + " ago";
        } else {
            return days + " day" + (days != 1 ? "s" : "") + " ago";
        }
    }

    private int calculateProgress(TestReport report) {
        if ("COMPLETED".equals(report.getStatus()) || "FAILED".equals(report.getStatus())) {
            return 100;
        } else if ("RUNNING".equals(report.getStatus())) {
            int completed = report.getPassedTests() + report.getFailedTests() + report.getSkippedTests();
            return report.getTotalTests() > 0 ? (completed * 100) / report.getTotalTests() : 0;
        }
        return 0;
    }

    private String mapSuiteType(String suite) {
        switch (suite.toLowerCase()) {
            case "api": return "API";
            case "ui": return "UI";
            case "integration": return "Integration";
            case "regression": return "Regression";
            case "smoke": return "Smoke";
            case "all": return "ALL";
            default: return "CUSTOM";
        }
    }

    /**
     * Get execution report details by report ID (with fallback to file parsing)
     */
    @GetMapping("/execution/{reportId}")
    public Map<String, Object> getExecutionReport(@PathVariable String reportId) {
        Map<String, Object> response = new HashMap<>();

        try {
            // First check database
            if (testReportRepository != null) {
                Optional<TestReport> reportOpt = testReportRepository.findByReportId(reportId);

                if (reportOpt.isPresent()) {
                    TestReport report = reportOpt.get();
                    response.put("success", true);
                    response.put("reportId", report.getReportId());
                    response.put("suite", report.getSuiteType());
                    response.put("status", report.getStatus());
                    response.put("executionDate", report.getExecutionDate());
                    response.put("durationMs", report.getDurationMs());
                    response.put("duration", report.getDurationMs() / 1000.0);
                    response.put("environment", report.getEnvironment() != null ? report.getEnvironment() : "N/A");
                    response.put("browser", report.getBrowser());
                    response.put("passed", report.getPassedTests());
                    response.put("failed", report.getFailedTests());
                    response.put("skipped", report.getSkippedTests());
                    response.put("total", report.getTotalTests());
                    response.put("successRate", report.getSuccessRate());
                    response.put("createdBy", report.getCreatedBy());

                    // Get test details
                    List<Map<String, Object>> testCases = new ArrayList<>();
                    for (org.automation.reports.model.TestReportDetail detail : report.getTestDetails()) {
                        testCases.add(convertTestDetailToMap(detail));
                    }
                    response.put("testCases", testCases);

                    return response;
                }
            }

            // Fallback: Try to read from report files
            File reportsDir = new File("artifacts/reports");
            if (reportsDir.exists()) {
                File reportDir = new File(reportsDir, reportId);
                if (reportDir.exists()) {
                    // Read summary.html to get basic stats
                    File summaryFile = new File(reportDir, "summary.html");
                    if (summaryFile.exists()) {
                        Map<String, Object> reportData = parseReportFromHtml(summaryFile, reportId);

                        // Try to get test details from TestNG results
                        List<Map<String, Object>> testCases = parseTestCasesFromTestNGResults();
                        reportData.put("testCases", testCases);
                        reportData.put("success", true);

                        return reportData;
                    }
                }
            }

            response.put("success", false);
            response.put("message", "Report not found");

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error fetching report: " + e.getMessage());
        }

        return response;
    }

    /**
     * Get specific test case details (with fallback)
     */
    @GetMapping("/execution/{reportId}/test/{testName}")
    public Map<String, Object> getExecutionTestDetail(@PathVariable String reportId, @PathVariable String testName) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (testReportRepository != null) {
                Optional<TestReport> reportOpt = testReportRepository.findByReportId(reportId);

                if (reportOpt.isPresent()) {
                    TestReport report = reportOpt.get();

                    // Find the specific test
                    for (org.automation.reports.model.TestReportDetail detail : report.getTestDetails()) {
                        if (detail.getTestName().equals(testName)) {
                            return convertTestDetailToMap(detail);
                        }
                    }
                }
            }

            response.put("success", false);
            response.put("message", "Test detail not found");

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error fetching test detail: " + e.getMessage());
        }

        return response;
    }

    /**
     * Convert TestReportDetail to Map for JSON response
     */
    private Map<String, Object> convertTestDetailToMap(org.automation.reports.model.TestReportDetail detail) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", detail.getId());
        map.put("name", detail.getTestName());
        map.put("testName", detail.getTestName());
        map.put("class", detail.getTestClass());
        map.put("testClass", detail.getTestClass());
        map.put("status", detail.getStatus());
        map.put("duration", formatDuration(detail.getDurationMs()));
        map.put("durationMs", detail.getDurationMs());
        map.put("startTime", detail.getStartTime());
        map.put("endTime", detail.getEndTime());
        map.put("errorMessage", detail.getErrorMessage());
        map.put("error", detail.getErrorMessage());
        map.put("stackTrace", detail.getStackTrace());
        map.put("screenshotPath", detail.getScreenshotPath());
        map.put("screenshotName", detail.getScreenshotName());
        map.put("testType", detail.getTestType());
        map.put("browser", detail.getBrowser());
        map.put("apiArtifactPath", detail.getApiArtifactPath());
        return map;
    }

    /**
     * Parse report data from HTML summary file
     */
    private Map<String, Object> parseReportFromHtml(File summaryFile, String reportId) {
        Map<String, Object> data = new HashMap<>();

        try {
            String content = new String(java.nio.file.Files.readAllBytes(summaryFile.toPath()));

            data.put("reportId", reportId);

            // Extract report ID from HTML
            if (content.contains("Report ID:")) {
                int start = content.indexOf("Report ID:") + "Report ID:".length();
                int end = content.indexOf("</p>", start);
                if (end > start) {
                    data.put("reportId", content.substring(start, end).replaceAll("<[^>]*>", "").trim());
                }
            }

            // Extract suite type
            if (content.contains("Suite Type:")) {
                int start = content.indexOf("Suite Type:") + "Suite Type:".length();
                int end = content.indexOf("</p>", start);
                if (end > start) {
                    data.put("suite", content.substring(start, end).replaceAll("<[^>]*>", "").trim());
                }
            }

            // Parse statistics from HTML
            int total = parseStatFromHtml(content, "Total Tests");
            int passed = parseStatFromHtml(content, "Passed");
            int failed = parseStatFromHtml(content, "Failed");
            int skipped = parseStatFromHtml(content, "Skipped");

            data.put("total", total);
            data.put("passed", passed);
            data.put("failed", failed);
            data.put("skipped", skipped);
            data.put("status", failed == 0 ? "COMPLETED" : "FAILED");
            data.put("successRate", total > 0 ? (passed * 100.0) / total : 0);
            data.put("environment", "N/A");
            data.put("browser", "chrome");
            data.put("createdBy", "CMD");
            data.put("executionDate", LocalDateTime.now());
            data.put("duration", 0);

        } catch (Exception e) {
            System.err.println("Error parsing HTML report: " + e.getMessage());
        }

        return data;
    }

    /**
     * Parse a statistic value from HTML content
     */
    private int parseStatFromHtml(String content, String label) {
        try {
            int labelIndex = content.indexOf(label);
            if (labelIndex > 0) {
                int valueStart = content.indexOf("<div class='stat-value", labelIndex);
                if (valueStart > 0) {
                    valueStart = content.indexOf(">", valueStart) + 1;
                    int valueEnd = content.indexOf("</div>", valueStart);
                    String valueStr = content.substring(valueStart, valueEnd).trim();
                    return Integer.parseInt(valueStr);
                }
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return 0;
    }

    /**
     * Parse test cases from TestNG results XML
     */
    private List<Map<String, Object>> parseTestCasesFromTestNGResults() {
        List<Map<String, Object>> testCases = new ArrayList<>();

        try {
            File testOutputDir = new File("test-output");
            if (!testOutputDir.exists()) {
                testOutputDir = new File("target/surefire-reports");
            }

            if (testOutputDir.exists()) {
                File[] xmlFiles = testOutputDir.listFiles((dir, name) -> name.startsWith("testng-results") && name.endsWith(".xml"));

                if (xmlFiles != null && xmlFiles.length > 0) {
                    // Parse the most recent TestNG results file
                    File resultsFile = xmlFiles[0];

                    javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                    javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
                    org.w3c.dom.Document doc = builder.parse(resultsFile);

                    org.w3c.dom.NodeList testMethods = doc.getElementsByTagName("test-method");

                    for (int i = 0; i < testMethods.getLength(); i++) {
                        org.w3c.dom.Element method = (org.w3c.dom.Element) testMethods.item(i);

                        String status = method.getAttribute("status");
                        String name = method.getAttribute("name");

                        // Skip configuration methods
                        if ("PASS".equals(status) || "FAIL".equals(status) || "SKIP".equals(status)) {
                            String isConfig = method.getAttribute("is-config");
                            if ("true".equals(isConfig)) {
                                continue;
                            }

                            Map<String, Object> testCase = new HashMap<>();
                            testCase.put("name", name);
                            testCase.put("testName", name);

                            // Get class name
                            org.w3c.dom.NodeList classNodes = method.getElementsByTagName("class");
                            if (classNodes.getLength() > 0) {
                                org.w3c.dom.Element classElement = (org.w3c.dom.Element) classNodes.item(0);
                                testCase.put("class", classElement.getAttribute("name"));
                                testCase.put("testClass", classElement.getAttribute("name"));
                            }

                            testCase.put("status", status.equals("PASS") ? "PASSED" : status);

                            // Get duration
                            String durationMs = method.getAttribute("duration-ms");
                            if (durationMs != null && !durationMs.isEmpty()) {
                                long duration = Long.parseLong(durationMs);
                                testCase.put("duration", formatDuration(duration));
                                testCase.put("durationMs", duration);
                            } else {
                                testCase.put("duration", "N/A");
                                testCase.put("durationMs", 0);
                            }

                            // Get error message if failed
                            if ("FAIL".equals(status)) {
                                org.w3c.dom.NodeList exceptionNodes = method.getElementsByTagName("exception");
                                if (exceptionNodes.getLength() > 0) {
                                    org.w3c.dom.Element exception = (org.w3c.dom.Element) exceptionNodes.item(0);
                                    org.w3c.dom.NodeList messageNodes = exception.getElementsByTagName("message");
                                    if (messageNodes.getLength() > 0) {
                                        testCase.put("errorMessage", messageNodes.item(0).getTextContent());
                                        testCase.put("error", messageNodes.item(0).getTextContent());
                                    }

                                    org.w3c.dom.NodeList stackNodes = exception.getElementsByTagName("full-stacktrace");
                                    if (stackNodes.getLength() > 0) {
                                        testCase.put("stackTrace", stackNodes.item(0).getTextContent());
                                    }
                                }
                            }

                            testCases.add(testCase);
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error parsing TestNG results: " + e.getMessage());
        }

        return testCases;
    }
}
