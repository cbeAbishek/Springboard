package org.example.service;

import org.example.model.*;
import org.example.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Service
public class MockDataService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MockDataService.class);

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private TestBatchRepository testBatchRepository;

    @Autowired
    private TestExecutionRepository testExecutionRepository;

    @Autowired
    private TestScheduleRepository testScheduleRepository;

    private final Random random = new Random();

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (testCaseRepository.count() == 0) {
            log.info("Creating mock data for automation framework...");
            createMockTestCases();
            createMockTestBatches();
            createMockTestExecutions();
            createMockSchedules();
            log.info("Mock data creation completed successfully!");
        } else {
            log.info("Mock data already exists, skipping creation.");
        }
    }

    private void createMockTestCases() {
        List<TestCase> testCases = Arrays.asList(
            // API Tests
            createTestCase("Get User API Test", "Verify user retrieval API works correctly",
                TestCase.TestType.API, "smoke", TestCase.Priority.HIGH,
                "{\"endpoint\":\"/api/users/2\",\"method\":\"GET\",\"expectedStatus\":200}"),

            createTestCase("Create User API Test", "Test user creation functionality",
                TestCase.TestType.API, "functional", TestCase.Priority.HIGH,
                "{\"endpoint\":\"/api/users\",\"method\":\"POST\",\"requestBody\":{\"name\":\"John\",\"job\":\"Developer\"},\"expectedStatus\":201}"),

            createTestCase("Update User API Test", "Test user update functionality",
                TestCase.TestType.API, "regression", TestCase.Priority.MEDIUM,
                "{\"endpoint\":\"/api/users/2\",\"method\":\"PUT\",\"requestBody\":{\"name\":\"John\",\"job\":\"Senior Developer\"},\"expectedStatus\":200}")
        );

        testCaseRepository.saveAll(testCases);
        log.info("Created {} test cases", testCases.size());
    }

    private void createMockTestBatches() {
        LocalDateTime now = LocalDateTime.now();

        List<TestBatch> batches = Arrays.asList(
            createTestBatch("SMOKE_" + System.currentTimeMillis(), "Daily Smoke Tests",
                TestBatch.BatchStatus.COMPLETED, now.minusHours(2), now.minusHours(1),
                "production", 15, 13, 2, 0),

            createTestBatch("REGRESSION_" + (System.currentTimeMillis() + 1), "Weekly Regression Suite",
                TestBatch.BatchStatus.COMPLETED, now.minusDays(1), now.minusDays(1).plusHours(3),
                "staging", 45, 40, 4, 1),

            createTestBatch("API_" + (System.currentTimeMillis() + 2), "API Integration Tests",
                TestBatch.BatchStatus.COMPLETED, now.minusHours(6), now.minusHours(5),
                "test", 20, 18, 2, 0),

            createTestBatch("E2E_" + (System.currentTimeMillis() + 3), "End-to-End User Flows",
                TestBatch.BatchStatus.RUNNING, now.minusMinutes(30), null,
                "staging", 25, 15, 0, 0),

            createTestBatch("PERF_" + (System.currentTimeMillis() + 4), "Performance Testing",
                TestBatch.BatchStatus.SCHEDULED, now.plusHours(2), null,
                "performance", 10, 0, 0, 0)
        );

        testBatchRepository.saveAll(batches);
        log.info("Created {} test batches", batches.size());
    }

    private void createMockTestExecutions() {
        List<TestCase> testCases = testCaseRepository.findAll();
        List<TestBatch> batches = testBatchRepository.findAll();

        for (TestBatch batch : batches) {
            if (batch.getStatus() == TestBatch.BatchStatus.COMPLETED ||
                batch.getStatus() == TestBatch.BatchStatus.RUNNING) {

                for (int i = 0; i < Math.min(batch.getTotalTests(), testCases.size()); i++) {
                    TestCase testCase = testCases.get(i % testCases.size());
                    TestExecution execution = createTestExecution(testCase, batch);
                    testExecutionRepository.save(execution);
                }
            }
        }

        log.info("Created test executions for batches");
    }

    private void createMockSchedules() {
        List<TestSchedule> schedules = Arrays.asList(
            createSchedule("Daily Smoke Tests", "0 8 * * *", "smoke", "production", true),
            createSchedule("Weekly Regression", "0 2 * * 0", "regression", "staging", true),
            createSchedule("API Health Check", "0 */4 * * *", "api", "production", true),
            createSchedule("Performance Tests", "0 23 * * 5", "performance", "performance", false),
            createSchedule("E2E User Flows", "0 6 * * 1,3,5", "e2e", "staging", true)
        );

        testScheduleRepository.saveAll(schedules);
        log.info("Created {} test schedules", schedules.size());
    }

    private TestCase createTestCase(String name, String description, TestCase.TestType type,
                                   String category, TestCase.Priority priority, String testData) {
        TestCase testCase = new TestCase();
        testCase.setName(name);
        testCase.setDescription(description);
        testCase.setTestType(type);
        testCase.setCategory(category);
        testCase.setPriority(priority);
        testCase.setTestData(testData);
        // expectedResult is non-nullable in the entity; provide a default to avoid DB constraint errors
        testCase.setExpectedResult("N/A");
        testCase.setIsActive(true);
        testCase.setCreatedBy("automation");
        return testCase;
    }

    private TestBatch createTestBatch(String batchId, String name, TestBatch.BatchStatus status,
                                     LocalDateTime startTime, LocalDateTime endTime, String environment,
                                     int total, int passed, int failed, int skipped) {
        TestBatch batch = new TestBatch();
        batch.setBatchId(batchId);
        batch.setName(name); // Changed from setBatchName
        batch.setStatus(status);
        batch.setStartedAt(startTime); // Changed from setStartTime
        batch.setCompletedAt(endTime); // Changed from setEndTime
        batch.setEnvironment(environment);
        batch.setTotalTests(total);
        batch.setPassedTests(passed);
        batch.setFailedTests(failed);
        batch.setSkippedTests(skipped);
        // Remove setParallelThreads as it doesn't exist in TestBatch
        batch.setCreatedBy("mock_service");
        return batch;
    }

    private TestExecution createTestExecution(TestCase testCase, TestBatch batch) {
        TestExecution execution = new TestExecution();
        execution.setTestCase(testCase);
        execution.setTestBatch(batch);
        execution.setExecutionId("EXEC_" + System.currentTimeMillis() + "_" + random.nextInt(1000));

        // Simulate realistic test results
        double successRate = 0.85; // 85% success rate
        if (random.nextDouble() < successRate) {
            execution.setStatus(TestExecution.ExecutionStatus.PASSED);
            execution.setActualResult("Test completed successfully");
        } else {
            execution.setStatus(TestExecution.ExecutionStatus.FAILED);
            execution.setErrorMessage("Element not found: locator timeout");
            execution.setActualResult("Test failed due to element interaction issue");
        }

        execution.setStartTime(batch.getStartedAt().plusMinutes(random.nextInt(60)));
        execution.setEndTime(execution.getStartTime().plusSeconds(30 + random.nextInt(120)));
        execution.setExecutionDuration(java.time.Duration.between(execution.getStartTime(), execution.getEndTime()).toMillis());
        execution.setEnvironment(batch.getEnvironment());
        execution.setBrowser("chrome");
        execution.setExecutedBy("automation");

        return execution;
    }

    private TestSchedule createSchedule(String name, String cronExpression, String testSuite,
                                       String environment, boolean isActive) {
        TestSchedule schedule = new TestSchedule();
        schedule.setName(name); // Changed from setScheduleName
        schedule.setCronExpression(cronExpression);
        schedule.setTestType(testSuite); // Changed from setTestSuite to setTestType
        schedule.setEnvironment(environment);
        schedule.setIsActive(isActive);
        // Remove methods that don't exist in the corrected TestSchedule model
        schedule.setCreatedBy("mock_service");
        schedule.setNextExecution(calculateNextExecution(cronExpression));
        return schedule;
    }

    private LocalDateTime calculateNextExecution(String cronExpression) {
        // Simplified next execution calculation
        LocalDateTime now = LocalDateTime.now();
        if (cronExpression.contains("8 * * *")) { // Daily at 8 AM
            return now.plusDays(1).withHour(8).withMinute(0).withSecond(0);
        } else if (cronExpression.contains("*/4 * * *")) { // Every 4 hours
            return now.plusHours(4);
        } else if (cronExpression.contains("* * 0")) { // Weekly
            return now.plusWeeks(1);
        }
        return now.plusDays(1);
    }
}
