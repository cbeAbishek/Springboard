package org.example.controller;

import org.example.model.TestBatch;
import org.example.model.TestExecution;
import org.example.service.TestExecutionService;
import org.example.repository.TestBatchRepository;
import org.example.repository.TestExecutionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/execution")
@CrossOrigin(origins = "*")
public class TestExecutionController {

    private static final Logger log = LoggerFactory.getLogger(TestExecutionController.class);

    @Autowired
    private TestExecutionService testExecutionService;

    /**
     * Execute single test case
     */
    @PostMapping("/single/{testCaseId}")
    public ResponseEntity<Map<String, Object>> executeSingleTest(
            @PathVariable Long testCaseId,
            @RequestParam(defaultValue = "dev") String environment) {

        try {
            log.info("Executing single test case: {} in environment: {}", testCaseId, environment);

            TestExecution result = testExecutionService.executeSingleTest(testCaseId, environment);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("executionId", result.getId());
            response.put("status", result.getStatus());
            response.put("testCase", result.getTestCase().getName());
            response.put("duration", result.getExecutionDuration());
            response.put("screenshotPath", result.getScreenshotPath());
            response.put("message", "Test executed successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to execute single test", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Execute multiple test cases sequentially
     */
    @PostMapping("/sequential")
    public ResponseEntity<Map<String, Object>> executeSequential(@RequestBody ExecutionRequest request) {
        try {
            log.info("Executing {} test cases sequentially in environment: {}",
                    request.getTestCaseIds().size(), request.getEnvironment());

            List<TestExecution> results = testExecutionService.executeTestsSequential(
                    request.getTestCaseIds(), request.getEnvironment());

            Map<String, Object> response = createExecutionResponse(results, "Sequential execution completed");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to execute sequential tests", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Execute multiple test cases in parallel
     */
    @PostMapping("/parallel")
    public ResponseEntity<Map<String, Object>> executeParallel(@RequestBody ExecutionRequest request) {
        try {
            log.info("Executing {} test cases in parallel with {} threads in environment: {}",
                    request.getTestCaseIds().size(), request.getMaxThreads(), request.getEnvironment());

            CompletableFuture<List<TestExecution>> futureResults = testExecutionService.executeTestsParallel(
                    request.getTestCaseIds(), request.getEnvironment(), request.getMaxThreads());

            List<TestExecution> results = futureResults.get();

            Map<String, Object> response = createExecutionResponse(results, "Parallel execution completed");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to execute parallel tests", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Execute BlazeDemo UI tests
     */
    @PostMapping("/blazedemo")
    public ResponseEntity<Map<String, Object>> executeBlazeDemo(
            @RequestParam(defaultValue = "dev") String environment,
            @RequestParam(defaultValue = "false") boolean parallel) {

        try {
            log.info("Executing BlazeDemo tests in environment: {} (parallel: {})", environment, parallel);

            List<TestExecution> results = testExecutionService.executeBlazeDemo(environment, parallel);

            Map<String, Object> response = createExecutionResponse(results, "BlazeDemo tests completed");
            response.put("testSuite", "BlazeDemo");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to execute BlazeDemo tests", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Execute ReqRes API integration tests
     */
    @PostMapping("/reqres")
    public ResponseEntity<Map<String, Object>> executeReqRes(
            @RequestParam(defaultValue = "dev") String environment,
            @RequestParam(defaultValue = "false") boolean parallel) {

        try {
            log.info("Executing ReqRes tests in environment: {} (parallel: {})", environment, parallel);

            List<TestExecution> results = testExecutionService.executeReqRes(environment, parallel);

            Map<String, Object> response = createExecutionResponse(results, "ReqRes API tests completed");
            response.put("testSuite", "ReqRes");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to execute ReqRes tests", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Execute regression tests
     */
    @PostMapping("/regression")
    public ResponseEntity<Map<String, Object>> executeRegression(
            @RequestParam(defaultValue = "dev") String environment,
            @RequestParam(defaultValue = "true") boolean parallel) {

        try {
            log.info("Executing regression tests in environment: {} (parallel: {})", environment, parallel);

            List<TestExecution> results = testExecutionService.executeRegressionTests(environment, parallel);

            Map<String, Object> response = createExecutionResponse(results, "Regression tests completed");
            response.put("testSuite", "Regression");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to execute regression tests", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Execute test batch with comprehensive reporting
     */
    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> executeBatch(@RequestBody BatchRequest request) {
        try {
            log.info("Executing test batch: {} with {} test cases", request.getBatchName(),
                    request.getTestCaseIds().size());

            TestBatch batch = testExecutionService.executeTestBatch(
                    request.getBatchName(),
                    request.getTestCaseIds(),
                    request.getEnvironment(),
                    request.isParallel(),
                    request.getMaxThreads()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("batchId", batch.getBatchId());
            response.put("status", batch.getStatus());
            response.put("totalTests", batch.getTotalTests());
            response.put("passedTests", batch.getPassedTests());
            response.put("failedTests", batch.getFailedTests());
            response.put("skippedTests", batch.getSkippedTests());
            response.put("message", "Batch execution completed");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to execute batch", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Get execution status
     */
    @GetMapping("/status/{executionId}")
    public ResponseEntity<Map<String, Object>> getExecutionStatus(@PathVariable Long executionId) {
        try {
            return testExecutionService.findById(executionId)
                    .map(execution -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("executionId", execution.getId());
                        response.put("status", execution.getStatus());
                        response.put("testCase", execution.getTestCase().getName());
                        response.put("startTime", execution.getStartTime());
                        response.put("endTime", execution.getEndTime());
                        response.put("duration", execution.getExecutionDuration());
                        response.put("errorMessage", execution.getErrorMessage());
                        response.put("screenshotPath", execution.getScreenshotPath());
                        return ResponseEntity.ok(response);
                    })
                    .orElse(ResponseEntity.notFound().build());

        } catch (Exception e) {
            log.error("Failed to get execution status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Get execution statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> stats = testExecutionService.getExecutionStatistics();
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Failed to get statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Get recent executions
     */
    @GetMapping("/recent")
    public ResponseEntity<List<Map<String, Object>>> getRecentExecutions(
            @RequestParam(defaultValue = "10") int limit) {

        try {
            List<TestExecution> executions = testExecutionService.getRecentExecutions(limit);
            List<Map<String, Object>> response = executions.stream()
                    .map(this::convertToMap)
                    .collect(java.util.stream.Collectors.toList());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get recent executions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Helper method to create execution response
     */
    private Map<String, Object> createExecutionResponse(List<TestExecution> results, String message) {
        Map<String, Object> response = new HashMap<>();

        long passed = results.stream().mapToLong(r -> r.getStatus() == TestExecution.ExecutionStatus.PASSED ? 1 : 0).sum();
        long failed = results.stream().mapToLong(r -> r.getStatus() == TestExecution.ExecutionStatus.FAILED ? 1 : 0).sum();
        long skipped = results.stream().mapToLong(r -> r.getStatus() == TestExecution.ExecutionStatus.SKIPPED ? 1 : 0).sum();

        response.put("success", true);
        response.put("totalTests", results.size());
        response.put("passedTests", passed);
        response.put("failedTests", failed);
        response.put("skippedTests", skipped);
        response.put("passRate", results.size() > 0 ? (double) passed / results.size() * 100 : 0);
        response.put("executions", results.stream().map(this::convertToMap).collect(java.util.stream.Collectors.toList()));
        response.put("message", message);

        return response;
    }

    /**
     * Convert execution to map
     */
    private Map<String, Object> convertToMap(TestExecution execution) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", execution.getId());
        map.put("testCaseName", execution.getTestCase().getName());
        map.put("testType", execution.getTestCase().getTestType());
        map.put("status", execution.getStatus());
        map.put("startTime", execution.getStartTime());
        map.put("endTime", execution.getEndTime());
        map.put("duration", execution.getExecutionDuration());
        map.put("environment", execution.getEnvironment());
        map.put("screenshotPath", execution.getScreenshotPath());
        map.put("errorMessage", execution.getErrorMessage());
        return map;
    }

    // Request DTOs
    public static class ExecutionRequest {
        private List<Long> testCaseIds;
        private String environment = "dev";
        private int maxThreads = 4;

        // Getters and setters
        public List<Long> getTestCaseIds() { return testCaseIds; }
        public void setTestCaseIds(List<Long> testCaseIds) { this.testCaseIds = testCaseIds; }
        public String getEnvironment() { return environment; }
        public void setEnvironment(String environment) { this.environment = environment; }
        public int getMaxThreads() { return maxThreads; }
        public void setMaxThreads(int maxThreads) { this.maxThreads = maxThreads; }
    }

    public static class BatchRequest {
        private String batchName;
        private List<Long> testCaseIds;
        private String environment = "dev";
        private boolean parallel = false;
        private int maxThreads = 4;

        // Getters and setters
        public String getBatchName() { return batchName; }
        public void setBatchName(String batchName) { this.batchName = batchName; }
        public List<Long> getTestCaseIds() { return testCaseIds; }
        public void setTestCaseIds(List<Long> testCaseIds) { this.testCaseIds = testCaseIds; }
        public String getEnvironment() { return environment; }
        public void setEnvironment(String environment) { this.environment = environment; }
        public boolean isParallel() { return parallel; }
        public void setParallel(boolean parallel) { this.parallel = parallel; }
        public int getMaxThreads() { return maxThreads; }
        public void setMaxThreads(int maxThreads) { this.maxThreads = maxThreads; }
    }
}
