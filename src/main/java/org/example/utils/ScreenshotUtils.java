package org.example.utils;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class ScreenshotUtils {

    private static final Logger log = LoggerFactory.getLogger(ScreenshotUtils.class);

    @Value("${automation.framework.reporting.outputPath:test-reports/}")
    private String reportOutputPath;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * Capture screenshot and return the file path
     */
    public String captureScreenshot(WebDriver driver, String testName) {
        if (driver == null) {
            log.warn("WebDriver is null, cannot capture screenshot");
            return null;
        }

        try {
            // Create screenshots directory
            Path screenshotDir = Paths.get(reportOutputPath, "screenshots");
            if (!Files.exists(screenshotDir)) {
                Files.createDirectories(screenshotDir);
            }

            // Generate filename with timestamp
            String timestamp = LocalDateTime.now().format(DATE_FORMAT);
            String filename = String.format("%s_%s.png", testName, timestamp);
            Path screenshotPath = screenshotDir.resolve(filename);

            // Capture screenshot
            TakesScreenshot takesScreenshot = (TakesScreenshot) driver;
            byte[] screenshotBytes = takesScreenshot.getScreenshotAs(OutputType.BYTES);

            // Write to file
            Files.write(screenshotPath, screenshotBytes);

            String absolutePath = screenshotPath.toAbsolutePath().toString();
            log.info("Screenshot captured: {}", absolutePath);
            return absolutePath;

        } catch (Exception e) {
            log.error("Failed to capture screenshot for test: {}", testName, e);
            return null;
        }
    }

    /**
     * Capture failure screenshot
     */
    public String captureFailureScreenshot(WebDriver driver, String testName) {
        return captureScreenshot(driver, testName + "_FAILURE");
    }

    /**
     * Capture step screenshot
     */
    public String captureStepScreenshot(WebDriver driver, String testName, String stepDescription) {
        String stepName = testName + "_" + stepDescription.replaceAll("[^a-zA-Z0-9]", "_");
        return captureScreenshot(driver, stepName);
    }

    /**
     * Get screenshots directory path
     */
    public String getScreenshotsDirectory() {
        return Paths.get(reportOutputPath, "screenshots").toAbsolutePath().toString();
    }

    /**
     * Clean up old screenshots
     */
    public void cleanupOldScreenshots(int daysOld) {
        try {
            Path screenshotDir = Paths.get(reportOutputPath, "screenshots");
            if (!Files.exists(screenshotDir)) {
                return;
            }

            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);

            Files.walk(screenshotDir)
                .filter(Files::isRegularFile)
                .filter(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toInstant()
                            .isBefore(cutoffDate.atZone(java.time.ZoneId.systemDefault()).toInstant());
                    } catch (IOException e) {
                        return false;
                    }
                })
                .forEach(path -> {
                    try {
                        Files.delete(path);
                        log.info("Deleted old screenshot: {}", path);
                    } catch (IOException e) {
                        log.warn("Failed to delete screenshot: {}", path, e);
                    }
                });

        } catch (IOException e) {
            log.error("Error cleaning up old screenshots", e);
        }
    }
}
