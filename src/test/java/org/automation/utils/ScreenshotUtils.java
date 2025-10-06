package org.automation.utils;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ScreenshotUtils {

    public static String capture(WebDriver driver, String testName) {
        if (driver == null) return null;

        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String folderPath = "target/screenshots/";
            Files.createDirectories(Paths.get(folderPath));

            String path = folderPath + testName + "_FAILED_" + timestamp + ".png";
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(src.toPath(), Paths.get(path));

            System.out.println("📸 Screenshot captured for failed test: " + path);
            return path;
        } catch (IOException e) {
            e.printStackTrace();
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
                System.err.println("No active WebDriver found for screenshot: " + testName);
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error taking screenshot: " + e.getMessage());
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
                System.err.println("No active WebDriver found for screenshot: " + testName);
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

            System.out.println("📸 Screenshot captured to: " + targetPath);
            return targetPath;

        } catch (Exception e) {
            System.err.println("Error capturing screenshot to path: " + e.getMessage());
            e.printStackTrace();
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
