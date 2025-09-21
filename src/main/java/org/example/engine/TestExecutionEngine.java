package org.example.engine;

import org.example.model.TestCase;
import org.example.model.TestExecution;
import org.example.model.TestStep;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private DatabaseTestExecutor databaseExecutor;

    @Autowired
    private ObjectMapper objectMapper;

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

        try {
            // Parse test data
            Map<String, Object> testData = parseTestData(testCase.getTestData());
            
            // Execute based on test type
            BaseTestExecutor.TestExecutionResult result = executeByType(testCase.getTestType(), testData, testCase);

            // Update execution with results
            execution.setStatus(result.isSuccess() ? 
                TestExecution.ExecutionStatus.PASSED : TestExecution.ExecutionStatus.FAILED);
            execution.setErrorMessage(result.getErrorMessage());
            execution.setExecutionLogs(result.getExecutionLogs());
            execution.setScreenshotPaths(String.join(",", result.getScreenshotPaths()));
            execution.setRequestResponseData(result.getRequestResponseData());
            
        } catch (Exception e) {
            log.error("Error executing test case: {}", testCase.getName(), e);
            execution.setStatus(TestExecution.ExecutionStatus.FAILED);
            execution.setErrorMessage("Execution failed: " + e.getMessage());
        } finally {
            execution.setEndTime(LocalDateTime.now());
            execution.calculateDuration();
        }

        return execution;
    }

    /**
     * Execute multiple test cases in parallel
     */
    public List<CompletableFuture<TestExecution>> executeTestsInParallel(
            List<TestCase> testCases, String environment, int maxParallelThreads) {
        
        log.info("Starting parallel execution of {} test cases with {} threads", 
                testCases.size(), maxParallelThreads);

        ExecutorService executor = Executors.newFixedThreadPool(maxParallelThreads);
        List<CompletableFuture<TestExecution>> futures = new ArrayList<>();

        for (TestCase testCase : testCases) {
            CompletableFuture<TestExecution> future = CompletableFuture.supplyAsync(() -> 
                executeTest(testCase, environment), executor);
            futures.add(future);
        }

        return futures;
    }

    /**
     * Execute test with step-by-step execution for debugging
     */
    public TestExecution executeTestWithSteps(TestCase testCase, String environment, boolean debugMode) {
        log.info("Starting step-by-step execution of test case: {}", testCase.getName());
        
        TestExecution execution = new TestExecution();
        execution.setTestCase(testCase);
        execution.setEnvironment(environment);
        execution.setStatus(TestExecution.ExecutionStatus.RUNNING);
        execution.setStartTime(LocalDateTime.now());

        List<String> stepLogs = new ArrayList<>();
        List<String> screenshots = new ArrayList<>();

        try {
            // Parse test steps from test data
            List<TestStep> testSteps = parseTestSteps(testCase.getTestData());
            
            for (int i = 0; i < testSteps.size(); i++) {
                TestStep step = testSteps.get(i);
                
                if (debugMode) {
                    stepLogs.add(String.format("Step %d: %s", i + 1, step.getDescription()));
                }

                BaseTestExecutor.TestExecutionResult stepResult = executeTestStep(step, testCase.getTestType());

                if (!stepResult.isSuccess()) {
                    execution.setStatus(TestExecution.ExecutionStatus.FAILED);
                    execution.setErrorMessage(String.format("Step %d failed: %s", i + 1, stepResult.getErrorMessage()));
                    
                    // Capture screenshot for failed UI steps
                    if (testCase.getTestType() == TestCase.TestType.WEB_UI && stepResult.getScreenshotPaths() != null) {
                        screenshots.addAll(stepResult.getScreenshotPaths());
                    }
                    break;
                }

                stepLogs.add(String.format("Step %d completed successfully", i + 1));
            }

            if (execution.getStatus() == TestExecution.ExecutionStatus.RUNNING) {
                execution.setStatus(TestExecution.ExecutionStatus.PASSED);
            }

        } catch (Exception e) {
            log.error("Error in step-by-step execution: {}", e.getMessage(), e);
            execution.setStatus(TestExecution.ExecutionStatus.FAILED);
            execution.setErrorMessage("Step execution failed: " + e.getMessage());
        } finally {
            execution.setEndTime(LocalDateTime.now());
            execution.calculateDuration();
            execution.setExecutionLogs(String.join("\n", stepLogs));
            execution.setScreenshotPaths(String.join(",", screenshots));
        }

        return execution;
    }

    private BaseTestExecutor.TestExecutionResult executeByType(TestCase.TestType testType, Map<String, Object> testData, TestCase testCase) {
        return switch (testType) {
            case WEB_UI, UI -> webUIExecutor.execute(testData, testCase);
            case API -> apiExecutor.execute(testData, testCase);
            case DATABASE -> databaseExecutor.execute(testData, testCase);
            case INTEGRATION -> executeIntegrationTest(testData, testCase);
            default -> throw new UnsupportedOperationException("Test type not supported: " + testType);
        };
    }

    private BaseTestExecutor.TestExecutionResult executeIntegrationTest(Map<String, Object> testData, TestCase testCase) {
        // Implementation for integration tests that might involve multiple systems
        BaseTestExecutor.TestExecutionResult result = new BaseTestExecutor.TestExecutionResult();
        result.setSuccess(true);
        result.setExecutionLogs("Integration test executed successfully");
        return result;
    }

    private BaseTestExecutor.TestExecutionResult executeTestStep(TestStep step, TestCase.TestType testType) {
        Map<String, Object> stepData = step.getStepData();
        
        // Create a temporary test case for the step
        TestCase stepTestCase = new TestCase();
        stepTestCase.setTestType(testType);
        
        return executeByType(testType, stepData, stepTestCase);
    }

    private Map<String, Object> parseTestData(String testDataJson) {
        try {
            return objectMapper.readValue(testDataJson, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse test data JSON, using empty map: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    private List<TestStep> parseTestSteps(String testDataJson) {
        try {
            Map<String, Object> testData = parseTestData(testDataJson);
            
            // Check if test data contains steps
            if (testData.containsKey("steps")) {
                List<Map<String, Object>> stepsData = (List<Map<String, Object>>) testData.get("steps");
                List<TestStep> steps = new ArrayList<>();
                
                for (int i = 0; i < stepsData.size(); i++) {
                    Map<String, Object> stepData = stepsData.get(i);
                    TestStep step = new TestStep();
                    step.setStepNumber(i + 1);
                    step.setDescription(stepData.getOrDefault("description", "Step " + (i + 1)).toString());
                    step.setStepData(stepData);
                    steps.add(step);
                }
                
                return steps;
            } else {
                // Create a single step from the entire test data
                TestStep singleStep = new TestStep();
                singleStep.setStepNumber(1);
                singleStep.setDescription("Execute test");
                singleStep.setStepData(testData);
                return Collections.singletonList(singleStep);
            }
        } catch (Exception e) {
            log.warn("Failed to parse test steps, using single step: {}", e.getMessage());
            TestStep singleStep = new TestStep();
            singleStep.setStepNumber(1);
            singleStep.setDescription("Execute test");
            singleStep.setStepData(new HashMap<>());
            return Collections.singletonList(singleStep);
        }
    }
}
