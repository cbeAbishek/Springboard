package org.example.mavendemo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "test_cases")
@Schema(description = "Test Case entity representing a test scenario")
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier of the test case", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Column(nullable = false)
    @NotBlank(message = "Test case name is required")
    @Size(max = 255, message = "Test case name must not exceed 255 characters")
    @Schema(description = "Name of the test case", example = "Login Test", maxLength = 255)
    private String name;

    @Size(max = 100, message = "Test case type must not exceed 100 characters")
    @Schema(description = "Type of the test case", example = "Functional", allowableValues = { "Functional",
            "Integration", "Unit", "Performance", "Security" })
    private String type;

    @Column(columnDefinition = "TEXT")
    @Schema(description = "Detailed description of what the test case does", example = "This test verifies that users can successfully log in with valid credentials")
    private String description;

    @Size(max = 50, message = "Status must not exceed 50 characters")
    @Schema(description = "Current status of the test case", example = "Active", allowableValues = { "Active",
            "Inactive", "Draft", "Deprecated" })
    private String status;

    @Column(length = 50)
    @Schema(description = "Priority level", example = "HIGH", allowableValues = { "HIGH", "MEDIUM", "LOW" })
    private String priority;

    @Column(length = 100)
    @Schema(description = "Test environment", example = "STAGING")
    private String environment;

    @Column(length = 255)
    @Schema(description = "URL or path for the test", example = "https://google.com")
    private String url;

    @Column(columnDefinition = "TEXT")
    @Schema(description = "Test steps in JSON format")
    private String testSteps;

    @Column(columnDefinition = "TEXT")
    @Schema(description = "Expected results")
    private String expectedResults;

    @Column(length = 100)
    @Schema(description = "Test author/creator", example = "John Doe")
    private String author;

    @Column(length = 100)
    @Schema(description = "Test category", example = "Smoke Test")
    private String category;

    @Column(length = 50)
    @Schema(description = "Estimated execution time in minutes", example = "5")
    private Integer estimatedDuration;

    @Column(columnDefinition = "TEXT")
    @Schema(description = "Test data in JSON format")
    private String testData;

    @Column(columnDefinition = "TEXT")
    @Schema(description = "Prerequisites for test execution")
    private String prerequisites;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @Schema(description = "Timestamp when the test case was created", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @Schema(description = "Timestamp when the test case was last updated", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;

    public TestCase() {
    }

    public TestCase(String name, String type, String description, String status) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.status = status;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTestSteps() {
        return testSteps;
    }

    public void setTestSteps(String testSteps) {
        this.testSteps = testSteps;
    }

    public String getExpectedResults() {
        return expectedResults;
    }

    public void setExpectedResults(String expectedResults) {
        this.expectedResults = expectedResults;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getEstimatedDuration() {
        return estimatedDuration;
    }

    public void setEstimatedDuration(Integer estimatedDuration) {
        this.estimatedDuration = estimatedDuration;
    }

    public String getTestData() {
        return testData;
    }

    public void setTestData(String testData) {
        this.testData = testData;
    }

    public String getPrerequisites() {
        return prerequisites;
    }

    public void setPrerequisites(String prerequisites) {
        this.prerequisites = prerequisites;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
