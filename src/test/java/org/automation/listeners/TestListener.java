package org.automation.listeners;

import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.ITestContext;
import org.automation.utils.ScreenshotUtils;
import org.automation.utils.DatabaseInserter;
import org.automation.reports.ReportManager;
import org.automation.reports.model.TestReportDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Main TestNG listener for handling test execution events
 * Integrated with ReportManager for comprehensive reporting
 */
public class TestListener implements ITestListener {
    
    private static final Logger logger = LoggerFactory.getLogger(TestListener.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static ReportManager reportManager;

    static {
        try {
            reportManager = ReportManager.getInstance();
        } catch (Exception e) {
            logger.warn("Could not initialize ReportManager, using fallback", e);
        }
    }

    private ThreadLocal<LocalDateTime> testStartTime = new ThreadLocal<>();

    @Override
    public void onTestStart(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        String className = result.getTestClass().getName();
        testStartTime.set(LocalDateTime.now());
        logger.info("Starting test: {}.{}", className, testName);
    }
    
    @Override
    public void onTestSuccess(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        String className = result.getTestClass().getName();
        long duration = result.getEndMillis() - result.getStartMillis();
        
        logger.info("Test PASSED: {}.{} (Duration: {}ms)", className, testName, duration);
        
        // Create test detail for report
        if (reportManager != null) {
            try {
                TestReportDetail detail = createTestDetail(result, "PASS", duration, null, null);
                reportManager.addTestDetail(detail);
            } catch (Exception e) {
                logger.warn("Failed to add test detail to report: {}", e.getMessage());
            }
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
            logger.warn("Failed to log test result to database: {}", e.getMessage());
        }

        testStartTime.remove();
    }
    
    @Override
    public void onTestFailure(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        String className = result.getTestClass().getName();
        long duration = result.getEndMillis() - result.getStartMillis();
        String errorMessage = result.getThrowable() != null ? result.getThrowable().getMessage() : "Unknown error";
        
        logger.error("Test FAILED: {}.{} (Duration: {}ms) - Error: {}", 
                    className, testName, duration, errorMessage);
        
        // Take screenshot for UI tests
        String screenshotPath = null;
        String screenshotName = null;
        if (isUiTest(className)) {
            screenshotName = testName + "_FAILED_" + System.currentTimeMillis() + ".png";
            screenshotPath = captureScreenshot(testName, screenshotName);
            if (screenshotPath != null) {
                logger.info("Screenshot captured: {}", screenshotPath);
            }
        }
        
        // Create test detail for report
        if (reportManager != null) {
            try {
                TestReportDetail detail = createTestDetail(result, "FAIL", duration, errorMessage, screenshotPath);
                if (screenshotName != null) {
                    detail.setScreenshotName(screenshotName);
                }
                if (result.getThrowable() != null) {
                    detail.setStackTrace(getStackTrace(result.getThrowable()));
                }
                reportManager.addTestDetail(detail);
            } catch (Exception e) {
                logger.warn("Failed to add test detail to report: {}", e.getMessage());
            }
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
            logger.warn("Failed to log test result to database: {}", e.getMessage());
        }

        testStartTime.remove();
    }
    
    @Override
    public void onTestSkipped(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        String className = result.getTestClass().getName();
        String skipReason = result.getThrowable() != null ? result.getThrowable().getMessage() : "Test skipped";
        
        logger.warn("Test SKIPPED: {}.{} - Reason: {}", className, testName, skipReason);
        
        // Create test detail for report
        if (reportManager != null) {
            try {
                TestReportDetail detail = createTestDetail(result, "SKIP", 0L, skipReason, null);
                reportManager.addTestDetail(detail);
            } catch (Exception e) {
                logger.warn("Failed to add test detail to report: {}", e.getMessage());
            }
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
            logger.warn("Failed to log test result to database: {}", e.getMessage());
        }

        testStartTime.remove();
    }
    
    @Override
    public void onStart(ITestContext context) {
        logger.info("Test context started: {}", context.getName());
    }
    
    @Override
    public void onFinish(ITestContext context) {
        logger.info("Test context finished: {}", context.getName());
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
                                     className.toLowerCase().contains("rest"));
    }

    /**
     * Capture screenshot for failed UI tests
     */
    private String captureScreenshot(String testName, String screenshotName) {
        try {
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
}
