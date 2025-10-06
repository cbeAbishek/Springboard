package org.automation.analytics.service;

import org.automation.analytics.model.ExecutionLog;
import org.automation.analytics.repo.ExecutionLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final ExecutionLogRepository repo;

    public AnalyticsService(ExecutionLogRepository repo) {
        this.repo = repo;
    }

    public Map<String, Object> getSummary(LocalDate from, LocalDate to) {
        LocalDateTime f = (from != null) ? from.atStartOfDay() : LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime t = (to != null) ? to.atTime(LocalTime.MAX) : LocalDate.now().atTime(LocalTime.MAX);

        List<ExecutionLog> rows = repo.findAll().stream()
                .filter(r -> r.getStartTime() != null && !r.getStartTime().isBefore(f) && !r.getStartTime().isAfter(t))
                .collect(Collectors.toList());

        long total = rows.size();
        long passed = rows.stream().filter(r -> "PASS".equalsIgnoreCase(r.getStatus())).count();
        long failed = rows.stream().filter(r -> "FAIL".equalsIgnoreCase(r.getStatus())).count();
        double passRate = total == 0 ? 0.0 : (passed * 100.0 / total);

        // ✅ FIX: no null check — durationMs is a primitive long (0 if unset)
        double avgDuration = total == 0 ? 0.0 : rows.stream().mapToLong(ExecutionLog::getDurationMs).average().orElse(0);

        Map<String, Object> out = new HashMap<>();
        out.put("total", total);
        out.put("passed", passed);
        out.put("failed", failed);
        out.put("passRate", passRate);
        out.put("avgDurationMs", avgDuration);
        return out;
    }

    public List<Map<String, Object>> getTrends(LocalDate from, LocalDate to) {
        LocalDate start = (from != null) ? from : LocalDate.now().minusDays(7);
        LocalDate end = (to != null) ? to : LocalDate.now();

        LocalDateTime f = start.atStartOfDay();
        LocalDateTime t = end.atTime(LocalTime.MAX);

        List<ExecutionLog> rows = repo.findAll().stream()
                .filter(r -> r.getStartTime() != null && !r.getStartTime().isBefore(f) && !r.getStartTime().isAfter(t))
                .collect(Collectors.toList());

        Map<LocalDate, long[]> map = new TreeMap<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            map.put(d, new long[]{0, 0}); // [passed, failed]
        }

        for (ExecutionLog r : rows) {
            LocalDate d = r.getStartTime().toLocalDate();
            long[] counts = map.getOrDefault(d, new long[]{0, 0});
            if ("PASS".equalsIgnoreCase(r.getStatus())) counts[0]++;
            else if ("FAIL".equalsIgnoreCase(r.getStatus())) counts[1]++;
            map.put(d, counts);
        }

        List<Map<String, Object>> out = new ArrayList<>();
        map.forEach((date, counts) -> {
            Map<String, Object> m = new HashMap<>();
            m.put("date", date.toString());
            m.put("passed", counts[0]);
            m.put("failed", counts[1]);
            out.add(m);
        });
        return out;
    }

    public List<ExecutionLog> getResultsBySuite(String suiteId) {
        return repo.findBySuiteId(suiteId);
    }

    /**
     * Get comprehensive test execution statistics for dashboard
     */
    public Map<String, Object> getTestExecutionStats() {
        List<ExecutionLog> allLogs = repo.findAll();

        long totalTests = allLogs.size();
        long passedTests = allLogs.stream().filter(r -> "PASS".equalsIgnoreCase(r.getStatus()) || "PASSED".equalsIgnoreCase(r.getStatus())).count();
        long failedTests = allLogs.stream().filter(r -> "FAIL".equalsIgnoreCase(r.getStatus()) || "FAILED".equalsIgnoreCase(r.getStatus())).count();
        long skippedTests = allLogs.stream().filter(r -> "SKIP".equalsIgnoreCase(r.getStatus()) || "SKIPPED".equalsIgnoreCase(r.getStatus())).count();

        double successRate = totalTests == 0 ? 0.0 : (passedTests * 100.0 / totalTests);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTests", totalTests);
        stats.put("passedTests", passedTests);
        stats.put("failedTests", failedTests);
        stats.put("skippedTests", skippedTests);
        stats.put("successRate", Math.round(successRate * 100.0) / 100.0);

        return stats;
    }

    /**
     * Get recent test executions for dashboard display
     */
    public List<ExecutionLog> getRecentExecutions(int limit) {
        return repo.findAll().stream()
                .sorted((a, b) -> b.getStartTime().compareTo(a.getStartTime()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Execute test suite asynchronously and return execution ID
     */
    public String executeTestSuite(String suite, String browser, boolean headless) throws Exception {
        String executionId = "exec_" + System.currentTimeMillis();

        // Create execution log entry
        ExecutionLog executionLog = new ExecutionLog();
        executionLog.setTestName("TestSuite_" + suite);
        executionLog.setTestClass(suite + "Tests");
        executionLog.setStatus("RUNNING");
        executionLog.setStartTime(LocalDateTime.now());
        executionLog.setBrowser(browser);

        repo.save(executionLog);

        // Execute tests asynchronously (in a real implementation, this would use @Async)
        CompletableFuture.runAsync(() -> {
            try {
                executeTestsInternal(suite, browser, headless, executionId);
            } catch (Exception e) {
                updateExecutionStatus(executionId, "FAILED", e.getMessage());
            }
        });

        return executionId;
    }

    /**
     * Get execution status for polling
     */
    public Map<String, Object> getExecutionStatus(String executionId) {
        // In a real implementation, this would track actual execution status
        Map<String, Object> status = new HashMap<>();
        status.put("executionId", executionId);
        status.put("status", "RUNNING");
        status.put("progress", 75);
        status.put("currentTest", "SampleTest");

        return status;
    }

    private void executeTestsInternal(String suite, String browser, boolean headless, String executionId) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            List<String> command = new ArrayList<>();
            command.add("mvn");
            command.add("clean");
            command.add("test");
            command.add("-Dsuite=" + suite);
            command.add("-Dbrowser=" + browser);
            command.add("-Dheadless=" + headless);

            processBuilder.command(command);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            String status = exitCode == 0 ? "COMPLETED" : "FAILED";
            updateExecutionStatus(executionId, status, null);

        } catch (Exception e) {
            updateExecutionStatus(executionId, "FAILED", e.getMessage());
        }
    }

    private void updateExecutionStatus(String executionId, String status, String errorMessage) {
        // Update the execution log with final status
        // In a real implementation, you'd find by executionId and update
    }

    /**
     * Get test matrix data for cumulative analysis
     */
    public Map<String, Object> getTestMatrix(int days, String suite, String status) {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(days);

        List<ExecutionLog> logs = repo.findAll().stream()
                .filter(log -> log.getStartTime() != null && log.getStartTime().isAfter(fromDate))
                .filter(log -> suite.equals("all") || suite.equalsIgnoreCase(log.getSuiteId()))
                .filter(log -> status.equals("all") || status.equalsIgnoreCase(log.getStatus()))
                .collect(Collectors.toList());

        // Group tests by name to calculate pass rates
        Map<String, List<ExecutionLog>> testGroups = logs.stream()
                .collect(Collectors.groupingBy(ExecutionLog::getTestName));

        List<Map<String, Object>> testData = new ArrayList<>();

        testGroups.forEach((testName, executions) -> {
            long totalRuns = executions.size();
            long passed = executions.stream().filter(e -> "PASS".equalsIgnoreCase(e.getStatus()) || "PASSED".equalsIgnoreCase(e.getStatus())).count();
            double passRate = totalRuns > 0 ? (passed * 100.0 / totalRuns) : 0;

            ExecutionLog latest = executions.stream()
                    .max(Comparator.comparing(ExecutionLog::getStartTime))
                    .orElse(executions.get(0));

            Map<String, Object> testInfo = new HashMap<>();
            testInfo.put("testName", testName);
            testInfo.put("suite", latest.getSuiteId() != null ? latest.getSuiteId() : "Unknown");
            testInfo.put("status", latest.getStatus());
            testInfo.put("executionTime", latest.getStartTime());
            testInfo.put("duration", latest.getDurationMs());
            testInfo.put("environment", latest.getBrowser() != null ? latest.getBrowser() : "N/A");
            testInfo.put("passRate", Math.round(passRate * 100.0) / 100.0);
            testInfo.put("lastRun", latest.getStartTime());
            testInfo.put("trend", calculateTrend(executions));
            testInfo.put("totalRuns", totalRuns);

            testData.add(testInfo);
        });

        // Calculate summary
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalExecutions", logs.size());
        summary.put("totalPassed", logs.stream().filter(e -> "PASS".equalsIgnoreCase(e.getStatus()) || "PASSED".equalsIgnoreCase(e.getStatus())).count());
        summary.put("totalFailed", logs.stream().filter(e -> "FAIL".equalsIgnoreCase(e.getStatus()) || "FAILED".equalsIgnoreCase(e.getStatus())).count());
        summary.put("overallPassRate", logs.isEmpty() ? 0 : (summary.get("totalPassed").toString().equals("0") ? 0 :
                (Long.parseLong(summary.get("totalPassed").toString()) * 100.0 / logs.size())));

        Map<String, Object> result = new HashMap<>();
        result.put("tests", testData);
        result.put("summary", summary);

        return result;
    }

    /**
     * Get failure heatmap data for last N days
     */
    public List<Map<String, Object>> getFailureHeatmap(int days) {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(days);

        List<ExecutionLog> logs = repo.findAll().stream()
                .filter(log -> log.getStartTime() != null && log.getStartTime().isAfter(fromDate))
                .collect(Collectors.toList());

        Map<String, List<ExecutionLog>> testGroups = logs.stream()
                .collect(Collectors.groupingBy(ExecutionLog::getTestName));

        List<Map<String, Object>> heatmapData = new ArrayList<>();

        testGroups.forEach((testName, executions) -> {
            Map<String, Object> testHeatmap = new HashMap<>();
            testHeatmap.put("testName", testName);

            // Create daily failure counts for the last N days
            List<Integer> dailyFailures = new ArrayList<>();
            for (int i = days - 1; i >= 0; i--) {
                LocalDateTime dayStart = LocalDateTime.now().minusDays(i).withHour(0).withMinute(0).withSecond(0);
                LocalDateTime dayEnd = dayStart.plusDays(1);

                long failures = executions.stream()
                        .filter(e -> e.getStartTime().isAfter(dayStart) && e.getStartTime().isBefore(dayEnd))
                        .filter(e -> "FAIL".equalsIgnoreCase(e.getStatus()) || "FAILED".equalsIgnoreCase(e.getStatus()))
                        .count();

                dailyFailures.add((int) failures);
            }

            testHeatmap.put("dailyFailures", dailyFailures);
            heatmapData.add(testHeatmap);
        });

        return heatmapData;
    }

    /**
     * Export test data to CSV format
     */
    public String exportTestDataToCsv() {
        List<ExecutionLog> logs = repo.findAll();
        StringBuilder csv = new StringBuilder();

        // CSV Header
        csv.append("Test Name,Suite,Status,Start Time,Duration (ms),Browser,Error Message\n");

        // CSV Data
        logs.forEach(log -> {
            csv.append(escapeCSV(log.getTestName())).append(",");
            csv.append(escapeCSV(log.getSuiteId())).append(",");
            csv.append(escapeCSV(log.getStatus())).append(",");
            csv.append(escapeCSV(log.getStartTime() != null ? log.getStartTime().toString() : "")).append(",");
            csv.append(log.getDurationMs()).append(",");
            csv.append(escapeCSV(log.getBrowser())).append(",");
            csv.append(escapeCSV(log.getErrorMessage())).append("\n");
        });

        return csv.toString();
    }

    /**
     * Export test data to Excel format
     */
    public byte[] exportTestDataToExcel() {
        // This is a placeholder. In a real implementation, you'd use Apache POI
        // to generate actual Excel files
        return exportTestDataToCsv().getBytes();
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private int calculateTrend(List<ExecutionLog> executions) {
        if (executions.size() < 2) return 0;

        List<ExecutionLog> sorted = executions.stream()
                .sorted(Comparator.comparing(ExecutionLog::getStartTime))
                .collect(Collectors.toList());

        int halfSize = sorted.size() / 2;
        List<ExecutionLog> firstHalf = sorted.subList(0, halfSize);
        List<ExecutionLog> secondHalf = sorted.subList(halfSize, sorted.size());

        double firstPassRate = firstHalf.stream()
                .filter(e -> "PASS".equalsIgnoreCase(e.getStatus()) || "PASSED".equalsIgnoreCase(e.getStatus()))
                .count() * 100.0 / firstHalf.size();

        double secondPassRate = secondHalf.stream()
                .filter(e -> "PASS".equalsIgnoreCase(e.getStatus()) || "PASSED".equalsIgnoreCase(e.getStatus()))
                .count() * 100.0 / secondHalf.size();

        if (secondPassRate > firstPassRate + 5) return 1;  // Improving
        if (secondPassRate < firstPassRate - 5) return -1; // Declining
        return 0; // Stable
    }
}
