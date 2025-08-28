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
    @Schema(description = "Execution status of the test", example = "PASSED", allowableValues = {"PASSED", "FAILED", "SKIPPED", "BLOCKED"})
    private String status;

    @Schema(description = "Timestamp when the test was executed", example = "2023-08-28T10:30:00")
    private LocalDateTime executedAt;

    @Column(columnDefinition = "TEXT")
    @Schema(description = "Additional notes or error messages from test execution", example = "Test completed successfully without any issues")
    private String notes;

    @Schema(description = "Duration of test execution in milliseconds", example = "1500")
    private Long duration;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @Schema(description = "Timestamp when the test result was recorded", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    // Add foreign key relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id", insertable = false, updatable = false)
    @Schema(hidden = true)
    private TestCase testCase;

    public TestResult() {}

    public TestResult(Long testCaseId, String status, LocalDateTime executedAt) {
        this.testCaseId = testCaseId;
        this.status = status;
        this.executedAt = executedAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTestCaseId() { return testCaseId; }
    public void setTestCaseId(Long testCaseId) { this.testCaseId = testCaseId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getExecutedAt() { return executedAt; }
    public void setExecutedAt(LocalDateTime executedAt) { this.executedAt = executedAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Long getDuration() { return duration; }
    public void setDuration(Long duration) { this.duration = duration; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public TestCase getTestCase() { return testCase; }
    public void setTestCase(TestCase testCase) { this.testCase = testCase; }
}
