package org.automation.utils;

import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ScreenshotUtils {

    private static final Logger logger = LoggerFactory.getLogger(ScreenshotUtils.class);

    public static String capture(WebDriver driver, String testName) {
        if (driver == null) return null;

        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String folderPath = "target/screenshots/";
            Files.createDirectories(Paths.get(folderPath));

            String path = folderPath + testName + "_FAILED_" + timestamp + ".png";
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(src.toPath(), Paths.get(path));

            logger.info("📸 Screenshot captured for failed test: {}", path);

            // Also attach to Allure report
            try {
                byte[] screenshotBytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                Allure.addAttachment(testName + " - Failure Screenshot", "image/png",
                                   new ByteArrayInputStream(screenshotBytes), "png");
            } catch (Exception e) {
                logger.error("Failed to attach screenshot to Allure: {}", e.getMessage());
            }

            return path;
        } catch (IOException e) {
            logger.error("Error capturing screenshot", e);
            return null;
        }
    }

    // Method referenced in listeners
    public static String takeScreenshot(String testName) {
        try {
            // Try to get driver from thread local or driver manager
            WebDriver driver = getActiveDriver();
            if (driver != null) {
                return capture(driver, testName);
            } else {
                logger.error("No active WebDriver found for screenshot: {}", testName);
                return null;
            }
        } catch (Exception e) {
            logger.error("Error taking screenshot: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Capture screenshot to a specific path (for report integration)
     */
    public static String captureToPath(String testName, String targetPath) {
        try {
            WebDriver driver = getActiveDriver();
            if (driver == null) {
                logger.error("No active WebDriver found for screenshot: {}", testName);
                return null;
            }

            if (targetPath == null) {
                return capture(driver, testName);
            }

            // Create parent directories if needed
            File targetFile = new File(targetPath);
            Files.createDirectories(targetFile.getParentFile().toPath());

            // Capture screenshot
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(src.toPath(), targetFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            logger.info("📸 Screenshot captured to: {}", targetPath);

            // Also attach to Allure report
            try {
                byte[] screenshotBytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                Allure.addAttachment(testName + " - Screenshot", "image/png",
                                   new ByteArrayInputStream(screenshotBytes), "png");
            } catch (Exception e) {
                logger.error("Failed to attach screenshot to Allure: {}", e.getMessage());
            }

            return targetPath;

        } catch (Exception e) {
            logger.error("Error capturing screenshot to path: {}", e.getMessage(), e);
            return capture(getActiveDriver(), testName); // Fallback
        }
    }

    // Helper method to get active driver (implement based on your driver management)
    private static WebDriver getActiveDriver() {
        try {
            // Try to get driver from DriverManager if it exists
            Class<?> driverManagerClass = Class.forName("org.automation.ui.DriverManager");
            java.lang.reflect.Method getMethod = driverManagerClass.getMethod("getDriver");
            return (WebDriver) getMethod.invoke(null);
        } catch (Exception e) {
            // Fallback - could implement other ways to get driver
            return null;
        }
    }
}
