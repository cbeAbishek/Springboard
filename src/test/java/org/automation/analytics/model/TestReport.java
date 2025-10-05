package org.automation.analytics.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a test execution report with unique ID
 */
@Entity
@Table(name = "test_reports")
public class TestReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", unique = true, nullable = false)
    private String reportId; // Unique ID format: RPT_YYYYMMDD_HHMMSS_UUID

    @Column(name = "report_name")
    private String reportName;

    @Column(name = "execution_date")
    private LocalDateTime executionDate;

    @Column(name = "suite_type")
    private String suiteType; // UI, API, ALL, SPECIFIC

    @Column(name = "browser")
    private String browser;

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

    @Column(name = "report_path")
    private String reportPath; // Path to report directory

    @Column(name = "status")
    private String status; // RUNNING, COMPLETED, FAILED

    @Column(name = "created_by")
    private String createdBy; // UI or CMD

    @Column(name = "trigger_type")
    private String triggerType; // MANUAL, SCHEDULED, CI/CD

    @Column(name = "branch_name")
    private String branchName;

    @Column(name = "commit_hash")
    private String commitHash;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TestReportDetail> testDetails = new ArrayList<>();

    // Constructors
    public TestReport() {}

    public TestReport(String reportId, LocalDateTime executionDate, String suiteType) {
        this.reportId = reportId;
        this.executionDate = executionDate;
        this.suiteType = suiteType;
        this.status = "RUNNING";
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }

    public String getReportName() { return reportName; }
    public void setReportName(String reportName) { this.reportName = reportName; }

    public LocalDateTime getExecutionDate() { return executionDate; }
    public void setExecutionDate(LocalDateTime executionDate) { this.executionDate = executionDate; }

    public String getSuiteType() { return suiteType; }
    public void setSuiteType(String suiteType) { this.suiteType = suiteType; }

    public String getBrowser() { return browser; }
    public void setBrowser(String browser) { this.browser = browser; }

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

    public String getReportPath() { return reportPath; }
    public void setReportPath(String reportPath) { this.reportPath = reportPath; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }

    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }

    public String getCommitHash() { return commitHash; }
    public void setCommitHash(String commitHash) { this.commitHash = commitHash; }

    public List<TestReportDetail> getTestDetails() { return testDetails; }
    public void setTestDetails(List<TestReportDetail> testDetails) { this.testDetails = testDetails; }

    public void addTestDetail(TestReportDetail detail) {
        testDetails.add(detail);
        detail.setReport(this);
    }
}

