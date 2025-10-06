package org.automation.dashboard.controller;

import org.automation.analytics.model.ExecutionLog;
import org.automation.analytics.repo.ExecutionLogRepository;
import org.automation.reports.model.TestReport;
import org.automation.reports.repository.TestReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
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

                    // Parse TestNG results
                    if (line.contains("Tests run:")) {
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
                    }

                    // Detect current test
                    if (line.contains("Running") && line.contains("org.automation")) {
                        String testClass = line.substring(line.indexOf("org.automation"));
                        tracker.currentTest = testClass;
                    }
                }

                // Wait for process to complete
                int exitCode = process.waitFor();

                // Update final status
                tracker.status = (exitCode == 0 && tracker.failed == 0) ? "COMPLETED" : "FAILED";

                // Update database
                if (testReportRepository != null) {
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
                }

                // Clean up after 5 minutes
                new Thread(() -> {
                    try {
                        Thread.sleep(5 * 60 * 1000); // 5 minutes
                        activeExecutions.remove(executionId);
                        executionProcesses.remove(executionId);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();

            } catch (Exception e) {
                tracker.status = "FAILED";
                e.printStackTrace();
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
}
