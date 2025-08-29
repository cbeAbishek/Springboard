package org.example.mavendemo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "test_results")
@Schema(description = "Test Result entity representing the outcome of a test execution")
public class TestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier of the test result", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Column(name = "test_case_id", nullable = false)
    @NotNull(message = "Test case ID is required")
    @Schema(description = "ID of the associated test case", example = "1")
    private Long testCaseId;

    @Size(max = 50, message = "Status must not exceed 50 characters")
    @Schema(description = "Execution status of the test", example = "PASSED", allowableValues = { "PASSED", "FAILED",
            "SKIPPED", "BLOCKED" })
    private String status;

    @Schema(description = "Timestamp when the test was executed", example = "2023-08-28T10:30:00")
    private LocalDateTime executedAt;

    @Column(columnDefinition = "TEXT")
    @Schema(description = "Additional notes or error messages from test execution", example = "Test completed successfully without any issues")
    private String notes;

    @Schema(description = "Duration of test execution in milliseconds", example = "1500")
    private Long duration;

    @Column(length = 100)
    @Schema(description = "Test environment where test was executed", example = "STAGING")
    private String environment;

    @Column(length = 100)
    @Schema(description = "Test executor/runner", example = "John Doe")
    private String executor;

    @Column(length = 255)
    @Schema(description = "Browser/Platform used for execution", example = "Chrome 118.0")
    private String platform;

    @Column(columnDefinition = "TEXT")
    @Schema(description = "Detailed error message or stack trace")
    private String errorMessage;

    @Column(columnDefinition = "TEXT")
    @Schema(description = "Screenshots or logs in JSON format")
    private String attachments;

    @Column(length = 50)
    @Schema(description = "Test verdict", example = "PASS", allowableValues = { "PASS", "FAIL", "BLOCKED", "NOT_RUN" })
    private String verdict;

    @Schema(description = "Number of retry attempts", example = "0")
    private Integer retryCount;

    @Column(columnDefinition = "TEXT")
    @Schema(description = "Execution summary and metrics")
    private String executionSummary;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @Schema(description = "Timestamp when the test result was recorded", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    // Add foreign key relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id", insertable = false, updatable = false)
    @Schema(hidden = true)
    private TestCase testCase;

    public TestResult() {
    }

    public TestResult(Long testCaseId, String status, LocalDateTime executedAt) {
        this.testCaseId = testCaseId;
        this.status = status;
        this.executedAt = executedAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTestCaseId() {
        return testCaseId;
    }

    public void setTestCaseId(Long testCaseId) {
        this.testCaseId = testCaseId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getExecutor() {
        return executor;
    }

    public void setExecutor(String executor) {
        this.executor = executor;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getAttachments() {
        return attachments;
    }

    public void setAttachments(String attachments) {
        this.attachments = attachments;
    }

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public String getExecutionSummary() {
        return executionSummary;
    }

    public void setExecutionSummary(String executionSummary) {
        this.executionSummary = executionSummary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public TestCase getTestCase() {
        return testCase;
    }

    public void setTestCase(TestCase testCase) {
        this.testCase = testCase;
    }
}
