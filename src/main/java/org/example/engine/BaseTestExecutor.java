package org.example.engine;

import org.example.model.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Enhanced Base class for all test executors providing common functionality
 * Supports parallel execution, centralized reporting, modular design, and comprehensive metrics
 */
public abstract class BaseTestExecutor {

    protected static final Logger log = LoggerFactory.getLogger(BaseTestExecutor.class);

    // Thread-safe execution tracking
    protected final Map<String, ExecutionContext> activeExecutions = new ConcurrentHashMap<>();
    protected final AtomicLong executionCounter = new AtomicLong(0);

    // Framework configuration and metrics
    protected final Map<String, Object> frameworkMetrics = new ConcurrentHashMap<>();
    protected final List<ExecutionListener> listeners = new ArrayList<>();

    /**
     * Enhanced execution result structure for all test types
     */
    public static class TestExecutionResult {
        private boolean success;
        private String errorMessage;
        private String executionLogs;
        private List<String> screenshotPaths = new ArrayList<>();
        private Map<String, Object> executionMetrics = new HashMap<>();
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private long executionId;
        private String testType;
        private String testName;
        private String environment;
        private Map<String, Object> customData = new HashMap<>();
        private String requestResponseData;

        // Enhanced constructors
        public TestExecutionResult() {
            this.startTime = LocalDateTime.now();
            this.success = false;
        }

        public TestExecutionResult(String testName, String testType) {
            this();
            this.testName = testName;
            this.testType = testType;
        }

        // ...existing getters and setters...
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public String getExecutionLogs() { return executionLogs; }
        public void setExecutionLogs(String executionLogs) { this.executionLogs = executionLogs; }

        public List<String> getScreenshotPaths() { return screenshotPaths; }
        public void setScreenshotPaths(List<String> screenshotPaths) { this.screenshotPaths = screenshotPaths; }

        public Map<String, Object> getExecutionMetrics() { return executionMetrics; }
        public void setExecutionMetrics(Map<String, Object> executionMetrics) { this.executionMetrics = executionMetrics; }

        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

        public long getExecutionId() { return executionId; }
        public void setExecutionId(long executionId) { this.executionId = executionId; }

        public String getTestType() { return testType; }
        public void setTestType(String testType) { this.testType = testType; }

        // New getters and setters
        public String getTestName() { return testName; }
        public void setTestName(String testName) { this.testName = testName; }

        public String getEnvironment() { return environment; }
        public void setEnvironment(String environment) { this.environment = environment; }

        public Map<String, Object> getCustomData() { return customData; }
        public void setCustomData(Map<String, Object> customData) { this.customData = customData; }

        public String getRequestResponseData() { return requestResponseData; }
        public void setRequestResponseData(String requestResponseData) { this.requestResponseData = requestResponseData; }

        public void addCustomData(String key, Object value) {
            this.customData.put(key, value);
        }

        public void markCompleted() {
            this.endTime = LocalDateTime.now();
        }

        public long getDurationMs() {
            if (startTime != null && endTime != null) {
                return java.time.Duration.between(startTime, endTime).toMillis();
            }
            return 0;
        }
    }

    /**
     * Enhanced execution context for tracking parallel executions
     */
    public static class ExecutionContext {
        private final String executionId;
        private final TestCase testCase;
        private final Map<String, Object> testData;
        private final String environment;
        private final LocalDateTime startTime;
        private TestExecutionResult result;
        private CompletableFuture<TestExecutionResult> future;
        private ExecutionStatus status;
        private final Map<String, Object> contextMetrics = new HashMap<>();

        public ExecutionContext(String executionId, TestCase testCase, Map<String, Object> testData, String environment) {
            this.executionId = executionId;
            this.testCase = testCase;
            this.testData = testData;
            this.environment = environment;
            this.startTime = LocalDateTime.now();
            this.status = ExecutionStatus.INITIALIZING;
        }

        // ...existing getters...
        public String getExecutionId() { return executionId; }
        public TestCase getTestCase() { return testCase; }
        public Map<String, Object> getTestData() { return testData; }
        public String getEnvironment() { return environment; }
        public LocalDateTime getStartTime() { return startTime; }
        public TestExecutionResult getResult() { return result; }
        public void setResult(TestExecutionResult result) { this.result = result; }
        public CompletableFuture<TestExecutionResult> getFuture() { return future; }
        public void setFuture(CompletableFuture<TestExecutionResult> future) { this.future = future; }

        // New getters and setters
        public ExecutionStatus getStatus() { return status; }
        public void setStatus(ExecutionStatus status) { this.status = status; }
        public Map<String, Object> getContextMetrics() { return contextMetrics; }
        public void addMetric(String key, Object value) { this.contextMetrics.put(key, value); }
    }

    /**
     * Execution status enumeration for better tracking
     */
    public enum ExecutionStatus {
        INITIALIZING, RUNNING, COMPLETED, FAILED, CANCELLED, TIMEOUT
    }

    /**
     * Enhanced execution status tracking
     */
    public static class ExecutionStatusInfo {
        private String executionId;
        private String testCaseName;
        private String environment;
        private LocalDateTime startTime;
        private boolean isComplete;
        private boolean success;
        private long durationMs;
        private ExecutionStatus status;
        private String errorMessage;
        private Map<String, Object> metrics = new HashMap<>();

        // Getters and setters
        public String getExecutionId() { return executionId; }
        public void setExecutionId(String executionId) { this.executionId = executionId; }

        public String getTestCaseName() { return testCaseName; }
        public void setTestCaseName(String testCaseName) { this.testCaseName = testCaseName; }

        public String getEnvironment() { return environment; }
        public void setEnvironment(String environment) { this.environment = environment; }

        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

        public boolean isComplete() { return isComplete; }
        public void setComplete(boolean complete) { isComplete = complete; }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public long getDurationMs() { return durationMs; }
        public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

        public ExecutionStatus getStatus() { return status; }
        public void setStatus(ExecutionStatus status) { this.status = status; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public Map<String, Object> getMetrics() { return metrics; }
        public void setMetrics(Map<String, Object> metrics) { this.metrics = metrics; }
    }

    /**
     * Execution listener interface for extensible monitoring
     */
    public interface ExecutionListener {
        void onExecutionStart(String executionId, TestCase testCase);
        void onExecutionComplete(String executionId, TestExecutionResult result);
        void onExecutionError(String executionId, Exception error);
    }

    /**
     * Abstract method that must be implemented by specific test executors
     */
    public abstract TestExecutionResult execute(Map<String, Object> testData, TestCase testCase);

    /**
     * Enhanced parallel execution method with better monitoring
     */
    public CompletableFuture<TestExecutionResult> executeAsync(Map<String, Object> testData, TestCase testCase, String environment) {
        String executionId = generateExecutionId();
        ExecutionContext context = new ExecutionContext(executionId, testCase, testData, environment);

        log.info("Starting async execution {} for test case: {}", executionId, testCase.getName());
        notifyListeners(l -> l.onExecutionStart(executionId, testCase));

        CompletableFuture<TestExecutionResult> future = CompletableFuture.supplyAsync(() -> {
            try {
                context.setStatus(ExecutionStatus.RUNNING);
                TestExecutionResult result = execute(testData, testCase);
                result.setExecutionId(Long.parseLong(executionId));
                result.setTestType(this.getClass().getSimpleName());
                result.setTestName(testCase.getName());
                result.setEnvironment(environment);
                result.markCompleted();

                context.setResult(result);
                context.setStatus(result.isSuccess() ? ExecutionStatus.COMPLETED : ExecutionStatus.FAILED);

                log.info("Async execution {} completed with status: {}", executionId, result.isSuccess() ? "SUCCESS" : "FAILED");
                notifyListeners(l -> l.onExecutionComplete(executionId, result));
                return result;

            } catch (Exception e) {
                log.error("Async execution {} failed with exception", executionId, e);
                context.setStatus(ExecutionStatus.FAILED);

                TestExecutionResult errorResult = new TestExecutionResult(testCase.getName(), this.getClass().getSimpleName());
                errorResult.setSuccess(false);
                errorResult.setErrorMessage("Execution failed: " + e.getMessage());
                errorResult.setExecutionId(Long.parseLong(executionId));
                errorResult.setEnvironment(environment);
                errorResult.markCompleted();

                context.setResult(errorResult);
                notifyListeners(l -> l.onExecutionError(executionId, e));
                return errorResult;
            } finally {
                // Clean up execution context after completion
                activeExecutions.remove(executionId);
            }
        });

        context.setFuture(future);
        activeExecutions.put(executionId, context);

        return future;
    }

    /**
     * Enhanced status tracking with detailed information
     */
    public Map<String, ExecutionStatusInfo> getActiveExecutionsStatus() {
        Map<String, ExecutionStatusInfo> statusMap = new HashMap<>();

        activeExecutions.forEach((executionId, context) -> {
            ExecutionStatusInfo status = new ExecutionStatusInfo();
            status.setExecutionId(executionId);
            status.setTestCaseName(context.getTestCase().getName());
            status.setEnvironment(context.getEnvironment());
            status.setStartTime(context.getStartTime());
            status.setComplete(context.getFuture().isDone());
            status.setStatus(context.getStatus());

            if (context.getResult() != null) {
                status.setSuccess(context.getResult().isSuccess());
                status.setDurationMs(context.getResult().getDurationMs());
                status.setErrorMessage(context.getResult().getErrorMessage());
                status.setMetrics(context.getResult().getExecutionMetrics());
            }

            statusMap.put(executionId, status);
        });

        return statusMap;
    }

    /**
     * Get execution result by ID with enhanced error handling
     */
    public TestExecutionResult getExecutionResult(String executionId) {
        ExecutionContext context = activeExecutions.get(executionId);
        if (context != null) {
            if (context.getFuture().isDone()) {
                try {
                    return context.getFuture().get();
                } catch (Exception e) {
                    log.error("Error retrieving execution result for {}: {}", executionId, e.getMessage());
                    TestExecutionResult errorResult = new TestExecutionResult();
                    errorResult.setSuccess(false);
                    errorResult.setErrorMessage("Failed to retrieve result: " + e.getMessage());
                    return errorResult;
                }
            } else {
                // Return in-progress result
                TestExecutionResult inProgressResult = new TestExecutionResult();
                inProgressResult.setExecutionId(Long.parseLong(executionId));
                inProgressResult.setTestType("IN_PROGRESS");
                inProgressResult.setErrorMessage("Execution still in progress");
                return inProgressResult;
            }
        }
        return null;
    }

    /**
     * Cancel an active execution with cleanup
     */
    public boolean cancelExecution(String executionId) {
        ExecutionContext context = activeExecutions.get(executionId);
        if (context != null && context.getFuture() != null) {
            context.setStatus(ExecutionStatus.CANCELLED);
            boolean cancelled = context.getFuture().cancel(true);
            if (cancelled) {
                activeExecutions.remove(executionId);
                log.info("Execution {} cancelled successfully", executionId);

                // Create cancellation result
                TestExecutionResult cancelResult = new TestExecutionResult();
                cancelResult.setSuccess(false);
                cancelResult.setErrorMessage("Execution cancelled by user");
                cancelResult.setExecutionId(Long.parseLong(executionId));
                cancelResult.markCompleted();
                context.setResult(cancelResult);
            }
            return cancelled;
        }
        return false;
    }

    /**
     * Add execution listener for monitoring
     */
    public void addExecutionListener(ExecutionListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove execution listener
     */
    public void removeExecutionListener(ExecutionListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notify all listeners
     */
    protected void notifyListeners(java.util.function.Consumer<ExecutionListener> action) {
        listeners.forEach(listener -> {
            try {
                action.accept(listener);
            } catch (Exception e) {
                log.warn("Error notifying execution listener: {}", e.getMessage());
            }
        });
    }

    /**
     * Generate unique execution ID
     */
    protected String generateExecutionId() {
        return String.valueOf(executionCounter.incrementAndGet());
    }

    /**
     * Utility methods for logging and metrics
     */
    protected void logStep(String executionId, String message, List<String> logs) {
        String logEntry = String.format("[%s] %s", executionId, message);
        log.debug(logEntry);
        if (logs != null) {
            logs.add(logEntry);
        }
    }

    protected void captureMetric(TestExecutionResult result, String key, Object value) {
        result.getExecutionMetrics().put(key, value);
    }

    protected void captureFrameworkMetric(String key, Object value) {
        frameworkMetrics.put(key, value);
    }

    /**
     * Get framework-level metrics
     */
    public Map<String, Object> getFrameworkMetrics() {
        Map<String, Object> metrics = new HashMap<>(frameworkMetrics);
        metrics.put("active_executions", activeExecutions.size());
        metrics.put("total_executions", executionCounter.get());
        metrics.put("timestamp", LocalDateTime.now());
        return metrics;
    }

    /**
     * Cleanup resources
     */
    public void cleanup() {
        // Cancel all active executions
        activeExecutions.keySet().forEach(this::cancelExecution);

        // Clear listeners and metrics
        listeners.clear();
        frameworkMetrics.clear();

        log.info("Framework cleanup completed");
    }
}
