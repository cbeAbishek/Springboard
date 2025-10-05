package org.automation.dashboard.model;

import java.time.LocalDateTime;

/**
 * Model class representing test execution status
 */
public class ExecutionStatus {

    private String executionId;
    private String status; // RUNNING, COMPLETED, FAILED, STOPPED
    private int progress; // 0-100
    private String currentTest;
    private int totalTests;
    private int completedTests;
    private int passedTests;
    private int failedTests;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String message;
    private String suite;

    // Constructors
    public ExecutionStatus() {}

    public ExecutionStatus(String executionId) {
        this.executionId = executionId;
        this.status = "RUNNING";
        this.progress = 0;
        this.startTime = LocalDateTime.now();
    }

    // Getters
    public String getExecutionId() { return executionId; }
    public String getStatus() { return status; }
    public int getProgress() { return progress; }
    public String getCurrentTest() { return currentTest; }
    public int getTotalTests() { return totalTests; }
    public int getCompletedTests() { return completedTests; }
    public int getPassedTests() { return passedTests; }
    public int getFailedTests() { return failedTests; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public String getMessage() { return message; }
    public String getSuite() { return suite; }

    // Setters
    public void setExecutionId(String executionId) { this.executionId = executionId; }
    public void setStatus(String status) { this.status = status; }
    public void setProgress(int progress) { this.progress = Math.min(100, Math.max(0, progress)); }
    public void setCurrentTest(String currentTest) { this.currentTest = currentTest; }
    public void setTotalTests(int totalTests) { this.totalTests = totalTests; }
    public void setCompletedTests(int completedTests) { this.completedTests = completedTests; }
    public void setPassedTests(int passedTests) { this.passedTests = passedTests; }
    public void setFailedTests(int failedTests) { this.failedTests = failedTests; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public void setMessage(String message) { this.message = message; }
    public void setSuite(String suite) { this.suite = suite; }

    /**
     * Update progress based on completed vs total tests
     */
    public void updateProgress() {
        if (totalTests > 0) {
            this.progress = (int) ((completedTests * 100.0) / totalTests);
        }
    }

    /**
     * Mark execution as completed
     */
    public void markCompleted() {
        this.status = "COMPLETED";
        this.progress = 100;
        this.endTime = LocalDateTime.now();
    }

    /**
     * Mark execution as failed
     */
    public void markFailed(String errorMessage) {
        this.status = "FAILED";
        this.endTime = LocalDateTime.now();
        this.message = errorMessage;
    }

    /**
     * Mark execution as stopped
     */
    public void markStopped() {
        this.status = "STOPPED";
        this.endTime = LocalDateTime.now();
        this.message = "Execution stopped by user";
    }
}
