package org.example.engine;

import org.example.model.TestCase;
import org.springframework.stereotype.Component;
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
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import io.github.bonigarcia.wdm.WebDriverManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class WebUITestExecutor {

    private static final Logger log = LoggerFactory.getLogger(WebUITestExecutor.class);

    @Value("${automation.framework.webDriver.defaultBrowser:chrome}")
    private String defaultBrowser;

    @Value("${automation.framework.webDriver.headless:false}")
    private boolean headlessMode;

    @Value("${automation.framework.webDriver.implicitWait:10}")
    private int implicitWait;

    @Value("${automation.framework.reporting.outputPath:test-reports/}")
    private String reportOutputPath;

    @Value("${automation.framework.reporting.captureScreenshots:true}")
    private boolean captureScreenshots;

    private WebDriver webDriver;
    private WebDriverWait driverWait;

    public TestExecutionEngine.TestExecutionResult execute(Map<String, Object> testData, TestCase testCase) {
        TestExecutionEngine.TestExecutionResult result = new TestExecutionEngine.TestExecutionResult();
        List<String> logs = new ArrayList<>();
        List<String> screenshots = new ArrayList<>();

        try {
            logs.add("Starting Web UI test execution for: " + testCase.getName());
            
            // Initialize WebDriver
            initializeDriver(getBrowserFromTestData(testData, defaultBrowser));
            logs.add("WebDriver initialized: " + defaultBrowser);

            // Execute test based on data structure
            if (testData.containsKey("steps")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> steps = (List<Map<String, Object>>) testData.get("steps");
                result = executeSteps(steps, logs, screenshots);
            } else {
                result = executeSingleAction(testData, logs, screenshots);
            }

            if (result.isSuccess()) {
                logs.add("Web UI test completed successfully");
            }

            result.setExecutionLogs(String.join("\n", logs));

        } catch (Exception e) {
            log.error("Web UI test execution failed", e);
            result.setSuccess(false);
            result.setErrorMessage("Web UI execution failed: " + e.getMessage());
            
            // Capture screenshot on error
            if (captureScreenshots && webDriver != null) {
                String screenshotPath = captureScreenshot("error_" + System.currentTimeMillis());
                if (screenshotPath != null) {
                    screenshots.add(screenshotPath);
                }
            }

            logs.add("ERROR: " + e.getMessage());
            result.setExecutionLogs(String.join("\n", logs));

        } finally {
            result.setScreenshotPaths(screenshots);
            cleanupDriver();
        }

        return result;
    }

    private TestExecutionEngine.TestExecutionResult executeSteps(List<Map<String, Object>> steps, 
                                                               List<String> logs, List<String> screenshots) {
        TestExecutionEngine.TestExecutionResult result = new TestExecutionEngine.TestExecutionResult();
        result.setSuccess(true);

        for (int i = 0; i < steps.size(); i++) {
            Map<String, Object> step = steps.get(i);
            String action = step.getOrDefault("action", "").toString();
            
            logs.add(String.format("Executing step %d: %s", i + 1, action));

            try {
                switch (action.toLowerCase()) {
                    case "navigate":
                        navigateTo(step.get("url").toString());
                        break;
                    case "click":
                        clickElement(step.get("selector").toString());
                        break;
                    case "type":
                    case "input":
                        typeText(step.get("selector").toString(), step.get("text").toString());
                        break;
                    case "wait":
                        waitForElement(step.get("selector").toString());
                        break;
                    case "verify":
                    case "assert":
                        boolean verified = verifyElement(step.get("selector").toString(), 
                                                       step.getOrDefault("expectedText", "").toString());
                        if (!verified) {
                            result.setSuccess(false);
                            result.setErrorMessage("Verification failed at step " + (i + 1));
                            
                            if (captureScreenshots) {
                                String screenshotPath = captureScreenshot("verification_failed_step_" + (i + 1));
                                if (screenshotPath != null) {
                                    screenshots.add(screenshotPath);
                                }
                            }
                            return result;
                        }
                        break;
                    case "screenshot":
                        if (captureScreenshots) {
                            String screenshotPath = captureScreenshot("step_" + (i + 1));
                            if (screenshotPath != null) {
                                screenshots.add(screenshotPath);
                            }
                        }
                        break;
                    default:
                        logs.add("Unknown action: " + action);
                }

                logs.add(String.format("Step %d completed successfully", i + 1));

            } catch (Exception e) {
                log.error("Step {} execution failed: {}", i + 1, e.getMessage());
                result.setSuccess(false);
                result.setErrorMessage("Step " + (i + 1) + " failed: " + e.getMessage());
                
                if (captureScreenshots) {
                    String screenshotPath = captureScreenshot("error_step_" + (i + 1));
                    if (screenshotPath != null) {
                        screenshots.add(screenshotPath);
                    }
                }
                
                return result;
            }
        }

        return result;
    }

    private TestExecutionEngine.TestExecutionResult executeSingleAction(Map<String, Object> testData, 
                                                                      List<String> logs, List<String> screenshots) {
        TestExecutionEngine.TestExecutionResult result = new TestExecutionEngine.TestExecutionResult();
        result.setSuccess(true);

        try {
            // Navigate to URL if provided
            if (testData.containsKey("url")) {
                navigateTo(testData.get("url").toString());
                logs.add("Navigated to: " + testData.get("url"));
            }

            // Execute basic login test as example
            if (testData.containsKey("username") && testData.containsKey("password")) {
                executeLoginTest(testData, logs);
            }

            // Take final screenshot if enabled
            if (captureScreenshots) {
                String screenshotPath = captureScreenshot("final_result");
                if (screenshotPath != null) {
                    screenshots.add(screenshotPath);
                }
            }

            logs.add("Single action test completed successfully");

        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage("Single action test failed: " + e.getMessage());
            logs.add("ERROR: " + e.getMessage());
        }

        return result;
    }

    private void initializeDriver(String browser) {
        ChromeOptions chromeOptions = new ChromeOptions();
        FirefoxOptions firefoxOptions = new FirefoxOptions();

        if (headlessMode) {
            chromeOptions.addArguments("--headless");
            firefoxOptions.addArguments("--headless");
        }

        chromeOptions.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu");

        try {
            switch (browser.toLowerCase()) {
                case "firefox":
                    WebDriverManager.firefoxdriver().setup();
                    webDriver = new FirefoxDriver(firefoxOptions);
                    break;
                case "chrome":
                default:
                    WebDriverManager.chromedriver().setup();
                    webDriver = new ChromeDriver(chromeOptions);
                    break;
            }

            webDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(implicitWait));
            webDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            driverWait = new WebDriverWait(webDriver, Duration.ofSeconds(20));

        } catch (Exception e) {
            log.error("Failed to initialize WebDriver: {}", e.getMessage());
            throw new RuntimeException("WebDriver initialization failed", e);
        }
    }

    private void navigateTo(String url) {
        if (webDriver != null) {
            webDriver.get(url);
            log.info("Navigated to: {}", url);
        }
    }

    private void clickElement(String selector) {
        WebElement element = driverWait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(selector)));
        element.click();
        log.info("Clicked element: {}", selector);
    }

    private void typeText(String selector, String text) {
        WebElement element = driverWait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(selector)));
        element.clear();
        element.sendKeys(text);
        log.info("Typed text '{}' into element: {}", text, selector);
    }

    private void waitForElement(String selector) {
        driverWait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(selector)));
        log.info("Waited for element: {}", selector);
    }

    private boolean verifyElement(String selector, String expectedText) {
        try {
            WebElement element = driverWait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(selector)));
            
            if (expectedText == null || expectedText.isEmpty()) {
                return element.isDisplayed();
            } else {
                return element.getText().contains(expectedText);
            }
        } catch (Exception e) {
            log.warn("Element verification failed for selector: {}", selector);
            return false;
        }
    }

    private void executeLoginTest(Map<String, Object> testData, List<String> logs) {
        try {
            String username = testData.get("username").toString();
            String password = testData.get("password").toString();

            // Try common username selectors
            String[] usernameSelectors = {"#username", "#user", "input[name='username']", "input[type='email']"};
            String[] passwordSelectors = {"#password", "#pass", "input[name='password']", "input[type='password']"};
            String[] submitSelectors = {"input[type='submit']", "button[type='submit']", "#login", ".login-button"};

            boolean loginExecuted = false;

            for (String userSelector : usernameSelectors) {
                try {
                    WebElement userElement = webDriver.findElement(By.cssSelector(userSelector));
                    if (userElement.isDisplayed()) {
                        userElement.clear();
                        userElement.sendKeys(username);
                        
                        for (String passSelector : passwordSelectors) {
                            try {
                                WebElement passElement = webDriver.findElement(By.cssSelector(passSelector));
                                if (passElement.isDisplayed()) {
                                    passElement.clear();
                                    passElement.sendKeys(password);
                                    
                                    for (String submitSelector : submitSelectors) {
                                        try {
                                            WebElement submitElement = webDriver.findElement(By.cssSelector(submitSelector));
                                            if (submitElement.isDisplayed()) {
                                                submitElement.click();
                                                loginExecuted = true;
                                                logs.add("Login executed successfully");
                                                Thread.sleep(2000); // Wait for login to process
                                                return;
                                            }
                                        } catch (Exception ignored) {}
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                } catch (Exception ignored) {}
            }

            if (!loginExecuted) {
                logs.add("Could not find standard login elements, login not executed");
            }

        } catch (Exception e) {
            log.error("Login test execution failed", e);
            throw e;
        }
    }

    private String captureScreenshot(String name) {
        if (webDriver == null) return null;

        try {
            // Create screenshots directory
            Path screenshotDir = Paths.get(reportOutputPath, "screenshots");
            Files.createDirectories(screenshotDir);

            // Generate filename
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("%s_%s.png", name, timestamp);
            File screenshotFile = screenshotDir.resolve(filename).toFile();

            // Take screenshot
            TakesScreenshot takesScreenshot = (TakesScreenshot) webDriver;
            File sourceFile = takesScreenshot.getScreenshotAs(OutputType.FILE);
            Files.copy(sourceFile.toPath(), screenshotFile.toPath());

            log.info("Screenshot captured: {}", screenshotFile.getAbsolutePath());
            return screenshotFile.getAbsolutePath();

        } catch (IOException e) {
            log.error("Failed to capture screenshot: {}", e.getMessage());
            return null;
        }
    }

    private String getBrowserFromTestData(Map<String, Object> testData, String defaultBrowser) {
        return testData.getOrDefault("browser", defaultBrowser).toString();
    }

    private void cleanupDriver() {
        if (webDriver != null) {
            try {
                webDriver.quit();
                log.info("WebDriver cleaned up successfully");
            } catch (Exception e) {
                log.warn("Error during WebDriver cleanup: {}", e.getMessage());
            } finally {
                webDriver = null;
                driverWait = null;
            }
        }
    }
}
