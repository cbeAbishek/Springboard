package org.automation.reports;

import org.automation.reports.model.TestReport;
import org.automation.reports.model.TestReportDetail;
import org.automation.reports.repository.TestReportRepository;
import org.automation.reports.repository.TestReportDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Central Report Manager for handling all test reporting with unique IDs
 * Works seamlessly from both UI and CMD execution
 */
@Component
public class ReportManager {

    private static final Logger logger = LoggerFactory.getLogger(ReportManager.class);
    private static final DateTimeFormatter ID_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String REPORT_BASE_DIR = "artifacts/reports";

    private static ThreadLocal<TestReport> currentReport = new ThreadLocal<>();
    private static ThreadLocal<String> executionMode = new ThreadLocal<>(); // "UI" or "CMD"

    @Autowired(required = false)
    private TestReportRepository reportRepository;

    @Autowired(required = false)
    private TestReportDetailRepository detailRepository;

    /**
     * Generate unique report ID based on execution time
     * Format: RPT_YYYYMMDD_HHMMSS_UUID
     */
    public static String generateReportId() {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(ID_FORMATTER);
        String shortUuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("RPT_%s_%s", timestamp, shortUuid);
    }

    /**
     * Initialize a new report for test execution
     * @param suiteType Type of test suite (UI, API, ALL)
     * @param mode Execution mode (UI or CMD)
     */
    public TestReport initializeReport(String suiteType, String mode) {
        try {
            String reportId = generateReportId();
            LocalDateTime executionDate = LocalDateTime.now();

            // Set execution mode
            executionMode.set(mode != null ? mode : detectExecutionMode());

            // Create report entity
            TestReport report = new TestReport();
            report.setReportId(reportId);
            report.setExecutionDate(executionDate);
            report.setSuiteType(suiteType);
            report.setStatus("RUNNING");
            report.setCreatedBy(executionMode.get());
            report.setTriggerType(detectTriggerType());

            // Create report directory
            String reportPath = createReportDirectory(reportId);
            report.setReportPath(reportPath);

            // Save to database if available
            if (reportRepository != null) {
                report = reportRepository.save(report);
                logger.info("Report initialized in database: {}", reportId);
            }

            // Store in thread local
            currentReport.set(report);

            logger.info("Report initialized: {} | Mode: {} | Path: {}", reportId, executionMode.get(), reportPath);
            return report;

        } catch (Exception e) {
            logger.error("Error initializing report", e);
            return createFallbackReport();
        }
    }

    /**
     * Create report directory structure
     */
    private String createReportDirectory(String reportId) {
        try {
            Path reportDir = Paths.get(REPORT_BASE_DIR, reportId);
            Files.createDirectories(reportDir);

            // Create subdirectories
            Files.createDirectories(reportDir.resolve("screenshots"));
            Files.createDirectories(reportDir.resolve("api-artifacts"));
            Files.createDirectories(reportDir.resolve("logs"));

            logger.info("Created report directory structure: {}", reportDir.toAbsolutePath());
            return reportDir.toString();

        } catch (IOException e) {
            logger.error("Error creating report directory", e);
            return REPORT_BASE_DIR + "/" + reportId;
        }
    }

    /**
     * Add test detail to current report
     */
    public void addTestDetail(TestReportDetail detail) {
        TestReport report = currentReport.get();
        if (report == null) {
            logger.warn("No active report found. Initializing fallback report.");
            report = initializeReport("UNKNOWN", detectExecutionMode());
        }

        try {
            detail.setReport(report);

            // Save to database if available
            if (detailRepository != null) {
                detailRepository.save(detail);
            }

            // Update report statistics
            updateReportStatistics(report, detail);

            logger.debug("Test detail added: {} - {}", detail.getTestName(), detail.getStatus());

        } catch (Exception e) {
            logger.error("Error adding test detail", e);
        }
    }

    /**
     * Update report statistics based on test results
     */
    private void updateReportStatistics(TestReport report, TestReportDetail detail) {
        report.setTotalTests(report.getTotalTests() + 1);

        switch (detail.getStatus().toUpperCase()) {
            case "PASS":
            case "PASSED":
                report.setPassedTests(report.getPassedTests() + 1);
                break;
            case "FAIL":
            case "FAILED":
                report.setFailedTests(report.getFailedTests() + 1);
                break;
            case "SKIP":
            case "SKIPPED":
                report.setSkippedTests(report.getSkippedTests() + 1);
                break;
        }

        // Calculate success rate
        if (report.getTotalTests() > 0) {
            double successRate = (report.getPassedTests() * 100.0) / report.getTotalTests();
            report.setSuccessRate(successRate);
        }

        // Update in database
        if (reportRepository != null) {
            reportRepository.save(report);
        }
    }

    /**
     * Finalize and save the report
     */
    public void finalizeReport() {
        TestReport report = currentReport.get();
        if (report == null) {
            logger.warn("No active report to finalize");
            return;
        }

        try {
            report.setStatus("COMPLETED");

            // Calculate total duration
            if (report.getExecutionDate() != null) {
                long durationMs = java.time.Duration.between(
                    report.getExecutionDate(),
                    LocalDateTime.now()
                ).toMillis();
                report.setDurationMs(durationMs);
            }

            // Save final state to database
            if (reportRepository != null) {
                reportRepository.save(report);
            }

            // Generate HTML summary
            generateHtmlSummary(report);

            logger.info("Report finalized: {} | Total: {} | Passed: {} | Failed: {} | Success Rate: {:.2f}%",
                report.getReportId(), report.getTotalTests(), report.getPassedTests(),
                report.getFailedTests(), report.getSuccessRate());

        } catch (Exception e) {
            logger.error("Error finalizing report", e);
            if (report != null) {
                report.setStatus("FAILED");
                if (reportRepository != null) {
                    reportRepository.save(report);
                }
            }
        } finally {
            currentReport.remove();
            executionMode.remove();
        }
    }

    /**
     * Generate HTML summary report
     */
    private void generateHtmlSummary(TestReport report) {
        try {
            String htmlPath = report.getReportPath() + "/summary.html";
            File htmlFile = new File(htmlPath);

            try (FileWriter writer = new FileWriter(htmlFile)) {
                writer.write(generateHtmlContent(report));
            }

            logger.info("HTML summary generated: {}", htmlPath);

        } catch (IOException e) {
            logger.error("Error generating HTML summary", e);
        }
    }

    /**
     * Generate HTML content for report summary
     */
    private String generateHtmlContent(TestReport report) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html>\n<head>\n");
        html.append("<title>Test Report - ").append(report.getReportId()).append("</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }\n");
        html.append(".header { background-color: #2c3e50; color: white; padding: 20px; border-radius: 5px; }\n");
        html.append(".summary { background-color: white; padding: 20px; margin-top: 20px; border-radius: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n");
        html.append(".stat { display: inline-block; margin: 10px 20px; }\n");
        html.append(".stat-label { font-weight: bold; color: #555; }\n");
        html.append(".stat-value { font-size: 24px; color: #2c3e50; }\n");
        html.append(".pass { color: #27ae60; }\n");
        html.append(".fail { color: #e74c3c; }\n");
        html.append(".skip { color: #f39c12; }\n");
        html.append("</style>\n</head>\n<body>\n");

        html.append("<div class='header'>\n");
        html.append("<h1>Test Execution Report</h1>\n");
        html.append("<p><strong>Report ID:</strong> ").append(report.getReportId()).append("</p>\n");
        html.append("<p><strong>Execution Date:</strong> ").append(report.getExecutionDate()).append("</p>\n");
        html.append("<p><strong>Suite Type:</strong> ").append(report.getSuiteType()).append("</p>\n");
        html.append("<p><strong>Executed From:</strong> ").append(report.getCreatedBy()).append("</p>\n");
        html.append("</div>\n");

        html.append("<div class='summary'>\n");
        html.append("<h2>Summary</h2>\n");
        html.append("<div class='stat'>\n");
        html.append("<div class='stat-label'>Total Tests</div>\n");
        html.append("<div class='stat-value'>").append(report.getTotalTests()).append("</div>\n");
        html.append("</div>\n");

        html.append("<div class='stat'>\n");
        html.append("<div class='stat-label'>Passed</div>\n");
        html.append("<div class='stat-value pass'>").append(report.getPassedTests()).append("</div>\n");
        html.append("</div>\n");

        html.append("<div class='stat'>\n");
        html.append("<div class='stat-label'>Failed</div>\n");
        html.append("<div class='stat-value fail'>").append(report.getFailedTests()).append("</div>\n");
        html.append("</div>\n");

        html.append("<div class='stat'>\n");
        html.append("<div class='stat-label'>Skipped</div>\n");
        html.append("<div class='stat-value skip'>").append(report.getSkippedTests()).append("</div>\n");
        html.append("</div>\n");

        html.append("<div class='stat'>\n");
        html.append("<div class='stat-label'>Success Rate</div>\n");
        html.append("<div class='stat-value'>").append(String.format("%.2f%%", report.getSuccessRate())).append("</div>\n");
        html.append("</div>\n");

        html.append("<div class='stat'>\n");
        html.append("<div class='stat-label'>Duration</div>\n");
        html.append("<div class='stat-value'>").append(formatDuration(report.getDurationMs())).append("</div>\n");
        html.append("</div>\n");

        html.append("</div>\n");
        html.append("</body>\n</html>");

        return html.toString();
    }

    /**
     * Format duration in human-readable format
     */
    private String formatDuration(long durationMs) {
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%dm %ds", minutes, seconds);
    }

    /**
     * Get current active report
     */
    public TestReport getCurrentReport() {
        return currentReport.get();
    }

    /**
     * Get screenshot path for current report
     */
    public String getScreenshotPath(String screenshotName) {
        TestReport report = currentReport.get();
        if (report != null && report.getReportPath() != null) {
            return report.getReportPath() + "/screenshots/" + screenshotName;
        }
        return "screenshots/" + screenshotName;
    }

    /**
     * Get API artifact path for current report
     */
    public String getApiArtifactPath(String artifactName) {
        TestReport report = currentReport.get();
        if (report != null && report.getReportPath() != null) {
            return report.getReportPath() + "/api-artifacts/" + artifactName;
        }
        return "api-artifacts/" + artifactName;
    }

    /**
     * Detect execution mode (UI or CMD)
     */
    private String detectExecutionMode() {
        // Check if running from Maven/command line
        String maven = System.getProperty("maven.home");
        if (maven != null) {
            return "CMD";
        }

        // Check for UI indicators
        if (System.getProperty("spring.application.name") != null) {
            return "UI";
        }

        return "CMD"; // Default to CMD
    }

    /**
     * Detect trigger type (MANUAL, SCHEDULED, CI/CD)
     */
    private String detectTriggerType() {
        if (System.getenv("CI") != null || System.getenv("JENKINS_HOME") != null) {
            return "CI/CD";
        }
        return "MANUAL";
    }

    /**
     * Create fallback report when database is not available
     */
    private TestReport createFallbackReport() {
        TestReport report = new TestReport();
        report.setReportId(generateReportId());
        report.setExecutionDate(LocalDateTime.now());
        report.setStatus("RUNNING");
        report.setCreatedBy(detectExecutionMode());
        currentReport.set(report);
        return report;
    }

    /**
     * Get singleton instance (for non-Spring contexts)
     */
    private static ReportManager instance;

    public static ReportManager getInstance() {
        if (instance == null) {
            instance = new ReportManager();
        }
        return instance;
    }

    /**
     * Get current report ID (static method for listeners)
     */
    public static String getCurrentReportId() {
        TestReport report = currentReport.get();
        return report != null ? report.getReportId() : null;
    }

    /**
     * Set current report ID (static method for fallback)
     */
    public static void setCurrentReportId(String reportId) {
        TestReport report = currentReport.get();
        if (report == null) {
            report = new TestReport();
            currentReport.set(report);
        }
        report.setReportId(reportId);
    }

    /**
     * Initialize report with full parameters
     */
    public TestReport initializeReport(String suiteType, String browser, String createdBy, String triggerType) {
        try {
            String reportId = generateReportId();
            LocalDateTime executionDate = LocalDateTime.now();

            // Set execution mode
            executionMode.set(createdBy != null ? createdBy : detectExecutionMode());

            // Create report entity
            TestReport report = new TestReport();
            report.setReportId(reportId);
            report.setExecutionDate(executionDate);
            report.setSuiteType(suiteType);
            report.setBrowser(browser);
            report.setStatus("RUNNING");
            report.setCreatedBy(createdBy != null ? createdBy : executionMode.get());
            report.setTriggerType(triggerType != null ? triggerType : detectTriggerType());

            // Create report directory
            String reportPath = createReportDirectory(reportId);
            report.setReportPath(reportPath);

            // Save to database if available
            if (reportRepository != null) {
                report = reportRepository.save(report);
                logger.info("Report initialized in database: {}", reportId);
            }

            // Store in thread local
            currentReport.set(report);

            logger.info("Report initialized: {} | Mode: {} | Path: {}", reportId, executionMode.get(), reportPath);
            return report;

        } catch (Exception e) {
            logger.error("Error initializing report", e);
            return createFallbackReport();
        }
    }

    /**
     * Generate aggregated report
     */
    public void generateAggregatedReport() {
        try {
            logger.info("Generating aggregated report...");
            // This is handled by ReportService
        } catch (Exception e) {
            logger.error("Error generating aggregated report", e);
        }
    }
}
