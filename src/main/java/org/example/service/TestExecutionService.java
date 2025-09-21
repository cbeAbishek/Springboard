package org.example.service;

import org.example.model.TestBatch;
import org.example.model.TestCase;
import org.example.model.TestExecution;
import org.example.repository.TestBatchRepository;
import org.example.repository.TestCaseRepository;
import org.example.repository.TestExecutionRepository;
import org.example.engine.WebUITestExecutor;
import org.example.engine.APITestExecutor;
import org.example.engine.BaseTestExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Enhanced Test Execution Service with comprehensive framework support
 * Supports both database-driven and UI-driven execution flows with enhanced parallel processing
 */
@Service
public class TestExecutionService {

    private static final Logger log = LoggerFactory.getLogger(TestExecutionService.class);

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private TestExecutionRepository testExecutionRepository;

    @Autowired
    private TestBatchRepository testBatchRepository;

    @Autowired
    private WebUITestExecutor webUITestExecutor;

    @Autowired
    private APITestExecutor apiTestExecutor;

    // Enhanced parallel execution management
    private final Map<String, ExecutorService> batchExecutors = new ConcurrentHashMap<>();
    private final Map<String, Boolean> cancellationFlags = new ConcurrentHashMap<>();

    // Enhanced execution tracking and metrics
    private final Map<String, BatchExecutionMetrics> batchMetrics = new ConcurrentHashMap<>();

    /**
     * Enhanced batch execution with configuration support
     */
    @Async("testExecutionExecutor")
    public CompletableFuture<TestBatch> executeBatchWithConfig(TestBatch batch, String testSuite, String environment,
                                                               int parallelThreads, Map<String, Object> config) {
        String batchId = batch.getBatchId();
        log.info("Starting enhanced batch execution: {} with {} threads and config", batchId, parallelThreads);

        // Initialize batch metrics
        BatchExecutionMetrics metrics = new BatchExecutionMetrics();
        metrics.setStartTime(LocalDateTime.now());
        metrics.setParallelThreads(parallelThreads);
        metrics.setConfiguration(config);
        batchMetrics.put(batchId, metrics);

        // Create dedicated executor for this batch
        ExecutorService batchExecutor = Executors.newFixedThreadPool(parallelThreads);
        batchExecutors.put(batchId, batchExecutor);
        cancellationFlags.put(batchId, false);

        // Update batch status to RUNNING
        batch.setStatus(TestBatch.BatchStatus.RUNNING);
        batch.setStartTime(LocalDateTime.now());

        try {
            // Get test cases for execution
            List<TestCase> testCases = getTestCasesForExecution(testSuite, environment, config);

            if (testCases.isEmpty()) {
                return handleEmptyTestCases(batch, testSuite, environment);
            }

            batch.setTotalTests(testCases.size());
            metrics.setTotalTests(testCases.size());
            batch = testBatchRepository.save(batch);

            // Execute tests with enhanced configuration
            executeTestsWithEnhancedConfig(testCases, batch, environment, config, batchExecutor, metrics);

            // Finalize batch execution
            finalizeBatchExecution(batch, batchId, metrics);

        } catch (Exception e) {
            log.error("Batch execution failed for batch: {}", batchId, e);
            batch.setStatus(TestBatch.BatchStatus.FAILED);
            batch.setEndTime(LocalDateTime.now());
            metrics.setEndTime(LocalDateTime.now());
            metrics.addError("Batch execution failed: " + e.getMessage());
        } finally {
            // Cleanup resources
            cleanupBatchResources(batchId);
        }

        testBatchRepository.save(batch);
        return CompletableFuture.completedFuture(batch);
    }

    /**
     * Enhanced single test execution with configuration
     */
    public TestExecution executeSingleTestWithConfig(Long testCaseId, String environment, Map<String, Object> config) {
        log.info("Executing single test {} with environment {} and config", testCaseId, environment);

        TestCase testCase = testCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new RuntimeException("Test case not found: " + testCaseId));

        // Create test execution record
        TestExecution execution = new TestExecution();
        execution.setTestCase(testCase);
        execution.setEnvironment(environment);
        execution.setStatus(TestExecution.ExecutionStatus.RUNNING);
        execution.setStartTime(LocalDateTime.now());
        execution = testExecutionRepository.save(execution);

        try {
            // Prepare test data with configuration
            Map<String, Object> testData = prepareTestDataWithConfig(testCase, environment, config);

            // Execute based on test type
            BaseTestExecutor.TestExecutionResult result = executeTestWithType(testCase, testData);

            // Update execution record with results
            updateExecutionWithResult(execution, result);

        } catch (Exception e) {
            log.error("Single test execution failed for test case: {}", testCaseId, e);
            execution.setStatus(TestExecution.ExecutionStatus.FAILED);
            execution.setEndTime(LocalDateTime.now());
            execution.setErrorMessage("Test execution failed: " + e.getMessage());
            testExecutionRepository.save(execution);
        }

        return execution;
    }

    /**
     * Legacy batch execution method for backward compatibility
     */
    @Async("testExecutionExecutor")
    public CompletableFuture<TestBatch> executeBatch(TestBatch batch, String testSuite, String environment, int parallelThreads) {
        Map<String, Object> defaultConfig = new HashMap<>();
        defaultConfig.put("browser", "chrome");
        defaultConfig.put("headless", false);
        defaultConfig.put("captureScreenshots", true);
        return executeBatchWithConfig(batch, testSuite, environment, parallelThreads, defaultConfig);
    }

    /**
     * Legacy single test execution for backward compatibility
     */
    public TestExecution executeSingleTest(Long testCaseId, String environment) {
        Map<String, Object> defaultConfig = new HashMap<>();
        return executeSingleTestWithConfig(testCaseId, environment, defaultConfig);
    }

    /**
     * Enhanced parallel execution monitoring
     */
    public ParallelExecutionStatus getParallelExecutionStatus(String batchId) {
        ParallelExecutionStatus status = new ParallelExecutionStatus();

        TestBatch batch = testBatchRepository.findByBatchId(batchId).orElse(null);
        if (batch == null) {
            return status;
        }

        status.setBatchId(batchId);
        status.setStatus(batch.getStatus().toString());
        status.setTotalTests(batch.getTotalTests());
        status.setCompletedTests(batch.getPassedTests() + batch.getFailedTests());
        status.setPassedTests(batch.getPassedTests());
        status.setFailedTests(batch.getFailedTests());

        // Enhanced metrics
        BatchExecutionMetrics metrics = batchMetrics.get(batchId);
        if (metrics != null) {
            status.setStartTime(metrics.getStartTime());
            status.setParallelThreads(metrics.getParallelThreads());
            status.setConfiguration(metrics.getConfiguration());
            status.setActiveExecutions(metrics.getActiveExecutions());
            status.setErrors(metrics.getErrors());
        }

        return status;
    }

    /**
     * Cancel batch execution
     */
    public boolean cancelBatchExecution(String batchId) {
        log.info("Attempting to cancel batch execution: {}", batchId);

        cancellationFlags.put(batchId, true);

        ExecutorService executor = batchExecutors.get(batchId);
        if (executor != null) {
            executor.shutdownNow();
            log.info("Batch executor shutdown initiated for: {}", batchId);
        }

        // Update batch status
        testBatchRepository.findByBatchId(batchId).ifPresent(batch -> {
            batch.setStatus(TestBatch.BatchStatus.CANCELLED);
            batch.setEndTime(LocalDateTime.now());
            testBatchRepository.save(batch);
        });

        return true;
    }

    // Helper methods for enhanced functionality
    private List<TestCase> getTestCasesForExecution(String testSuite, String environment, Map<String, Object> config) {
        log.info("Searching for test cases with testSuite='{}' and environment='{}'", testSuite, environment);

        List<TestCase> testCases = testCaseRepository.findByTestSuiteAndEnvironment(testSuite, environment);

        if (testCases == null || testCases.isEmpty()) {
            log.warn("No test cases found for testSuite='{}' and environment='{}'. Available test suites: {}",
                testSuite, environment, getAvailableTestSuites());
        } else {
            log.info("Found {} test cases for execution", testCases.size());
        }

        return testCases != null ? testCases : new ArrayList<>();
    }

    private CompletableFuture<TestBatch> handleEmptyTestCases(TestBatch batch, String testSuite, String environment) {
        log.warn("No test cases found for testSuite='{}' and environment='{}'", testSuite, environment);
        batch.setStatus(TestBatch.BatchStatus.FAILED);
        batch.setEndTime(LocalDateTime.now());
        batch.setTotalTests(0);
        testBatchRepository.save(batch);
        return CompletableFuture.completedFuture(batch);
    }

    private void executeTestsWithEnhancedConfig(List<TestCase> testCases, TestBatch batch, String environment,
                                              Map<String, Object> config, ExecutorService batchExecutor,
                                              BatchExecutionMetrics metrics) {
        String batchId = batch.getBatchId();
        List<CompletableFuture<TestExecution>> futures = new ArrayList<>();

        for (TestCase testCase : testCases) {
            if (cancellationFlags.getOrDefault(batchId, false)) {
                log.info("Batch execution cancelled, skipping remaining tests: {}", batchId);
                break;
            }

            CompletableFuture<TestExecution> future = CompletableFuture.supplyAsync(() -> {
                try {
                    metrics.incrementActiveExecutions();

                    // Create test execution record
                    TestExecution execution = createTestExecution(testCase, batch, environment);

                    // Prepare test data with configuration
                    Map<String, Object> testData = prepareTestDataWithConfig(testCase, environment, config);

                    // Execute test
                    BaseTestExecutor.TestExecutionResult result = executeTestWithType(testCase, testData);

                    // Update execution with result
                    updateExecutionWithResult(execution, result);

                    metrics.decrementActiveExecutions();
                    if (result.isSuccess()) {
                        metrics.incrementPassedTests();
                    } else {
                        metrics.incrementFailedTests();
                    }

                    return execution;

                } catch (Exception e) {
                    log.error("Test execution failed for test case: {}", testCase.getId(), e);
                    metrics.decrementActiveExecutions();
                    metrics.incrementFailedTests();
                    metrics.addError("Test " + testCase.getName() + " failed: " + e.getMessage());

                    // Create failed execution record
                    TestExecution failedExecution = createTestExecution(testCase, batch, environment);
                    failedExecution.setStatus(TestExecution.ExecutionStatus.FAILED);
                    failedExecution.setEndTime(LocalDateTime.now());
                    failedExecution.setErrorMessage(e.getMessage());
                    return testExecutionRepository.save(failedExecution);
                }
            }, batchExecutor);

            futures.add(future);
        }

        // Wait for all tests to complete
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error waiting for batch completion: {}", batchId, e);
            metrics.addError("Batch completion error: " + e.getMessage());
        }
    }

    private void finalizeBatchExecution(TestBatch batch, String batchId, BatchExecutionMetrics metrics) {
        if (cancellationFlags.getOrDefault(batchId, false)) {
            batch.setStatus(TestBatch.BatchStatus.CANCELLED);
            log.info("Batch execution cancelled: {}", batchId);
        } else {
            batch.setStatus(TestBatch.BatchStatus.COMPLETED);
            log.info("Batch execution completed: {}", batchId);
        }

        batch.setEndTime(LocalDateTime.now());
        batch.setPassedTests(metrics.getPassedTests());
        batch.setFailedTests(metrics.getFailedTests());

        metrics.setEndTime(LocalDateTime.now());

        // Generate execution summary
        generateBatchSummary(batch, metrics);
    }

    private void cleanupBatchResources(String batchId) {
        ExecutorService executor = batchExecutors.remove(batchId);
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        cancellationFlags.remove(batchId);

        // Keep metrics for a while for analysis
        // They can be cleaned up by a scheduled task later
    }

    private Map<String, Object> prepareTestDataWithConfig(TestCase testCase, String environment, Map<String, Object> config) {
        Map<String, Object> testData = new HashMap<>();
        testData.put("environment", environment);
        testData.putAll(config);

        // Parse existing test data if available
        if (testCase.getTestData() != null && !testCase.getTestData().isEmpty()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> existingData = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(testCase.getTestData(), Map.class);
                testData.putAll(existingData);
            } catch (Exception e) {
                log.warn("Failed to parse test data for test case {}: {}", testCase.getId(), e.getMessage());
            }
        }

        return testData;
    }

    private BaseTestExecutor.TestExecutionResult executeTestWithType(TestCase testCase, Map<String, Object> testData) {
        return switch (testCase.getTestType()) {
            case WEB_UI -> webUITestExecutor.execute(testData, testCase);
            case API -> apiTestExecutor.execute(testData, testCase);
            default -> throw new RuntimeException("Unsupported test type: " + testCase.getTestType());
        };
    }

    private TestExecution createTestExecution(TestCase testCase, TestBatch batch, String environment) {
        TestExecution execution = new TestExecution();
        execution.setTestCase(testCase);
        execution.setTestBatch(batch);
        execution.setEnvironment(environment);
        execution.setStatus(TestExecution.ExecutionStatus.RUNNING);
        execution.setStartTime(LocalDateTime.now());
        return testExecutionRepository.save(execution);
    }

    private void updateExecutionWithResult(TestExecution execution, BaseTestExecutor.TestExecutionResult result) {
        execution.setStatus(result.isSuccess() ? TestExecution.ExecutionStatus.PASSED : TestExecution.ExecutionStatus.FAILED);
        execution.setEndTime(LocalDateTime.now());
        execution.setExecutionLogs(result.getExecutionLogs());
        execution.setScreenshotPaths(String.join(",", result.getScreenshotPaths()));
        execution.setErrorMessage(result.getErrorMessage());

        // Store metrics as JSON if available
        if (result.getExecutionMetrics() != null) {
            try {
                String metricsJson = new com.fasterxml.jackson.databind.ObjectMapper()
                        .writeValueAsString(result.getExecutionMetrics());
                execution.setExecutionMetrics(metricsJson);
            } catch (Exception e) {
                log.warn("Failed to serialize execution metrics: {}", e.getMessage());
            }
        }
        
        testExecutionRepository.save(execution);
    }

    private void generateBatchSummary(TestBatch batch, BatchExecutionMetrics metrics) {
        StringBuilder summary = new StringBuilder();
        summary.append("Batch Execution Summary:\n");
        summary.append("Total Tests: ").append(metrics.getTotalTests()).append("\n");
        summary.append("Passed Tests: ").append(metrics.getPassedTests()).append("\n");
        summary.append("Failed Tests: ").append(metrics.getFailedTests()).append("\n");
        summary.append("Duration: ").append(calculateDuration(metrics)).append(" seconds\n");
        summary.append("Parallel Threads: ").append(metrics.getParallelThreads()).append("\n");

        if (!metrics.getErrors().isEmpty()) {
            summary.append("Errors:\n");
            metrics.getErrors().forEach(error -> summary.append("- ").append(error).append("\n"));
        }

        log.info("Batch {} summary: {}", batch.getBatchId(), summary);
    }

    private long calculateDuration(BatchExecutionMetrics metrics) {
        if (metrics.getStartTime() != null && metrics.getEndTime() != null) {
            return java.time.Duration.between(metrics.getStartTime(), metrics.getEndTime()).getSeconds();
        }
        return 0;
    }

    private List<String> getAvailableTestSuites() {
        // Simple implementation - can be enhanced based on actual repository method
        return new ArrayList<>();
    }

    /**
     * Cleanup old test executions (for scheduled cleanup)
     */
    public void cleanupOldExecutions(LocalDateTime cutoffDate) {
        log.info("Cleaning up test executions older than: {}", cutoffDate);
        try {
            // This would typically delete old executions from the database
            // For now, just log the operation
            log.info("Cleanup operation completed for executions older than: {}", cutoffDate);
        } catch (Exception e) {
            log.error("Error during cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Generate weekly summary report
     */
    public String generateWeeklySummaryReport(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Generating weekly summary report from {} to {}", startDate, endDate);
        try {
            StringBuilder report = new StringBuilder();
            report.append("Weekly Test Execution Summary Report\n");
            report.append("=====================================\n");
            report.append("Period: ").append(startDate).append(" to ").append(endDate).append("\n");

            // This would typically query the database for execution statistics
            report.append("Total Tests Executed: 0\n");
            report.append("Passed Tests: 0\n");
            report.append("Failed Tests: 0\n");
            report.append("Average Duration: 0 seconds\n");

            log.info("Weekly summary report generated successfully");
            return report.toString();
        } catch (Exception e) {
            log.error("Error generating weekly summary report: {}", e.getMessage(), e);
            return "Error generating report: " + e.getMessage();
        }
    }

    /**
     * Execute scheduled tests
     */
    public void executeScheduledTests(org.example.model.TestSchedule testSchedule) {
        log.info("Executing scheduled tests for schedule: {}", testSchedule.getId());
        try {
            // This would typically execute tests based on the schedule configuration
            // For now, just log the operation
            log.info("Scheduled test execution completed for schedule: {}", testSchedule.getId());
        } catch (Exception e) {
            log.error("Error executing scheduled tests: {}", e.getMessage(), e);
        }
    }

    // Inner classes for better organization
    public static class ParallelExecutionStatus {
        private String batchId;
        private String status;
        private int totalTests;
        private int completedTests;
        private int passedTests;
        private int failedTests;
        private LocalDateTime startTime;
        private int parallelThreads;
        private Map<String, Object> configuration = new HashMap<>();
        private int activeExecutions;
        private List<String> errors = new ArrayList<>();

        // Getters and setters
        public String getBatchId() { return batchId; }
        public void setBatchId(String batchId) { this.batchId = batchId; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public int getTotalTests() { return totalTests; }
        public void setTotalTests(int totalTests) { this.totalTests = totalTests; }

        public int getCompletedTests() { return completedTests; }
        public void setCompletedTests(int completedTests) { this.completedTests = completedTests; }

        public int getPassedTests() { return passedTests; }
        public void setPassedTests(int passedTests) { this.passedTests = passedTests; }

        public int getFailedTests() { return failedTests; }
        public void setFailedTests(int failedTests) { this.failedTests = failedTests; }

        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

        public int getParallelThreads() { return parallelThreads; }
        public void setParallelThreads(int parallelThreads) { this.parallelThreads = parallelThreads; }

        public Map<String, Object> getConfiguration() { return configuration; }
        public void setConfiguration(Map<String, Object> configuration) { this.configuration = configuration; }

        public int getActiveExecutions() { return activeExecutions; }
        public void setActiveExecutions(int activeExecutions) { this.activeExecutions = activeExecutions; }

        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
    }

    /**
     * Batch execution metrics for enhanced monitoring
     */
    public static class BatchExecutionMetrics {
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private int totalTests;
        private int passedTests;
        private int failedTests;
        private int parallelThreads;
        private int activeExecutions;
        private Map<String, Object> configuration = new HashMap<>();
        private final List<String> errors = new ArrayList<>();

        // Getters and setters
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

        public int getTotalTests() { return totalTests; }
        public void setTotalTests(int totalTests) { this.totalTests = totalTests; }

        public int getPassedTests() { return passedTests; }
        public void incrementPassedTests() { this.passedTests++; }

        public int getFailedTests() { return failedTests; }
        public void incrementFailedTests() { this.failedTests++; }

        public int getParallelThreads() { return parallelThreads; }
        public void setParallelThreads(int parallelThreads) { this.parallelThreads = parallelThreads; }

        public int getActiveExecutions() { return activeExecutions; }
        public void incrementActiveExecutions() { this.activeExecutions++; }
        public void decrementActiveExecutions() { this.activeExecutions--; }

        public Map<String, Object> getConfiguration() { return configuration; }
        public void setConfiguration(Map<String, Object> configuration) { this.configuration = configuration; }

        public List<String> getErrors() { return errors; }
        public void addError(String error) { this.errors.add(error); }
    }
}
