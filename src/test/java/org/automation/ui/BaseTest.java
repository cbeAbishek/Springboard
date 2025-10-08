package org.automation.ui;

import org.apache.commons.io.FileUtils;
import org.automation.utils.DbMigrationUtil; // added
import org.automation.utils.ScreenshotUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import io.github.bonigarcia.wdm.WebDriverManager; // added

import java.io.File;
import java.io.IOException;
import java.time.Duration;

public class BaseTest {
    private static volatile boolean DB_MIGRATED = false;

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        if (!DB_MIGRATED) {
            synchronized (BaseTest.class) {
                if (!DB_MIGRATED) {
                    DbMigrationUtil.migrate();
                    DB_MIGRATED = true;
                }
            }
        }
        ChromeOptions options = new ChromeOptions();
        // Ensure we always get a matching chromedriver for the installed Chrome (v141+)
        // WebDriverManager caches the binary, so repeated calls are cheap.
        try {
            WebDriverManager.chromedriver().setup();
            System.out.println("[Driver] Using ChromeDriver version: " + WebDriverManager.chromedriver().getDownloadedDriverVersion());
        } catch (Exception e) {
            System.err.println("[Driver] Failed to resolve ChromeDriver via WebDriverManager: " + e.getMessage());
        }

        String ciEnv = System.getenv("CI");
        boolean isCI = ciEnv != null && ciEnv.equalsIgnoreCase("true");

        if (isCI) {
            System.out.println("🚀 Running in CI mode – enabling headless Chrome...");
            // Use new headless mode if supported
            options.addArguments("--headless=new");
            options.addArguments("--no-sandbox", "--disable-dev-shm-usage");
        } else {
            System.out.println("🖥️ Running locally – launching full Chrome browser...");
            options.addArguments("--start-maximized");
        }

        // General stability / compatibility flags
        options.addArguments(
                "--remote-allow-origins=*",
                "--disable-gpu",
                "--disable-background-networking",
                "--disable-background-timer-throttling",
                "--disable-client-side-phishing-detection",
                "--disable-default-apps",
                "--disable-extensions",
                "--disable-sync",
                "--metrics-recording-only",
                "--no-first-run",
                "--disable-features=Translate,NetworkServiceInProcess"
        );

        // Honor CHROME_BIN if provided
        String chromeBinary = System.getenv("CHROME_BIN");
        if (chromeBinary != null && !chromeBinary.isEmpty()) {
            options.setBinary(chromeBinary);
            System.out.println("[Driver] Using custom Chrome binary: " + chromeBinary);
        }

        WebDriver webDriver = new ChromeDriver(options);
        webDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        webDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
        DriverManager.setDriver(webDriver);
    }

    public WebDriver getDriver() {
        return DriverManager.getDriver();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        WebDriver driver = DriverManager.getDriver();
        if (result.getStatus() == ITestResult.FAILURE && driver != null) {
            String testName = result.getMethod().getMethodName();
            ScreenshotUtils.capture(driver, testName);
        }
        if (driver != null) {
            driver.quit();
            DriverManager.removeDriver();
        }
    }

    public void takeScreenshot(String name) {
        WebDriver driver = DriverManager.getDriver();
        if (driver == null) return;
        File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        File destFile = new File("target/screenshots/" + name + ".png");
        destFile.getParentFile().mkdirs();
        try {
            FileUtils.copyFile(srcFile, destFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
