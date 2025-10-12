package org.example.service;

import org.example.model.TestBatch;
import org.example.model.TestCase;
import org.example.model.TestExecution;
import org.example.repository.TestBatchRepository;
import org.example.repository.TestCaseRepository;
import org.example.repository.TestExecutionRepository;
import org.example.engine.TestExecutionEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private TestExecutionEngine executionEngine;

    /**
     * Save test execution
     */
    public TestExecution save(TestExecution execution) {
        return testExecutionRepository.save(execution);
    }

    /**
     * Find execution by ID
     */
    public Optional<TestExecution> findById(Long id) {
        return testExecutionRepository.findById(id);
    }

    /**
     * Get all executions for a test case
     */
    public List<TestExecution> getExecutionsByTestCase(Long testCaseId) {
        return testExecutionRepository.findByTestCaseIdOrderByStartTimeDesc(testCaseId);
    }

    /**
     * Execute single test case
     */
    public TestExecution executeSingleTest(Long testCaseId, String environment) {
        log.info("Executing single test case ID: {} in environment: {}", testCaseId, environment);

        Optional<TestCase> testCaseOpt = testCaseRepository.findById(testCaseId);
        if (testCaseOpt.isEmpty()) {
            throw new RuntimeException("Test case not found: " + testCaseId);
        }

        TestCase testCase = testCaseOpt.get();
        TestExecution execution = executionEngine.executeTest(testCase, environment);

        // Save execution to database
        return save(execution);
    }

    /**
     * Execute multiple test cases sequentially
     */
    public List<TestExecution> executeTestsSequential(List<Long> testCaseIds, String environment) {
        log.info("Executing {} test cases sequentially in environment: {}", testCaseIds.size(), environment);

        List<TestCase> testCases = testCaseRepository.findAllById(testCaseIds);
        List<TestExecution> results = executionEngine.executeTestsSequential(testCases, environment);

        // Save all executions
        return testExecutionRepository.saveAll(results);
    }

    /**
     * Execute multiple test cases in parallel
     */
    @Async
    public CompletableFuture<List<TestExecution>> executeTestsParallel(List<Long> testCaseIds, String environment, int maxThreads) {
        log.info("Executing {} test cases in parallel with {} threads in environment: {}",
                testCaseIds.size(), maxThreads, environment);

        List<TestCase> testCases = testCaseRepository.findAllById(testCaseIds);
        List<TestExecution> results = executionEngine.executeTestsParallel(testCases, environment, maxThreads);

        // Save all executions
        List<TestExecution> savedResults = testExecutionRepository.saveAll(results);
        return CompletableFuture.completedFuture(savedResults);
    }

    /**
     * Execute test batch
     */
    public TestBatch executeTestBatch(String batchName, List<Long> testCaseIds, String environment, boolean parallel, int maxThreads) {
        log.info("Executing test batch: {} with {} test cases", batchName, testCaseIds.size());

        // Create test batch
        TestBatch batch = new TestBatch();
        batch.setBatchId(java.util.UUID.randomUUID().toString());
        batch.setName(batchName);
        batch.setDescription("Batch execution of " + testCaseIds.size() + " test cases");
        batch.setStatus(TestBatch.BatchStatus.RUNNING);
        batch.setCreatedAt(LocalDateTime.now());
        batch.setStartedAt(LocalDateTime.now());
        batch.setTotalTests(testCaseIds.size());
        batch.setEnvironment(environment);
        batch.setCreatedBy("system");

        batch = testBatchRepository.save(batch);

        try {
            List<TestExecution> results;
            if (parallel) {
                results = executeTestsParallel(testCaseIds, environment, maxThreads).get();
            } else {
                results = executeTestsSequential(testCaseIds, environment);
            }

            // Update batch results
            long passed = results.stream().mapToLong(r -> r.getStatus() == TestExecution.ExecutionStatus.PASSED ? 1 : 0).sum();
            long failed = results.stream().mapToLong(r -> r.getStatus() == TestExecution.ExecutionStatus.FAILED ? 1 : 0).sum();
            long skipped = results.stream().mapToLong(r -> r.getStatus() == TestExecution.ExecutionStatus.SKIPPED ? 1 : 0).sum();

            batch.setPassedTests((int) passed);
            batch.setFailedTests((int) failed);
            batch.setSkippedTests((int) skipped);
            batch.setStatus(failed > 0 ? TestBatch.BatchStatus.FAILED : TestBatch.BatchStatus.COMPLETED);
            batch.setCompletedAt(LocalDateTime.now());

            return testBatchRepository.save(batch);

        } catch (Exception e) {
            log.error("Error executing test batch", e);
            batch.setStatus(TestBatch.BatchStatus.FAILED);
            batch.setCompletedAt(LocalDateTime.now());
            return testBatchRepository.save(batch);
        }
    }

    /**
     * Execute BlazeDemo UI tests
     */
    public List<TestExecution> executeBlazeDemo(String environment, boolean parallel) {
        List<TestCase> blazeTests = testCaseRepository.findByNameContaining("BlazeDemo");
        log.info("Found {} BlazeDemo test cases", blazeTests.size());

        if (parallel) {
            return executionEngine.executeTestsParallel(blazeTests, environment, 4);
        } else {
            return executionEngine.executeTestsSequential(blazeTests, environment);
        }
    }

    /**
     * Execute ReqRes API integration tests
     */
    public List<TestExecution> executeReqRes(String environment, boolean parallel) {
        List<TestCase> reqresTests = testCaseRepository.findByNameContaining("ReqRes");
        log.info("Found {} ReqRes test cases", reqresTests.size());

        if (parallel) {
            return executionEngine.executeTestsParallel(reqresTests, environment, 4);
        } else {
            return executionEngine.executeTestsSequential(reqresTests, environment);
        }
    }

    /**
     * Execute regression tests
     */
    public List<TestExecution> executeRegressionTests(String environment, boolean parallel) {
        List<TestCase> regressionTests = testCaseRepository.findByPriorityIn(
                List.of(TestCase.Priority.HIGH, TestCase.Priority.CRITICAL)
        );
        log.info("Found {} regression test cases", regressionTests.size());

        if (parallel) {
            return executionEngine.executeTestsParallel(regressionTests, environment, 6);
        } else {
            return executionEngine.executeTestsSequential(regressionTests, environment);
        }
    }

    /**
     * Get execution statistics
     */
    public java.util.Map<String, Object> getExecutionStatistics() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();

        long totalExecutions = testExecutionRepository.count();
        long passedExecutions = testExecutionRepository.countByStatus(TestExecution.ExecutionStatus.PASSED);
        long failedExecutions = testExecutionRepository.countByStatus(TestExecution.ExecutionStatus.FAILED);
        long skippedExecutions = testExecutionRepository.countByStatus(TestExecution.ExecutionStatus.SKIPPED);

        stats.put("totalExecutions", totalExecutions);
        stats.put("passedExecutions", passedExecutions);
        stats.put("failedExecutions", failedExecutions);
        stats.put("skippedExecutions", skippedExecutions);
        stats.put("passRate", totalExecutions > 0 ? (double) passedExecutions / totalExecutions * 100 : 0);

        return stats;
    }

    /**
     * Get recent executions
     */
    public List<TestExecution> getRecentExecutions(int limit) {
        return testExecutionRepository.findTop10ByOrderByStartTimeDesc().stream()
                .limit(limit)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Clean up old executions
     */
    public void cleanupOldExecutions(int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        List<TestExecution> oldExecutions = testExecutionRepository.findByStartTimeBefore(cutoffDate);

        log.info("Cleaning up {} old executions before {}", oldExecutions.size(), cutoffDate);
        testExecutionRepository.deleteAll(oldExecutions);
    }
}
