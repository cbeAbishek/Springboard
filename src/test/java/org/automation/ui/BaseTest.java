package org.automation.ui;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class BaseTest {

    private boolean isHeadless() {
        // Priority: System property then Env var
        String sys = System.getProperty("headless", "");
        String env = System.getenv().getOrDefault("HEADLESS", "");
        String val = sys.isBlank() ? env : sys;
        if (val == null) return true; // default headless for CI safety
        val = val.trim().toLowerCase();
        return val.isEmpty() || val.equals("true") || val.equals("1") || val.equals("yes");
    }

    private static boolean chromeAvailable = detectChrome();

    private static boolean detectChrome() {
        String[] candidates = {"google-chrome", "chromium-browser", "chrome", "chromium"};
        String path = System.getenv("PATH");
        if (path != null) {
            String[] dirs = path.split(java.io.File.pathSeparator);
            for (String d : dirs) {
                for (String c : candidates) {
                    if (new java.io.File(d, c).exists()) return true;
                }
            }
        }
        return false;
    }

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        if (!chromeAvailable) {
            throw new SkipException("Skipping UI test: Chrome/Chromium binary not found on PATH. Install Chrome or run API suite only.");
        }
        try {
            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            if (isHeadless()) {
                options.addArguments("--headless=new");
            }
            options.addArguments(
                    "--no-sandbox",
                    "--disable-dev-shm-usage",
                    "--disable-gpu",
                    "--window-size=1920,1080",
                    "--remote-allow-origins=*",
                    "--disable-extensions",
                    "--disable-popup-blocking"
            );
            options.addArguments("--start-maximized");
            WebDriver webDriver = new ChromeDriver(options);
            DriverManager.setDriver(webDriver);
        } catch (Exception e) {
            System.err.println("[UI SETUP] Browser initialization failed: " + e.getMessage());
            throw new SkipException("Skipping test due to browser initialization failure");
        }
    }

    public WebDriver getDriver() {
        return DriverManager.getDriver();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        WebDriver driver = DriverManager.getDriver();
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception ignored) {}
            DriverManager.removeDriver();
        }
    }

    // Screenshot helper (optional, still usable in tests)
    public File takeScreenshot(String name) {
        WebDriver driver = DriverManager.getDriver();
        if (driver == null) return null;
        File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        File destFile = new File("target/screenshots/" + name + ".png");
        destFile.getParentFile().mkdirs();
        try {
            FileUtils.copyFile(srcFile, destFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return destFile;
    }
}
