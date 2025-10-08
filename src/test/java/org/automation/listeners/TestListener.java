package org.automation.listeners;

import io.qameta.allure.Allure;
import io.qameta.allure.Attachment;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.ITestContext;
import org.automation.utils.ScreenshotUtils;
import org.automation.utils.DatabaseInserter;
import org.automation.config.SpringContext;
import org.automation.reports.ReportManager;
import org.automation.reports.model.TestReport;
import org.automation.reports.model.TestReportDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.ByteArrayInputStream;

/**
 * Main TestNG listener for handling test execution events
 * Integrated with ReportManager and Allure for comprehensive reporting
 */
public class TestListener implements ITestListener {
    
    private static final Logger logger = LoggerFactory.getLogger(TestListener.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ThreadLocal<LocalDateTime> testStartTime = new ThreadLocal<>();

    /**
     * Get Spring-managed ReportManager instance
     */
    private ReportManager getReportManager() {
        try {
            ReportManager manager = SpringContext.getBean(ReportManager.class);
            if (manager != null) {
                return manager;
            }
            logger.warn("Could not get Spring-managed ReportManager, using fallback");
            return ReportManager.getInstance();
        } catch (Exception e) {
            logger.warn("Error getting ReportManager from Spring context: {}", e.getMessage());
            return ReportManager.getInstance();
        }
    }

    @Override
    public void onStart(ITestContext context) {
        logger.info("==================== Test Suite Starting: {} ====================", context.getName());

        // Initialize report at suite start
        ReportManager reportManager = getReportManager();
        if (reportManager != null) {
            try {
                // Check if report ID is provided as system property (from UI execution)
                String providedReportId = System.getProperty("report.id");

                TestReport report;
                if (providedReportId != null && !providedReportId.isEmpty()) {
                    logger.info("Using provided report ID from UI: {}", providedReportId);

                    // Load existing report from database instead of creating new one
                    ReportManager.setCurrentReportId(providedReportId);

                    // Get the loaded report
                    report = reportManager.getCurrentReport();

                    if (report != null) {
                        logger.info("✓ Existing report loaded: {} | Status: {}", report.getReportId(), report.getStatus());

                        // Update environment if provided
                        String environment = System.getProperty("environment", "QA");
                        report.setEnvironment(environment);
                        logger.info("Test environment: {}", environment);
                    } else {
                        logger.warn("Could not load existing report, creating new one");
                        throw new RuntimeException("Report not found");
                    }
                } else {
                    logger.info("No report ID provided, initializing new report");

                    // Initialize new report with all parameters
                    String suiteType = determineSuiteType(context.getName());
                    String browser = System.getProperty("browser", "chrome");
                    String createdBy = System.getProperty("report.created.by", "CMD");
                    String triggerType = System.getProperty("report.trigger.type", "MANUAL");
                    String environment = System.getProperty("environment", "QA");

                    report = reportManager.initializeReport(suiteType, browser, createdBy, triggerType);
                    report.setEnvironment(environment);

                    logger.info("✓ New report initialized: {} | Suite: {} | Browser: {} | Env: {}",
                        report.getReportId(), suiteType, browser, environment);
                }

            } catch (Exception e) {
                logger.error("Failed to initialize report at suite start", e);
            }
        } else {
            logger.error("ReportManager is null! Test results will not be saved.");
        }
    }

    @Override
    public void onFinish(ITestContext context) {
        logger.info("==================== Test Suite Finished: {} ====================", context.getName());
        logger.info("Passed: {} | Failed: {} | Skipped: {}",
            context.getPassedTests().size(),
            context.getFailedTests().size(),
            context.getSkippedTests().size());

        // Finalize report at suite end
        ReportManager reportManager = getReportManager();
        if (reportManager != null) {
            try {
                reportManager.finalizeReport();
                String reportId = ReportManager.getCurrentReportId();
                logger.info("✓ Report finalized successfully: {}", reportId);
            } catch (Exception e) {
                logger.error("Failed to finalize report", e);
            }
        }
    }

    @Override
    public void onTestStart(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        String className = result.getTestClass().getName();
        testStartTime.set(LocalDateTime.now());
        logger.info("▶ Starting test: {}.{}", className, testName);

        // Add test description to Allure
        String description = result.getMethod().getDescription();
        if (description != null && !description.isEmpty()) {
            Allure.description(description);
        }
    }
    
    @Override
    public void onTestSuccess(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        String className = result.getTestClass().getName();
        long duration = result.getEndMillis() - result.getStartMillis();
        
        logger.info("✓ Test PASSED: {}.{} (Duration: {}ms)", className, testName, duration);

        // Create test detail for report
        ReportManager reportManager = getReportManager();
        if (reportManager != null) {
            try {
                TestReportDetail detail = createTestDetail(result, "PASSED", duration, null, null);
                reportManager.addTestDetail(detail);
                logger.info("  → Test detail saved to report: {}", testName);
            } catch (Exception e) {
                logger.error("Failed to add test detail to report: {}", e.getMessage(), e);
            }
        } else {
            logger.warn("ReportManager not available, test result not saved to database");
        }

        // Legacy database logging
        try {
            DatabaseInserter.insertTestResult(
                className,
                testName,
                "PASSED",
                LocalDateTime.now().format(formatter),
                duration,
                null,
                null
            );
        } catch (Exception e) {
            logger.debug("Legacy database insert not available: {}", e.getMessage());
        }

        testStartTime.remove();
    }
    
    @Override
    public void onTestFailure(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        String className = result.getTestClass().getName();
        long duration = result.getEndMillis() - result.getStartMillis();
        String errorMessage = result.getThrowable() != null ? result.getThrowable().getMessage() : "Unknown error";
        String fullStackTrace = getStackTrace(result.getThrowable());

        logger.error("✗ Test FAILED: {}.{} (Duration: {}ms)", className, testName, duration);
        logger.error("  Error: {}", errorMessage);

        // Attach error details to Allure
        attachTextToAllure("Error Message", errorMessage);
        if (fullStackTrace != null) {
            attachTextToAllure("Stack Trace", fullStackTrace);
        }

        // Take screenshot for UI tests
        String screenshotPath = null;
        String screenshotName = null;
        if (isUiTest(className)) {
            screenshotName = testName + "_FAILED_" + System.currentTimeMillis() + ".png";
            screenshotPath = captureScreenshot(testName, screenshotName);
            if (screenshotPath != null) {
                logger.info("  📸 Screenshot captured: {}", screenshotPath);

                // Attach screenshot to Allure report
                try {
                    attachScreenshotToAllure(screenshotPath, "Failure Screenshot");
                } catch (Exception e) {
                    logger.error("Failed to attach screenshot to Allure: {}", e.getMessage());
                }
            }
        }
        
        // Create test detail for report
        ReportManager reportManager = getReportManager();
        if (reportManager != null) {
            try {
                TestReportDetail detail = createTestDetail(result, "FAILED", duration, errorMessage, screenshotPath);
                if (screenshotName != null) {
                    detail.setScreenshotName(screenshotName);
                }
                detail.setStackTrace(fullStackTrace);
                reportManager.addTestDetail(detail);
                logger.info("  → Test detail with error saved to report: {}", testName);
            } catch (Exception e) {
                logger.error("Failed to add test detail to report: {}", e.getMessage(), e);
            }
        } else {
            logger.warn("ReportManager not available, test result not saved to database");
        }

        // Legacy database logging
        try {
            DatabaseInserter.insertTestResult(
                className,
                testName,
                "FAILED",
                LocalDateTime.now().format(formatter),
                duration,
                errorMessage,
                screenshotPath
            );
        } catch (Exception e) {
            logger.debug("Legacy database insert not available: {}", e.getMessage());
        }

        testStartTime.remove();
    }
    
    @Override
    public void onTestSkipped(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        String className = result.getTestClass().getName();
        String skipReason = result.getThrowable() != null ? result.getThrowable().getMessage() : "Test skipped";
        
        logger.warn("⊘ Test SKIPPED: {}.{} - Reason: {}", className, testName, skipReason);

        // Attach skip reason to Allure
        attachTextToAllure("Skip Reason", skipReason);

        // Create test detail for report
        ReportManager reportManager = getReportManager();
        if (reportManager != null) {
            try {
                TestReportDetail detail = createTestDetail(result, "SKIPPED", 0L, skipReason, null);
                reportManager.addTestDetail(detail);
                logger.info("  → Skipped test detail saved to report: {}", testName);
            } catch (Exception e) {
                logger.error("Failed to add test detail to report: {}", e.getMessage(), e);
            }
        } else {
            logger.warn("ReportManager not available, test result not saved to database");
        }

        // Legacy database logging
        try {
            DatabaseInserter.insertTestResult(
                className,
                testName,
                "SKIPPED",
                LocalDateTime.now().format(formatter),
                0L,
                skipReason,
                null
            );
        } catch (Exception e) {
            logger.debug("Legacy database insert not available: {}", e.getMessage());
        }

        testStartTime.remove();
    }

    /**
     * Determine suite type from context name
     */
    private String determineSuiteType(String contextName) {
        if (contextName == null) return "UNKNOWN";

        String lower = contextName.toLowerCase();
        if (lower.contains("api")) return "API";
        if (lower.contains("ui")) return "UI";
        if (lower.contains("smoke")) return "Smoke";
        if (lower.contains("regression")) return "Regression";
        if (lower.contains("integration")) return "Integration";

        return "ALL";
    }

    /**
     * Create TestReportDetail from ITestResult
     */
    private TestReportDetail createTestDetail(ITestResult result, String status, long duration,
                                              String errorMessage, String screenshotPath) {
        TestReportDetail detail = new TestReportDetail();
        detail.setTestName(result.getMethod().getMethodName());
        detail.setTestClass(result.getTestClass().getName());
        detail.setTestMethod(result.getMethod().getMethodName());
        detail.setStatus(status);
        detail.setStartTime(testStartTime.get());
        detail.setEndTime(LocalDateTime.now());
        detail.setDurationMs(duration);
        detail.setErrorMessage(errorMessage);
        detail.setScreenshotPath(screenshotPath);

        // Determine test type
        String className = result.getTestClass().getName();
        if (isUiTest(className)) {
            detail.setTestType("UI");
            detail.setBrowser(System.getProperty("browser", "chrome"));
        } else if (isApiTest(className)) {
            detail.setTestType("API");
        } else {
            detail.setTestType("UNKNOWN");
        }

        return detail;
    }

    /**
     * Check if test is a UI test
     */
    private boolean isUiTest(String className) {
        return className != null && (className.toLowerCase().contains("ui") ||
                                     className.toLowerCase().contains("selenium") ||
                                     className.toLowerCase().contains("web"));
    }

    /**
     * Check if test is an API test
     */
    private boolean isApiTest(String className) {
        return className != null && (className.toLowerCase().contains("api") ||
                                     className.toLowerCase().contains("rest") ||
                                     className.toLowerCase().contains("service"));
    }

    /**
     * Capture screenshot for failed UI tests
     */
    private String captureScreenshot(String testName, String screenshotName) {
        try {
            ReportManager reportManager = getReportManager();
            String screenshotPath = null;
            if (reportManager != null && reportManager.getCurrentReport() != null) {
                screenshotPath = reportManager.getScreenshotPath(screenshotName);
            }

            String actualPath = ScreenshotUtils.captureToPath(testName, screenshotPath);
            return actualPath;
        } catch (Exception e) {
            logger.error("Error capturing screenshot: {}", e.getMessage());
            return ScreenshotUtils.takeScreenshot(testName + "_FAILED");
        }
    }

    /**
     * Get stack trace as string
     */
    private String getStackTrace(Throwable throwable) {
        if (throwable == null) return null;

        StringBuilder sb = new StringBuilder();
        sb.append(throwable.toString()).append("\n");

        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
            if (sb.length() > 4000) break; // Limit size
        }

        return sb.toString();
    }

    /**
     * Attach text content to Allure report
     */
    @Attachment(value = "{0}", type = "text/plain")
    private byte[] attachTextToAllure(String title, String text) {
        return text != null ? text.getBytes() : null;
    }

    /**
     * Attach screenshot to Allure report
     */
    private void attachScreenshotToAllure(String filePath, String title) throws IOException {
        if (filePath != null && Files.exists(Paths.get(filePath))) {
            byte[] fileContent = Files.readAllBytes(Paths.get(filePath));
            Allure.addAttachment(title, "image/png", new ByteArrayInputStream(fileContent), "png");
        }
    }
}
