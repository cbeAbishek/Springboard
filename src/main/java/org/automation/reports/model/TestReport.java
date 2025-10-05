package org.automation.reports.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a test execution report with unique ID
 * Supports both UI and CMD execution modes
 */
@Entity
@Table(name = "test_reports")
public class TestReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", unique = true, nullable = false, length = 100)
    private String reportId;

    @Column(name = "report_name", length = 255)
    private String reportName;

    @Column(name = "suite_type", length = 50)
    private String suiteType; // UI, API, ALL, SPECIFIC

    @Column(name = "execution_date")
    private LocalDateTime executionDate;

    @Column(name = "status", length = 20)
    private String status; // RUNNING, COMPLETED, FAILED

    @Column(name = "total_tests")
    private int totalTests;

    @Column(name = "passed_tests")
    private int passedTests;

    @Column(name = "failed_tests")
    private int failedTests;

    @Column(name = "skipped_tests")
    private int skippedTests;

    @Column(name = "success_rate")
    private double successRate;

    @Column(name = "duration_ms")
    private long durationMs;

    @Column(name = "browser", length = 50)
    private String browser;

    @Column(name = "environment", length = 50)
    private String environment;

    @Column(name = "created_by", length = 100)
    private String createdBy; // UI or CMD

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "report_path", length = 500)
    private String reportPath;

    @Column(name = "trigger_type", length = 50)
    private String triggerType;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TestReportDetail> testDetails = new ArrayList<>();

    // Constructors
    public TestReport() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public TestReport(String reportId, String reportName, String suiteType) {
        this();
        this.reportId = reportId;
        this.reportName = reportName;
        this.suiteType = suiteType;
        this.executionDate = LocalDateTime.now();
        this.status = "RUNNING";
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }

    public String getReportName() { return reportName; }
    public void setReportName(String reportName) { this.reportName = reportName; }

    public String getSuiteType() { return suiteType; }
    public void setSuiteType(String suiteType) { this.suiteType = suiteType; }

    public LocalDateTime getExecutionDate() { return executionDate; }
    public void setExecutionDate(LocalDateTime executionDate) { this.executionDate = executionDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getTotalTests() { return totalTests; }
    public void setTotalTests(int totalTests) { this.totalTests = totalTests; }

    public int getPassedTests() { return passedTests; }
    public void setPassedTests(int passedTests) { this.passedTests = passedTests; }

    public int getFailedTests() { return failedTests; }
    public void setFailedTests(int failedTests) { this.failedTests = failedTests; }

    public int getSkippedTests() { return skippedTests; }
    public void setSkippedTests(int skippedTests) { this.skippedTests = skippedTests; }

    public double getSuccessRate() { return successRate; }
    public void setSuccessRate(double successRate) { this.successRate = successRate; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    public String getBrowser() { return browser; }
    public void setBrowser(String browser) { this.browser = browser; }

    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getReportPath() { return reportPath; }
    public void setReportPath(String reportPath) { this.reportPath = reportPath; }

    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }

    public List<TestReportDetail> getTestDetails() { return testDetails; }
    public void setTestDetails(List<TestReportDetail> testDetails) { this.testDetails = testDetails; }

    // Helper methods
    public void addTestDetail(TestReportDetail detail) {
        testDetails.add(detail);
        detail.setReport(this);
    }

    public void removeTestDetail(TestReportDetail detail) {
        testDetails.remove(detail);
        detail.setReport(null);
    }

    public void calculateStatistics() {
        this.totalTests = testDetails.size();
        this.passedTests = (int) testDetails.stream().filter(d -> "PASS".equalsIgnoreCase(d.getStatus())).count();
        this.failedTests = (int) testDetails.stream().filter(d -> "FAIL".equalsIgnoreCase(d.getStatus())).count();
        this.skippedTests = (int) testDetails.stream().filter(d -> "SKIP".equalsIgnoreCase(d.getStatus())).count();
        this.successRate = totalTests > 0 ? (passedTests * 100.0 / totalTests) : 0.0;
        this.durationMs = testDetails.stream().mapToLong(TestReportDetail::getDurationMs).sum();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
