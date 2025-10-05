package org.automation.dashboard.service;

import org.automation.analytics.model.ExecutionLog;
import org.automation.analytics.repo.ExecutionLogRepository;
import org.automation.dashboard.model.ExecutionStatus;
import org.automation.dashboard.model.TestExecutionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for managing test execution lifecycle
 */
@Service
public class TestExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(TestExecutionService.class);

    @Autowired(required = false)
    private ExecutionLogRepository executionLogRepository;

    // Store active executions
    private final Map<String, ExecutionStatus> activeExecutions = new ConcurrentHashMap<>();
    private final Map<String, Process> executionProcesses = new ConcurrentHashMap<>();

    /**
     * Execute a test suite asynchronously
     */
    public String executeTestSuite(TestExecutionRequest request) throws Exception {
        String executionId = "exec_" + System.currentTimeMillis();

        logger.info("Starting test execution - ID: {}, Suite: {}, Browser: {}",
                   executionId, request.getSuite(), request.getBrowser());

        // Create execution status
        ExecutionStatus status = new ExecutionStatus(executionId);
        status.setSuite(request.getSuite());
        status.setMessage("Initializing test execution...");
        activeExecutions.put(executionId, status);

        // Create execution log entry
        if (executionLogRepository != null) {
            ExecutionLog log = new ExecutionLog();
            log.setTestName("TestSuite_" + request.getSuite());
            log.setTestClass(request.getSuite() + "Tests");
            log.setStatus("RUNNING");
            log.setStartTime(LocalDateTime.now());
            log.setBrowser(request.getBrowser());
            log.setSuiteId(executionId);
            log.setTestSuite(request.getSuite());
            executionLogRepository.save(log);
        }

        // Execute tests asynchronously
        executeTestsAsync(executionId, request);

        return executionId;
    }

    /**
     * Execute tests in a separate thread
     */
    @Async
    protected void executeTestsAsync(String executionId, TestExecutionRequest request) {
        logger.info("Async execution started for: {}", executionId);
        ExecutionStatus status = activeExecutions.get(executionId);

        try {
            // Build Maven command
            List<String> command = buildMavenCommand(request);

            logger.info("Executing command: {}", String.join(" ", command));
            status.setMessage("Executing test suite...");
            status.setProgress(5);

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(new File(System.getProperty("user.dir")));
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            executionProcesses.put(executionId, process);

            // Monitor process output
            monitorProcessOutput(executionId, process, status);

            // Wait for completion
            int exitCode = process.waitFor();

            logger.info("Test execution {} completed with exit code: {}", executionId, exitCode);

            // Update final status
            if (exitCode == 0) {
                status.markCompleted();
                status.setMessage("All tests completed successfully");
                updateExecutionLog(executionId, "PASSED", null);
            } else {
                status.setProgress(100);
                status.setStatus("COMPLETED");
                status.setMessage("Tests completed with failures");
                updateExecutionLog(executionId, "FAILED", "Some tests failed");
            }

        } catch (InterruptedException e) {
            logger.warn("Test execution {} was interrupted", executionId);
            status.markStopped();
            updateExecutionLog(executionId, "STOPPED", "Execution interrupted");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Error executing tests for {}: {}", executionId, e.getMessage(), e);
            status.markFailed(e.getMessage());
            updateExecutionLog(executionId, "FAILED", e.getMessage());
        } finally {
            executionProcesses.remove(executionId);
            // Keep status for 5 minutes for polling
            scheduleStatusCleanup(executionId);
        }
    }

    /**
     * Monitor process output and update progress
     */
    private void monitorProcessOutput(String executionId, Process process, ExecutionStatus status) {
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                Pattern testPattern = Pattern.compile("\\[.*?\\].*?Running (.*?)");
                Pattern progressPattern = Pattern.compile("Tests run: (\\d+), Failures: (\\d+), Errors: (\\d+), Skipped: (\\d+)");

                int totalTests = 0;
                int completedTests = 0;

                while ((line = reader.readLine()) != null) {
                    logger.debug("Execution {}: {}", executionId, line);

                    // Detect current test
                    Matcher testMatcher = testPattern.matcher(line);
                    if (testMatcher.find()) {
                        String currentTest = testMatcher.group(1);
                        status.setCurrentTest(currentTest);
                        logger.info("Execution {} - Running: {}", executionId, currentTest);
                    }

                    // Parse progress
                    Matcher progressMatcher = progressPattern.matcher(line);
                    if (progressMatcher.find()) {
                        int run = Integer.parseInt(progressMatcher.group(1));
                        int failures = Integer.parseInt(progressMatcher.group(2));
                        int errors = Integer.parseInt(progressMatcher.group(3));

                        completedTests = run;
                        status.setCompletedTests(completedTests);
                        status.setPassedTests(run - failures - errors);
                        status.setFailedTests(failures + errors);

                        if (totalTests > 0) {
                            // Calculate accurate progress
                            int progress = (int) ((completedTests * 100.0) / totalTests);
                            status.setProgress(Math.min(100, progress));
                        } else {
                            // Estimate progress
                            status.setProgress(Math.min(90, status.getProgress() + 10));
                        }

                        logger.info("Execution {} - Progress: {}% ({}/{}), Passed: {}, Failed: {}",
                                   executionId, status.getProgress(), completedTests, totalTests,
                                   status.getPassedTests(), status.getFailedTests());
                    }

                    // Try to detect total tests
                    if (line.contains("Total tests run:") || line.contains("Tests to run:")) {
                        Pattern totalPattern = Pattern.compile("(\\d+)");
                        Matcher totalMatcher = totalPattern.matcher(line);
                        if (totalMatcher.find()) {
                            totalTests = Integer.parseInt(totalMatcher.group(1));
                            status.setTotalTests(totalTests);
                            logger.info("Execution {} - Total tests detected: {}", executionId, totalTests);
                        }
                    }
                }
            } catch (IOException e) {
                logger.error("Error monitoring process output for {}: {}", executionId, e.getMessage());
            }
        }).start();
    }

    /**
     * Build Maven command for test execution
     */
    private List<String> buildMavenCommand(TestExecutionRequest request) {
        List<String> command = new ArrayList<>();

        // Use mvnw if available, otherwise mvn
        File mvnw = new File("./mvnw");
        if (mvnw.exists() && mvnw.canExecute()) {
            command.add("./mvnw");
        } else {
            command.add("mvn");
        }

        command.add("clean");
        command.add("test");
        command.add("-Dsuite=" + request.getSuite());
        command.add("-Dbrowser=" + request.getBrowser());
        command.add("-Dheadless=" + request.isHeadless());

        if (request.getTestClass() != null && !request.getTestClass().isEmpty()) {
            command.add("-Dtest=" + request.getTestClass());
        }

        return command;
    }

    /**
     * Get execution status
     */
    public Map<String, Object> getExecutionStatus(String executionId) {
        logger.debug("Fetching status for execution: {}", executionId);

        ExecutionStatus status = activeExecutions.get(executionId);
        Map<String, Object> result = new HashMap<>();

        if (status != null) {
            result.put("executionId", status.getExecutionId());
            result.put("status", status.getStatus());
            result.put("progress", status.getProgress());
            result.put("currentTest", status.getCurrentTest());
            result.put("totalTests", status.getTotalTests());
            result.put("completedTests", status.getCompletedTests());
            result.put("passedTests", status.getPassedTests());
            result.put("failedTests", status.getFailedTests());
            result.put("message", status.getMessage());
            result.put("suite", status.getSuite());
        } else {
            result.put("executionId", executionId);
            result.put("status", "NOT_FOUND");
            result.put("progress", 0);
            result.put("message", "Execution not found or already completed");
        }

        return result;
    }

    /**
     * Stop an active execution
     */
    public boolean stopExecution(String executionId) {
        logger.info("Stopping execution: {}", executionId);

        Process process = executionProcesses.get(executionId);
        if (process != null && process.isAlive()) {
            process.destroy();

            // Force kill if necessary
            if (process.isAlive()) {
                try {
                    Thread.sleep(2000);
                    if (process.isAlive()) {
                        process.destroyForcibly();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            ExecutionStatus status = activeExecutions.get(executionId);
            if (status != null) {
                status.markStopped();
            }

            updateExecutionLog(executionId, "STOPPED", "Stopped by user");
            logger.info("Execution {} stopped successfully", executionId);
            return true;
        }

        logger.warn("Execution {} not found or already completed", executionId);
        return false;
    }

    /**
     * Get available test classes
     */
    public List<Map<String, Object>> getAvailableTestClasses(String type) {
        logger.info("Fetching available test classes for type: {}", type);

        List<Map<String, Object>> testClasses = new ArrayList<>();
        Path testDir = Paths.get("src/test/java/org/automation/" + type);

        try {
            if (Files.exists(testDir)) {
                Files.walk(testDir)
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> {
                        Map<String, Object> testClass = new HashMap<>();
                        String fileName = path.getFileName().toString();
                        String className = fileName.replace(".java", "");

                        testClass.put("name", className);
                        testClass.put("type", type);
                        testClass.put("path", path.toString());
                        testClass.put("fullName", "org.automation." + type + "." + className);

                        testClasses.add(testClass);
                    });
            }
        } catch (IOException e) {
            logger.error("Error reading test directory for type {}: {}", type, e.getMessage());
        }

        logger.info("Found {} test classes for type {}", testClasses.size(), type);
        return testClasses;
    }

    /**
     * Update execution log in database
     */
    private void updateExecutionLog(String executionId, String status, String errorMessage) {
        if (executionLogRepository != null) {
            try {
                executionLogRepository.findBySuiteId(executionId).forEach(log -> {
                    log.setStatus(status);
                    log.setEndTime(LocalDateTime.now());
                    if (log.getStartTime() != null) {
                        long duration = java.time.Duration.between(log.getStartTime(), log.getEndTime()).toMillis();
                        log.setDurationMs(duration);
                    }
                    if (errorMessage != null) {
                        log.setErrorMessage(errorMessage);
                    }
                    executionLogRepository.save(log);
                });
            } catch (Exception e) {
                logger.error("Error updating execution log for {}: {}", executionId, e.getMessage());
            }
        }
    }

    /**
     * Schedule cleanup of execution status after 5 minutes
     */
    private void scheduleStatusCleanup(String executionId) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                activeExecutions.remove(executionId);
                logger.debug("Cleaned up execution status for: {}", executionId);
            }
        }, 5 * 60 * 1000); // 5 minutes
    }
}

