package org.automation.ui;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.commons.io.FileUtils;
import org.automation.utils.ScreenshotUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Parameters;

import java.io.File;
import java.io.IOException;

public class BaseTest {

    @BeforeMethod(alwaysRun = true)
    @Parameters({"browser"})
    public void setUp(String browser) {
        WebDriver webDriver = createDriver(browser);
        DriverManager.setDriver(webDriver); // Save driver in ThreadLocal
    }

    /**
     * Create WebDriver instance based on browser parameter
     * Uses WebDriverManager to automatically download correct driver version
     */
    private WebDriver createDriver(String browser) {
        WebDriver driver;
        String browserName = browser != null ? browser.toLowerCase() : "chrome";
        boolean headless = Boolean.parseBoolean(System.getProperty("headless", "false"));

        switch (browserName) {
            case "firefox":
                WebDriverManager.firefoxdriver().setup();
                FirefoxOptions firefoxOptions = new FirefoxOptions();
                if (headless) {
                    firefoxOptions.addArguments("--headless");
                }
                firefoxOptions.addArguments("--start-maximized");
                driver = new FirefoxDriver(firefoxOptions);
                break;

            case "edge":
                WebDriverManager.edgedriver().setup();
                EdgeOptions edgeOptions = new EdgeOptions();
                if (headless) {
                    edgeOptions.addArguments("--headless");
                }
                edgeOptions.addArguments("--start-maximized");
                driver = new EdgeDriver(edgeOptions);
                break;

            case "chrome":
            default:
                // WebDriverManager automatically downloads the correct ChromeDriver version
                WebDriverManager.chromedriver().setup();
                ChromeOptions chromeOptions = new ChromeOptions();
                chromeOptions.addArguments("--start-maximized");
                chromeOptions.addArguments("--remote-allow-origins=*");
                if (headless) {
                    chromeOptions.addArguments("--headless=new");
                    chromeOptions.addArguments("--disable-gpu");
                    chromeOptions.addArguments("--no-sandbox");
                    chromeOptions.addArguments("--disable-dev-shm-usage");
                }
                driver = new ChromeDriver(chromeOptions);
                break;
        }

        driver.manage().window().maximize();
        return driver;
    }

    public WebDriver getDriver() {
        return DriverManager.getDriver();
    }

    /**
     * Tear down driver and capture screenshot ONLY on failure.
     */
    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        WebDriver driver = DriverManager.getDriver();

        // ✅ Capture screenshot ONLY if test failed
        if (result.getStatus() == ITestResult.FAILURE && driver != null) {
            String testName = result.getMethod().getMethodName();
            ScreenshotUtils.capture(driver, testName);
        }

        if (driver != null) {
            driver.quit();
            DriverManager.removeDriver();
        }
    }

    // (Optional) Manual screenshot helper — not needed if ScreenshotUtils is used.
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
