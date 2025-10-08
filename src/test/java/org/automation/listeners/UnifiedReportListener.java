package org.automation.listeners;

import org.automation.reports.model.TestReportDetail;
import org.automation.reports.ReportManager;
import org.automation.config.SpringContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Unified listener for test execution and report generation
 * Works for both UI-triggered and CMD-triggered test executions
 */
public class UnifiedReportListener implements ITestListener, ISuiteListener {

    private static final Logger logger = LoggerFactory.getLogger(UnifiedReportListener.class);
    private ReportManager reportManager;

    // Use static singleton instance when Spring context is not available
    private static ReportManager fallbackReportManager;

    /**
     * Get ReportManager bean from Spring context or fallback to singleton
     */
    private ReportManager getReportManager() {
        if (reportManager == null) {
            try {
                if (SpringContext.isContextAvailable()) {
                    reportManager = SpringContext.getBean(ReportManager.class);
                    logger.info("ReportManager bean retrieved from Spring context");
                } else {
                    // Use singleton fallback when Spring is not available
                    if (fallbackReportManager == null) {
                        fallbackReportManager = ReportManager.getInstance();
                        logger.info("Using fallback ReportManager instance (non-Spring context)");
                    }
                    reportManager = fallbackReportManager;
                }
            } catch (Exception e) {
                logger.warn("Failed to get ReportManager from Spring context: {}", e.getMessage());
                // Use singleton fallback
                if (fallbackReportManager == null) {
                    fallbackReportManager = ReportManager.getInstance();
                }
                reportManager = fallbackReportManager;
            }
        }
        return reportManager;
    }

    @Override
    public void onStart(ISuite suite) {
        logger.info("=== Test Suite Starting: {} ===", suite.getName());

        try {
            // Determine execution source
            String createdBy = System.getProperty("report.created.by", "CMD");
            String triggerType = System.getProperty("report.trigger.type", "MANUAL");
            String suiteType = determineSuiteType(suite.getName());
            String browser = System.getProperty("browser", "chrome");

            // Get ReportManager instance (works with or without Spring)
            ReportManager manager = getReportManager();

            // Initialize report
            if (manager != null) {
                manager.initializeReport(suiteType, browser, createdBy, triggerType);
                logger.info("✅ Report initialized with ID: {}", ReportManager.getCurrentReportId());
            } else {
                logger.error("❌ Failed to initialize ReportManager");
            }
        } catch (Exception e) {
            logger.error("Error initializing report: {}", e.getMessage(), e);
        }
    }

    @Override
    public void onFinish(ISuite suite) {
        logger.info("=== Test Suite Finished: {} ===", suite.getName());

        try {
            ReportManager manager = getReportManager();
            if (manager != null) {
                manager.finalizeReport();
                logger.info("✅ Report finalized successfully");
            }
        } catch (Exception e) {
            logger.error("Error finalizing report: {}", e.getMessage(), e);
        }
    }

    @Override
    public void onTestStart(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        String className = result.getTestClass().getName();
        logger.info("Starting test: {}.{}", className, testName);

        // Store start time in result attributes
        result.setAttribute("startTime", LocalDateTime.now());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        addTestDetailToReport(result, "PASS");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        addTestDetailToReport(result, "FAIL");
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        addTestDetailToReport(result, "SKIP");
    }

    private void addTestDetailToReport(ITestResult result, String status) {
        try {
            TestReportDetail detail = new TestReportDetail();

            // Basic test info
            detail.setTestName(result.getMethod().getMethodName());
            detail.setTestClass(result.getTestClass().getName());
            detail.setTestMethod(result.getMethod().getMethodName());
            detail.setStatus(status);

            // Timing
            LocalDateTime startTime = (LocalDateTime) result.getAttribute("startTime");
            if (startTime == null) {
                startTime = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(result.getStartMillis()),
                    ZoneId.systemDefault()
                );
            }
            detail.setStartTime(startTime);
            detail.setEndTime(LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(result.getEndMillis()),
                ZoneId.systemDefault()
            ));
            detail.setDurationMs(result.getEndMillis() - result.getStartMillis());

            // Test type detection
            String className = result.getTestClass().getName();
            if (className.contains(".ui.")) {
                detail.setTestType("UI");
                detail.setBrowser(System.getProperty("browser", "chrome"));
            } else if (className.contains(".api.")) {
                detail.setTestType("API");
            } else {
                detail.setTestType("OTHER");
            }

            // Error information
            if ("FAIL".equals(status) && result.getThrowable() != null) {
                detail.setErrorMessage(result.getThrowable().getMessage());
                detail.setStackTrace(getStackTrace(result.getThrowable()));
            }

            // Screenshot path (set by ScreenshotListener)
            String screenshotPath = (String) result.getAttribute("screenshotPath");
            if (screenshotPath != null) {
                detail.setScreenshotPath(screenshotPath);
                detail.setScreenshotName(extractFileName(screenshotPath));
            }

            // API artifact path (if applicable)
            String artifactPath = (String) result.getAttribute("apiArtifactPath");
            if (artifactPath != null) {
                detail.setApiArtifactPath(artifactPath);
            }

            // Add to report - now this should always work!
            ReportManager manager = getReportManager();
            if (manager != null) {
                manager.addTestDetail(detail);
                logger.info("✅ Test detail saved: {} - {} (Duration: {}ms)",
                    detail.getTestName(), status, detail.getDurationMs());
            } else {
                logger.error("❌ Cannot save test detail - ReportManager not available: {} - {}",
                    detail.getTestName(), status);
            }

        } catch (Exception e) {
            logger.error("Error adding test detail to report: {}", e.getMessage(), e);
        }
    }

    private String determineSuiteType(String suiteName) {
        String lowerName = suiteName.toLowerCase();
        if (lowerName.contains("ui")) return "UI";
        if (lowerName.contains("api")) return "API";
        if (lowerName.contains("all") || lowerName.contains("complete")) return "ALL";
        return "SPECIFIC";
    }

    private String getStackTrace(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.toString()).append("\n");
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
            if (sb.length() > 4000) break; // Limit stack trace size
        }
        return sb.toString();
    }

    private String extractFileName(String path) {
        if (path == null) return null;
        int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }
}
