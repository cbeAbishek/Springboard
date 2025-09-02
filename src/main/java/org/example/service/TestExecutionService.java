package org.example.service;

import org.example.model.TestBatch;
import org.example.model.TestCase;
import org.example.model.TestExecution;
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
}
