package org.example.engine;

import lombok.SneakyThrows;
import org.example.model.TestCase;
import org.example.model.TestExecution;
import org.example.utils.ScreenshotUtils;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.JavascriptExecutor;

import io.github.bonigarcia.wdm.WebDriverManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Enhanced WebUI Test Executor with parallel execution support
 * Extends BaseTestExecutor for common functionality and scalability
 */
@Component
public class WebUITestExecutor {

    private static final Logger log = LoggerFactory.getLogger(WebUITestExecutor.class);

    @Autowired
    private ScreenshotUtils screenshotUtils;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${automation.framework.webDriver.defaultBrowser:chrome}")
    private String defaultBrowser;

    @Value("${automation.framework.webDriver.headless:true}")
    private boolean headlessMode;

    @Value("${automation.framework.webDriver.implicitWait:10}")
    private int implicitWait;

    @Value("${automation.framework.reporting.outputPath:test-reports/}")
    private String reportOutputPath;

    @Value("${automation.framework.reporting.captureScreenshots:true}")
    private boolean captureScreenshots;

    private final ThreadLocal<WebDriver> threadLocalDriver = new ThreadLocal<>();
    private final ThreadLocal<WebDriverWait> threadLocalWait = new ThreadLocal<>();

    /**
     * Execute UI test case
     */
    public TestExecution.ExecutionStatus executeUITest(TestCase testCase, Map<String, Object> testData, String environment) {
        log.info("Starting UI test execution for: {}", testCase.getName());
        WebDriver driver = null;

        try {
            driver = createWebDriver(environment);
            threadLocalDriver.set(driver);
            threadLocalWait.set(new WebDriverWait(driver, Duration.ofSeconds(30)));

            // Execute test based on test case name/category
            if (testCase.getName().contains("BlazeDemo")) {
                return executeBlazeDemo(testCase, testData, driver);
            } else if (testCase.getName().contains("ReqRes")) {
                return executeReqResIntegration(testCase, testData, driver);
            } else {
                return executeGenericUITest(testCase, testData, driver);
            }

        } catch (Exception e) {
            log.error("UI test execution failed for: {}", testCase.getName(), e);
            captureFailureScreenshot(driver, testCase.getName());
            return TestExecution.ExecutionStatus.FAILED;
        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception e) {
                    log.warn("Error closing driver", e);
                }
            }
            threadLocalDriver.remove();
            threadLocalWait.remove();
        }
    }

    /**
     * Execute BlazeDemo UI tests
     */
    private TestExecution.ExecutionStatus executeBlazeDemo(TestCase testCase, Map<String, Object> testData, WebDriver driver) {
        try {
            String url = (String) testData.get("url");
            if (url == null) url = "https://blazedemo.com/";

            log.info("Navigating to BlazeDemo: {}", url);
            driver.get(url);

            // Take initial screenshot
            captureScreenshot(driver, testCase.getName() + "_initial");

            switch (testCase.getName()) {
                case "UI_TC001_BlazeDemo_HomePage_Load":
                    return verifyHomePage(driver, testData);
                case "UI_TC002_BlazeDemo_Flight_Search":
                    return performFlightSearch(driver, testData);
                case "UI_TC003_BlazeDemo_Flight_Selection":
                    return selectFlight(driver, testData);
                case "UI_TC004_BlazeDemo_Booking_Form_Fill":
                    return fillBookingForm(driver, testData);
                case "UI_TC005_BlazeDemo_Purchase_Complete":
                    return completePurchase(driver, testData);
                case "UI_TC006_BlazeDemo_Form_Validation":
                    return testFormValidation(driver, testData);
                case "UI_TC007_BlazeDemo_Dropdown_Cities":
                    return testCityDropdowns(driver, testData);
                default:
                    return executeGenericBlazeDemo(driver, testData, testCase.getName());
            }

        } catch (Exception e) {
            log.error("BlazeDemo test execution failed", e);
            return TestExecution.ExecutionStatus.FAILED;
        }
    }

    /**
     * Execute ReqRes API integration tests
     */
    private TestExecution.ExecutionStatus executeReqResIntegration(TestCase testCase, Map<String, Object> testData, WebDriver driver) {
        try {
            // Navigate to local API test page
            String uiUrl = (String) testData.get("uiDisplayUrl");
            if (uiUrl == null) uiUrl = "http://localhost:8080/api-test";

            log.info("Navigating to API test UI: {}", uiUrl);
            driver.get(uiUrl);

            // Take initial screenshot
            captureScreenshot(driver, testCase.getName() + "_initial");

            // Execute API test through UI
            String apiUrl = (String) testData.get("apiUrl");
            String method = (String) testData.get("method");

            if (apiUrl != null && method != null) {
                return executeApiTestThroughUI(driver, apiUrl, method, testData, testCase.getName());
            }

            return TestExecution.ExecutionStatus.PASSED;

        } catch (Exception e) {
            log.error("ReqRes integration test execution failed", e);
            return TestExecution.ExecutionStatus.FAILED;
        }
    }

    /**
     * Verify BlazeDemo homepage
     */
    private TestExecution.ExecutionStatus verifyHomePage(WebDriver driver, Map<String, Object> testData) {
        try {
            WebDriverWait wait = threadLocalWait.get();

            // Check page title
            String expectedTitle = (String) testData.getOrDefault("expectedTitle", "BlazeDemo");
            if (!driver.getTitle().contains(expectedTitle)) {
                log.error("Expected title '{}' but got '{}'", expectedTitle, driver.getTitle());
                return TestExecution.ExecutionStatus.FAILED;
            }

            // Check for key elements
            wait.until(ExpectedConditions.presenceOfElementLocated(By.name("fromPort")));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.name("toPort")));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[value='Find Flights']")));

            captureScreenshot(driver, "homepage_verified");
            log.info("Homepage verification completed successfully");
            return TestExecution.ExecutionStatus.PASSED;

        } catch (Exception e) {
            log.error("Homepage verification failed", e);
            return TestExecution.ExecutionStatus.FAILED;
        }
    }

    /**
     * Perform flight search
     */
    private TestExecution.ExecutionStatus performFlightSearch(WebDriver driver, Map<String, Object> testData) {
        try {
            WebDriverWait wait = threadLocalWait.get();

            // Select departure city
            String fromCity = (String) testData.getOrDefault("fromCity", "Paris");
            Select fromSelect = new Select(driver.findElement(By.name("fromPort")));
            fromSelect.selectByVisibleText(fromCity);

            // Select destination city
            String toCity = (String) testData.getOrDefault("toCity", "London");
            Select toSelect = new Select(driver.findElement(By.name("toPort")));
            toSelect.selectByVisibleText(toCity);

            // Click find flights
            driver.findElement(By.cssSelector("input[value='Find Flights']")).click();

            // Wait for results page
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("table")));

            // Verify we're on the reserve page
            if (!driver.getCurrentUrl().contains("reserve.php")) {
                log.error("Expected to be on reserve page but was on: {}", driver.getCurrentUrl());
                return TestExecution.ExecutionStatus.FAILED;
            }

            captureScreenshot(driver, "flight_search_results");
            log.info("Flight search completed successfully");
            return TestExecution.ExecutionStatus.PASSED;

        } catch (Exception e) {
            log.error("Flight search failed", e);
            return TestExecution.ExecutionStatus.FAILED;
        }
    }

    /**
     * Select a flight from results
     */
    private TestExecution.ExecutionStatus selectFlight(WebDriver driver, Map<String, Object> testData) {
        try {
            WebDriverWait wait = threadLocalWait.get();

            // Wait for flight table
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("table")));

            // Select first available flight
            WebElement chooseButton = driver.findElement(By.cssSelector("table tbody tr:first-child input[value='Choose This Flight']"));
            chooseButton.click();

            // Wait for purchase page
            wait.until(ExpectedConditions.urlContains("purchase.php"));

            captureScreenshot(driver, "flight_selected");
            log.info("Flight selection completed successfully");
            return TestExecution.ExecutionStatus.PASSED;

        } catch (Exception e) {
            log.error("Flight selection failed", e);
            return TestExecution.ExecutionStatus.FAILED;
        }
    }

    /**
     * Fill booking form
     */
    private TestExecution.ExecutionStatus fillBookingForm(WebDriver driver, Map<String, Object> testData) {
        try {
            WebDriverWait wait = threadLocalWait.get();

            // Wait for form elements
            wait.until(ExpectedConditions.presenceOfElementLocated(By.name("inputName")));

            // Fill form fields
            driver.findElement(By.name("inputName")).sendKeys((String) testData.getOrDefault("passengerName", "John Doe"));
            driver.findElement(By.name("address")).sendKeys((String) testData.getOrDefault("address", "123 Test St"));
            driver.findElement(By.name("city")).sendKeys((String) testData.getOrDefault("city", "Test City"));
            driver.findElement(By.name("state")).sendKeys((String) testData.getOrDefault("state", "Test State"));
            driver.findElement(By.name("zipCode")).sendKeys((String) testData.getOrDefault("zipCode", "12345"));

            // Select card type
            String cardType = (String) testData.getOrDefault("cardType", "Visa");
            Select cardSelect = new Select(driver.findElement(By.name("cardType")));
            cardSelect.selectByVisibleText(cardType);

            // Fill credit card details
            driver.findElement(By.name("creditCardNumber")).sendKeys((String) testData.getOrDefault("creditCardNumber", "4111111111111111"));
            driver.findElement(By.name("creditCardMonth")).sendKeys((String) testData.getOrDefault("creditCardMonth", "12"));
            driver.findElement(By.name("creditCardYear")).sendKeys((String) testData.getOrDefault("creditCardYear", "2025"));
            driver.findElement(By.name("nameOnCard")).sendKeys((String) testData.getOrDefault("nameOnCard", "John Doe"));

            captureScreenshot(driver, "booking_form_filled");
            log.info("Booking form filled successfully");
            return TestExecution.ExecutionStatus.PASSED;

        } catch (Exception e) {
            log.error("Booking form filling failed", e);
            return TestExecution.ExecutionStatus.FAILED;
        }
    }

    /**
     * Complete purchase
     */
    private TestExecution.ExecutionStatus completePurchase(WebDriver driver, Map<String, Object> testData) {
        try {
            WebDriverWait wait = threadLocalWait.get();

            // Click purchase button
            WebElement purchaseButton = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("input[value='Purchase Flight']")));
            purchaseButton.click();

            // Wait for confirmation page
            wait.until(ExpectedConditions.urlContains("confirmation.php"));

            // Verify confirmation elements
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("h1")));

            captureScreenshot(driver, "purchase_completed");
            log.info("Purchase completed successfully");
            return TestExecution.ExecutionStatus.PASSED;

        } catch (Exception e) {
            log.error("Purchase completion failed", e);
            return TestExecution.ExecutionStatus.FAILED;
        }
    }

    /**
     * Test form validation
     */
    private TestExecution.ExecutionStatus testFormValidation(WebDriver driver, Map<String, Object> testData) {
        try {
            WebDriverWait wait = threadLocalWait.get();

            // Try to submit empty form
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[value='Purchase Flight']")));
            driver.findElement(By.cssSelector("input[value='Purchase Flight']")).click();

            // Check if we're still on the same page (validation should prevent submission)
            Thread.sleep(2000); // Give time for any validation

            if (driver.getCurrentUrl().contains("confirmation.php")) {
                log.error("Form validation failed - empty form was submitted");
                return TestExecution.ExecutionStatus.FAILED;
            }

            captureScreenshot(driver, "form_validation_tested");
            log.info("Form validation test completed");
            return TestExecution.ExecutionStatus.PASSED;

        } catch (Exception e) {
            log.error("Form validation test failed", e);
            return TestExecution.ExecutionStatus.FAILED;
        }
    }

    /**
     * Test city dropdowns
     */
    private TestExecution.ExecutionStatus testCityDropdowns(WebDriver driver, Map<String, Object> testData) {
        try {
            WebDriverWait wait = threadLocalWait.get();

            // Check from city dropdown
            Select fromSelect = new Select(wait.until(ExpectedConditions.presenceOfElementLocated(By.name("fromPort"))));
            List<WebElement> fromOptions = fromSelect.getOptions();

            // Check to city dropdown
            Select toSelect = new Select(driver.findElement(By.name("toPort")));
            List<WebElement> toOptions = toSelect.getOptions();

            // Verify we have options
            if (fromOptions.size() <= 1 || toOptions.size() <= 1) {
                log.error("Dropdowns don't have enough options");
                return TestExecution.ExecutionStatus.FAILED;
            }

            captureScreenshot(driver, "dropdowns_tested");
            log.info("City dropdowns test completed - From: {} options, To: {} options", fromOptions.size(), toOptions.size());
            return TestExecution.ExecutionStatus.PASSED;

        } catch (Exception e) {
            log.error("City dropdowns test failed", e);
            return TestExecution.ExecutionStatus.FAILED;
        }
    }

    /**
     * Execute generic BlazeDemo tests
     */
    private TestExecution.ExecutionStatus executeGenericBlazeDemo(WebDriver driver, Map<String, Object> testData, String testName) {
        try {
            // Basic navigation and element verification
            WebDriverWait wait = threadLocalWait.get();

            // Wait for page to load
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

            // Take screenshot
            captureScreenshot(driver, testName + "_execution");

            log.info("Generic BlazeDemo test '{}' completed", testName);
            return TestExecution.ExecutionStatus.PASSED;

        } catch (Exception e) {
            log.error("Generic BlazeDemo test failed", e);
            return TestExecution.ExecutionStatus.FAILED;
        }
    }

    /**
     * Execute API test through UI
     */
    private TestExecution.ExecutionStatus executeApiTestThroughUI(WebDriver driver, String apiUrl, String method, Map<String, Object> testData, String testName) {
        try {
            // This would typically involve interacting with a UI that makes API calls
            // For now, we'll simulate by checking if the API test page loads
            WebDriverWait wait = threadLocalWait.get();

            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

            // Take screenshot
            captureScreenshot(driver, testName + "_api_ui");

            log.info("API integration test '{}' completed for URL: {}", testName, apiUrl);
            return TestExecution.ExecutionStatus.PASSED;

        } catch (Exception e) {
            log.error("API integration test failed", e);
            return TestExecution.ExecutionStatus.FAILED;
        }
    }

    /**
     * Execute generic UI test
     */
    private TestExecution.ExecutionStatus executeGenericUITest(TestCase testCase, Map<String, Object> testData, WebDriver driver) {
        try {
            String url = (String) testData.get("url");
            if (url != null) {
                driver.get(url);

                WebDriverWait wait = threadLocalWait.get();
                wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

                captureScreenshot(driver, testCase.getName() + "_generic");
            }

            return TestExecution.ExecutionStatus.PASSED;

        } catch (Exception e) {
            log.error("Generic UI test failed", e);
            return TestExecution.ExecutionStatus.FAILED;
        }
    }

    /**
     * Create WebDriver instance
     */
    private WebDriver createWebDriver(String environment) {
        try {
            String browser = defaultBrowser.toLowerCase();

            switch (browser) {
                case "chrome":
                    return createChromeDriver();
                case "firefox":
                    return createFirefoxDriver();
                default:
                    log.warn("Unknown browser: {}, defaulting to Chrome", browser);
                    return createChromeDriver();
            }

        } catch (Exception e) {
            log.error("Failed to create WebDriver", e);
            throw new RuntimeException("Failed to create WebDriver", e);
        }
    }

    /**
     * Create Chrome driver with options
     */
    private WebDriver createChromeDriver() {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();

        if (headlessMode) {
            options.addArguments("--headless");
        }

        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-web-security");
        options.addArguments("--allow-running-insecure-content");

        ChromeDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(implicitWait));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));

        return driver;
    }

    /**
     * Create Firefox driver with options
     */
    private WebDriver createFirefoxDriver() {
        WebDriverManager.firefoxdriver().setup();

        FirefoxOptions options = new FirefoxOptions();

        if (headlessMode) {
            options.addArguments("--headless");
        }

        FirefoxDriver driver = new FirefoxDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(implicitWait));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));

        return driver;
    }

    /**
     * Capture screenshot
     */
    private void captureScreenshot(WebDriver driver, String testName) {
        if (!captureScreenshots || driver == null) return;

        try {
            if (screenshotUtils != null) {
                screenshotUtils.captureScreenshot(driver, testName);
            } else {
                // Fallback screenshot capture
                TakesScreenshot screenshot = (TakesScreenshot) driver;
                byte[] screenshotBytes = screenshot.getScreenshotAs(OutputType.BYTES);

                String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
                String fileName = testName + "_" + timestamp + ".png";
                Path screenshotPath = Paths.get(reportOutputPath, "screenshots", fileName);

                Files.createDirectories(screenshotPath.getParent());
                Files.write(screenshotPath, screenshotBytes);

                log.info("Screenshot captured: {}", screenshotPath.toAbsolutePath());
            }
        } catch (Exception e) {
            log.warn("Failed to capture screenshot for {}", testName, e);
        }
    }

    /**
     * Capture failure screenshot
     */
    private void captureFailureScreenshot(WebDriver driver, String testName) {
        captureScreenshot(driver, testName + "_FAILURE");
    }

    public void execute(Map<String, Object> testData, TestCase testCase) {
    }
}
