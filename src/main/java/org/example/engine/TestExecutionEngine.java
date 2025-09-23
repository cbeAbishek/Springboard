package org.example.engine;

import org.example.model.TestCase;
import org.example.model.TestExecution;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@Service
public class TestExecutionEngine {

    private static final Logger log = LoggerFactory.getLogger(TestExecutionEngine.class);

    @Autowired
    private WebUITestExecutor webUIExecutor;

    @Autowired
    private APITestExecutor apiExecutor;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationContext applicationContext;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    /**
     * Execute a single test case
     */
    public TestExecution executeTest(TestCase testCase, String environment) {
        log.info("Starting execution of test case: {} in environment: {}", testCase.getName(), environment);
        
        TestExecution execution = new TestExecution();
        execution.setTestCase(testCase);
        execution.setEnvironment(environment);
        execution.setStatus(TestExecution.ExecutionStatus.RUNNING);
        execution.setStartTime(LocalDateTime.now());
        execution.setExecutionId(UUID.randomUUID().toString());

        try {
            // Parse test data
            Map<String, Object> testData = parseTestData(testCase.getTestData());
            
            // Execute based on test type
            TestExecution.ExecutionStatus result = executeByType(testCase, testData, environment);
            execution.setStatus(result);

            if (result == TestExecution.ExecutionStatus.PASSED) {
                execution.setActualResult("Test completed successfully");
            }

        } catch (Exception e) {
            log.error("Error executing test case: {}", testCase.getName(), e);
            execution.setStatus(TestExecution.ExecutionStatus.FAILED);
            execution.setErrorMessage(e.getMessage());
            execution.setStackTrace(getStackTrace(e));
        } finally {
            execution.setEndTime(LocalDateTime.now());
            if (execution.getStartTime() != null && execution.getEndTime() != null) {
                execution.setExecutionDuration(
                    java.time.Duration.between(execution.getStartTime(), execution.getEndTime()).toMillis()
                );
            }
        }

        log.info("Completed execution of test case: {} with status: {}",
                testCase.getName(), execution.getStatus());
        return execution;
    }

    /**
     * Execute multiple test cases in parallel
     */
    public List<TestExecution> executeTestsParallel(List<TestCase> testCases, String environment, int maxThreads) {
        log.info("Starting parallel execution of {} test cases with {} threads", testCases.size(), maxThreads);

        ExecutorService parallelExecutor = Executors.newFixedThreadPool(maxThreads);

        try {
            List<Future<TestExecution>> futures = testCases.stream()
                .map(testCase -> parallelExecutor.submit(() -> executeTest(testCase, environment)))
                .collect(Collectors.toList());

            List<TestExecution> results = new ArrayList<>();
            for (Future<TestExecution> future : futures) {
                try {
                    results.add(future.get());
                } catch (Exception e) {
                    log.error("Error getting parallel execution result", e);
                }
            }

            return results;
        } finally {
            parallelExecutor.shutdown();
        }
    }

    /**
     * Execute test cases sequentially (individual execution)
     */
    public List<TestExecution> executeTestsSequential(List<TestCase> testCases, String environment) {
        log.info("Starting sequential execution of {} test cases", testCases.size());

        List<TestExecution> results = new ArrayList<>();
        for (TestCase testCase : testCases) {
            TestExecution result = executeTest(testCase, environment);
            results.add(result);
        }

        return results;
    }

    private TestExecution.ExecutionStatus executeByType(TestCase testCase, Map<String, Object> testData, String environment) {
        try {
            switch (testCase.getTestType()) {
                case UI:
                    return webUIExecutor.executeUITest(testCase, testData, environment);
                case API:
                    return apiExecutor.executeAPITest(testCase, testData, environment);
                case INTEGRATION:
                    // For integration tests, execute both UI and API components
                    TestExecution.ExecutionStatus apiResult = apiExecutor.executeAPITest(testCase, testData, environment);
                    if (apiResult == TestExecution.ExecutionStatus.PASSED) {
                        return webUIExecutor.executeUITest(testCase, testData, environment);
                    }
                    return apiResult;
                default:
                    log.warn("Unsupported test type: {}", testCase.getTestType());
                    return TestExecution.ExecutionStatus.SKIPPED;
            }
        } catch (Exception e) {
            log.error("Error executing test by type", e);
            return TestExecution.ExecutionStatus.FAILED;
        }
    }

    private Map<String, Object> parseTestData(String testDataJson) {
        try {
            if (testDataJson == null || testDataJson.trim().isEmpty()) {
                return new HashMap<>();
            }
            return objectMapper.readValue(testDataJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse test data JSON: {}", testDataJson, e);
            return new HashMap<>();
        }
    }

    private String getStackTrace(Exception e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    public void shutdown() {
        executorService.shutdown();
    }
}
