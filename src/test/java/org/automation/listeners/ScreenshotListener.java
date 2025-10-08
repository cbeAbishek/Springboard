package org.automation.listeners;

import io.qameta.allure.Allure;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.automation.utils.ScreenshotUtils;
import org.automation.analytics.model.ExecutionLog;
import org.automation.analytics.repo.ExecutionLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;

/**
 * TestNG listener specifically for handling screenshot capture on test failures
 * Enhanced with comprehensive logging, database integration, and Allure reporting
 */
@Component
public class ScreenshotListener implements ITestListener {
    
    private static final Logger logger = LoggerFactory.getLogger(ScreenshotListener.class);
    
    @Autowired(required = false)
    private ExecutionLogRepository executionLogRepository;

    @Override
    public void onTestFailure(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        String className = result.getTestClass().getName();
        
        logger.info("ScreenshotListener: Test FAILED - {} in class {}", testName, className);

        // Only take screenshots for UI tests
        if (isUiTest(className)) {
            try {
                logger.info("ScreenshotListener: Capturing screenshot for failed UI test: {}", testName);

                String screenshotPath = ScreenshotUtils.takeScreenshot(testName + "_FAILED");

                if (screenshotPath != null) {
                    logger.info("ScreenshotListener: Screenshot captured successfully at: {}", screenshotPath);

                    // Attach screenshot path to test result for reporting
                    System.setProperty("screenshot.path." + testName, screenshotPath);

                    // Store in test result attributes
                    result.setAttribute("screenshotPath", screenshotPath);

                    // Attach screenshot to Allure report
                    try {
                        attachScreenshotToAllure(screenshotPath, "UI Failure Screenshot");
                        logger.info("ScreenshotListener: Screenshot attached to Allure report");
                    } catch (Exception e) {
                        logger.error("ScreenshotListener: Failed to attach screenshot to Allure: {}", e.getMessage());
                    }

                    // Attach error details to Allure
                    if (result.getThrowable() != null) {
                        String errorMsg = result.getThrowable().getMessage();
                        if (errorMsg != null) {
                            Allure.addAttachment("Error Details", "text/plain", errorMsg, ".txt");
                        }

                        // Add full stack trace
                        String stackTrace = getStackTraceAsString(result.getThrowable());
                        if (stackTrace != null) {
                            Allure.addAttachment("Stack Trace", "text/plain", stackTrace, ".txt");
                        }
                    }

                    // Update execution log with screenshot path
                    updateExecutionLogWithScreenshot(testName, className, screenshotPath, result);

                    logger.info("ScreenshotListener: Screenshot information stored in test result and database");
                } else {
                    logger.warn("ScreenshotListener: Failed to capture screenshot for test: {}", testName);
                }
            } catch (Exception e) {
                logger.error("ScreenshotListener: Error capturing screenshot for test {}: {}", testName, e.getMessage(), e);
            }
        } else {
            logger.debug("ScreenshotListener: Skipping screenshot for non-UI test: {}", testName);
        }
    }
    
    @Override
    public void onTestSuccess(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        String className = result.getTestClass().getName();
        
        logger.info("ScreenshotListener: Test PASSED - {} in class {}", testName, className);

        // Optionally capture success screenshots for UI tests in certain scenarios
        boolean captureSuccessScreenshots = Boolean.parseBoolean(
            System.getProperty("capture.success.screenshots", "false")
        );
        
        if (isUiTest(className) && captureSuccessScreenshots) {
            try {
                logger.info("ScreenshotListener: Capturing success screenshot for: {}", testName);

                String screenshotPath = ScreenshotUtils.takeScreenshot(testName + "_SUCCESS");

                if (screenshotPath != null) {
                    logger.debug("ScreenshotListener: Success screenshot captured at: {}", screenshotPath);
                    System.setProperty("screenshot.path." + testName, screenshotPath);
                    result.setAttribute("screenshotPath", screenshotPath);
                }
            } catch (Exception e) {
                logger.debug("ScreenshotListener: Error capturing success screenshot for test {}: {}", testName, e.getMessage());
            }
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        logger.info("ScreenshotListener: Test SKIPPED - {}", testName);
    }

    @Override
    public void onTestStart(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        logger.info("ScreenshotListener: Test STARTED - {}", testName);
    }

    /**
     * Determines if the test is a UI test based on class name or package structure
     */
    private boolean isUiTest(String className) {
        boolean isUi = className.contains(".ui.") ||
                       className.contains("UI") ||
                       className.contains("Selenium") ||
                       className.contains("Web") ||
                       className.toLowerCase().contains("browser");

        logger.debug("ScreenshotListener: Class {} is UI test: {}", className, isUi);
        return isUi;
    }

    /**
     * Update execution log in database with screenshot information
     */
    private void updateExecutionLogWithScreenshot(String testName, String className,
                                                   String screenshotPath, ITestResult result) {
        try {
            if (executionLogRepository != null) {
                logger.debug("ScreenshotListener: Updating execution log for test: {}", testName);

                // Create or update execution log
                ExecutionLog log = new ExecutionLog();
                log.setTestName(testName);
                log.setTestClass(className);
                log.setStatus("FAILED");
                log.setScreenshotPath(screenshotPath);
                log.setStartTime(LocalDateTime.now());
                log.setEndTime(LocalDateTime.now());

                // Extract error message from result
                Throwable throwable = result.getThrowable();
                if (throwable != null) {
                    String errorMessage = throwable.getMessage();
                    if (errorMessage != null && errorMessage.length() > 500) {
                        errorMessage = errorMessage.substring(0, 500) + "...";
                    }
                    log.setErrorMessage(errorMessage);
                    logger.debug("ScreenshotListener: Error message captured: {}", errorMessage);
                }

                // Set duration
                long duration = result.getEndMillis() - result.getStartMillis();
                log.setDurationMs(duration);

                executionLogRepository.save(log);
                logger.debug("ScreenshotListener: Execution log saved with screenshot path");
            }
        } catch (Exception e) {
            logger.warn("ScreenshotListener: Failed to update execution log: {}", e.getMessage());
        }
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

    /**
     * Get stack trace as string
     */
    private String getStackTraceAsString(Throwable throwable) {
        if (throwable == null) return null;

        StringBuilder sb = new StringBuilder();
        sb.append(throwable.toString()).append("\n");

        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
            if (sb.length() > 5000) {
                sb.append("\n... (truncated)");
                break;
            }
        }

        return sb.toString();
    }
}
