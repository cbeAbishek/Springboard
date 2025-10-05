package org.automation.analytics.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing individual test details within a report
 */
@Entity
@Table(name = "test_report_details")
public class TestReportDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private TestReport report;

    @Column(name = "test_name")
    private String testName;

    @Column(name = "test_class")
    private String testClass;

    @Column(name = "test_method")
    private String testMethod;

    @Column(name = "status")
    private String status; // PASS, FAIL, SKIP

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "duration_ms")
    private long durationMs;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "stack_trace", length = 5000)
    private String stackTrace;

    @Column(name = "screenshot_path")
    private String screenshotPath;

    @Column(name = "screenshot_name")
    private String screenshotName;

    @Column(name = "test_type")
    private String testType; // UI or API

    @Column(name = "browser")
    private String browser;

    @Column(name = "api_endpoint")
    private String apiEndpoint;

    @Column(name = "api_method")
    private String apiMethod;

    @Column(name = "api_response_code")
    private Integer apiResponseCode;

    @Column(name = "api_artifact_path")
    private String apiArtifactPath;

    // Constructors
    public TestReportDetail() {}

    public TestReportDetail(String testName, String testClass, String status) {
        this.testName = testName;
        this.testClass = testClass;
        this.status = status;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TestReport getReport() { return report; }
    public void setReport(TestReport report) { this.report = report; }

    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }

    public String getTestClass() { return testClass; }
    public void setTestClass(String testClass) { this.testClass = testClass; }

    public String getTestMethod() { return testMethod; }
    public void setTestMethod(String testMethod) { this.testMethod = testMethod; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getStackTrace() { return stackTrace; }
    public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }

    public String getScreenshotPath() { return screenshotPath; }
    public void setScreenshotPath(String screenshotPath) { this.screenshotPath = screenshotPath; }

    public String getScreenshotName() { return screenshotName; }
    public void setScreenshotName(String screenshotName) { this.screenshotName = screenshotName; }

    public String getTestType() { return testType; }
    public void setTestType(String testType) { this.testType = testType; }

    public String getBrowser() { return browser; }
    public void setBrowser(String browser) { this.browser = browser; }

    public String getApiEndpoint() { return apiEndpoint; }
    public void setApiEndpoint(String apiEndpoint) { this.apiEndpoint = apiEndpoint; }

    public String getApiMethod() { return apiMethod; }
    public void setApiMethod(String apiMethod) { this.apiMethod = apiMethod; }

    public Integer getApiResponseCode() { return apiResponseCode; }
    public void setApiResponseCode(Integer apiResponseCode) { this.apiResponseCode = apiResponseCode; }

    public String getApiArtifactPath() { return apiArtifactPath; }
    public void setApiArtifactPath(String apiArtifactPath) { this.apiArtifactPath = apiArtifactPath; }
}

