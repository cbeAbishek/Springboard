package org.example.service;

import org.example.dto.TestExecutionResultDTO;
import org.example.model.TestBatch;
import org.example.model.TestCase;
import org.example.model.TestExecution;
import org.example.model.TestSchedule;
import org.example.repository.TestBatchRepository;
import org.example.repository.TestCaseRepository;
import org.example.repository.TestExecutionRepository;
import org.example.engine.WebUITestExecutor;
import org.example.engine.ApiTestExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
    private ApiTestExecutor apiTestExecutor;

    @Async("testExecutionExecutor")
    public CompletableFuture<TestBatch> executeBatch(TestBatch batch, String testSuite, String environment, int parallelThreads) {
        // Update batch status to RUNNING
        batch.setStatus(TestBatch.BatchStatus.RUNNING);
        batch.setStartTime(LocalDateTime.now());

        try {
            log.info("Starting batch execution: {}", batch.getBatchId());

            // Get test cases for execution
            List<TestCase> testCases = testCaseRepository.findByTestSuiteAndEnvironment(testSuite, environment);
            batch.setTotalTests(testCases.size());

            // Save batch initially
            batch = testBatchRepository.save(batch);

            // Execute tests in parallel
            executeTestsInParallel(testCases, batch, environment, parallelThreads);

            // Update batch status
            batch.setStatus(TestBatch.BatchStatus.COMPLETED);
            batch.setEndTime(LocalDateTime.now());

        } catch (Exception e) {
            log.error("Batch execution failed: {}", batch.getBatchId(), e);
            batch.setStatus(TestBatch.BatchStatus.FAILED);
            batch.setEndTime(LocalDateTime.now());
        } finally {
            batch = testBatchRepository.save(batch);
        }

        return CompletableFuture.completedFuture(batch);
    }

    private void executeTestsInParallel(List<TestCase> testCases, TestBatch batch, String environment, int parallelThreads) {
        ExecutorService executor = Executors.newFixedThreadPool(parallelThreads);

        try {
            for (TestCase testCase : testCases) {
                executor.submit(() -> {
                    try {
                        TestExecution execution = executeTestCase(testCase, environment);
                        execution.setTestBatch(batch); // Set the batch relationship
                        updateBatchStats(batch, execution);
                        testExecutionRepository.save(execution);
                    } catch (Exception e) {
                        log.error("Test execution failed for test case: {}", testCase.getName(), e);
                    }
                });
            }

            executor.shutdown();
            if (!executor.awaitTermination(30, TimeUnit.MINUTES)) {
                executor.shutdownNow();
                log.warn("Batch execution timed out: {}", batch.getBatchId());
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
            log.error("Batch execution interrupted: {}", batch.getBatchId());
        }
    }

    private TestExecution executeTestCase(TestCase testCase, String environment) {
        TestExecution execution;

        switch (testCase.getTestType()) {
            case WEB_UI:
                execution = webUITestExecutor.executeWebUITest(testCase, environment, "chrome");
                break;
            case API:
                execution = apiTestExecutor.executeApiTest(testCase, environment);
                break;
            default:
                throw new UnsupportedOperationException("Test type not supported: " + testCase.getTestType());
        }

        return execution;
    }

    private synchronized void updateBatchStats(TestBatch batch, TestExecution execution) {
        switch (execution.getStatus()) {
            case PASSED:
                batch.setPassedTests(batch.getPassedTests() + 1);
                break;
            case FAILED:
            case ERROR:
                batch.setFailedTests(batch.getFailedTests() + 1);
                break;
            case SKIPPED:
                batch.setSkippedTests(batch.getSkippedTests() + 1);
                break;
        }
    }

    public TestExecution executeSingleTest(Long testCaseId, String environment) {
        TestCase testCase = testCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new RuntimeException("Test case not found: " + testCaseId));

        TestExecution execution = executeTestCase(testCase, environment);
        return testExecutionRepository.save(execution);
    }

    @Async("testExecutionExecutor")
    public CompletableFuture<TestExecutionResultDTO> executeScheduledTests(TestSchedule schedule) {
        try {
            log.info("Executing scheduled tests for: {}", schedule.getName());

            // Create a new batch for scheduled execution
            TestBatch batch = new TestBatch();
            batch.setBatchId("SCHEDULED_" + schedule.getId() + "_" + System.currentTimeMillis());
            batch.setName("Scheduled: " + schedule.getName());
            batch.setEnvironment(schedule.getEnvironment());
            batch.setStatus(TestBatch.BatchStatus.RUNNING);
            batch.setStartTime(LocalDateTime.now());

            batch = testBatchRepository.save(batch);

            // Execute the batch
            CompletableFuture<TestBatch> batchFuture = executeBatch(
                batch,
                "scheduled",
                schedule.getEnvironment(),
                schedule.getParallelThreads()
            );

            // Convert to DTO
            return batchFuture.thenApply(this::convertToResultDTO);

        } catch (Exception e) {
            log.error("Error executing scheduled tests", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    public int cleanupOldExecutions(LocalDateTime cutoffDate) {
        try {
            log.info("Cleaning up test executions older than: {}", cutoffDate);

            List<TestExecution> oldExecutions = testExecutionRepository.findByStartTimeBefore(cutoffDate);
            int deletedCount = oldExecutions.size();

            testExecutionRepository.deleteAll(oldExecutions);

            log.info("Cleaned up {} old test executions", deletedCount);
            return deletedCount;

        } catch (Exception e) {
            log.error("Error during cleanup", e);
            return 0;
        }
    }

    public void generateWeeklySummaryReport(LocalDateTime weekStart, LocalDateTime weekEnd) {
        try {
            log.info("Generating weekly summary report from {} to {}", weekStart, weekEnd);

            List<TestBatch> weeklyBatches = testBatchRepository.findByStartTimeBetween(weekStart, weekEnd);

            // Generate summary statistics
            int totalBatches = weeklyBatches.size();
            int totalTests = weeklyBatches.stream().mapToInt(TestBatch::getTotalTests).sum();
            int totalPassed = weeklyBatches.stream().mapToInt(TestBatch::getPassedTests).sum();
            int totalFailed = weeklyBatches.stream().mapToInt(TestBatch::getFailedTests).sum();

            log.info("Weekly Summary - Batches: {}, Tests: {}, Passed: {}, Failed: {}",
                totalBatches, totalTests, totalPassed, totalFailed);

            // In a real implementation, you would generate and store the summary report

        } catch (Exception e) {
            log.error("Error generating weekly summary report", e);
        }
    }

    private TestExecutionResultDTO convertToResultDTO(TestBatch batch) {
        TestExecutionResultDTO dto = new TestExecutionResultDTO();
        dto.setBatchId(batch.getBatchId());
        dto.setStatus(batch.getStatus().toString());
        dto.setStartTime(batch.getStartTime());
        dto.setEndTime(batch.getEndTime());
        dto.setTotalTests(batch.getTotalTests());
        dto.setPassedTests(batch.getPassedTests());
        dto.setFailedTests(batch.getFailedTests());
        dto.setSkippedTests(batch.getSkippedTests());
        dto.setEnvironment(batch.getEnvironment());

        if (batch.getStartTime() != null && batch.getEndTime() != null) {
            dto.setDuration(java.time.Duration.between(batch.getStartTime(), batch.getEndTime()).toMillis());
        }

        return dto;
    }
}
