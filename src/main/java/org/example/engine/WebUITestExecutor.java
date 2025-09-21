package org.example.engine;

import lombok.SneakyThrows;
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
public class WebUITestExecutor extends BaseTestExecutor {

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

    @Value("${automation.framework.webDriver.chromiumPath:/usr/bin/chromium-browser}")
    private String chromiumPath;

    @Value("${automation.framework.webDriver.autoDetectDisplay:true}")
    private boolean autoDetectDisplay;

    // Thread-safe WebDriver management for parallel execution
    private final ThreadLocal<WebDriver> threadLocalDriver = new ThreadLocal<>();
    private final ThreadLocal<WebDriverWait> threadLocalWait = new ThreadLocal<>();
    private final Map<String, WebDriver> driverPool = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> driverLastUsed = new ConcurrentHashMap<>();

    /**
     * Configuration class for test execution
     */
    public static class TestExecutionConfig {
        private String browser = "chrome";
        private boolean headless = false;
        private boolean enableScreenshots = true;
        private int timeout = 30;

        public String getBrowser() { return browser; }
        public void setBrowser(String browser) { this.browser = browser; }

        public boolean isHeadless() { return headless; }
        public void setHeadless(boolean headless) { this.headless = headless; }

        public boolean isEnableScreenshots() { return enableScreenshots; }
        public void setEnableScreenshots(boolean enableScreenshots) { this.enableScreenshots = enableScreenshots; }

        public int getTimeout() { return timeout; }
        public void setTimeout(int timeout) { this.timeout = timeout; }
    }

    /**
     * Main execution method - implements BaseTestExecutor contract
     */
    @Override
    public TestExecutionResult execute(Map<String, Object> testData, TestCase testCase) {
        String executionId = generateExecutionId();
        TestExecutionResult result = new TestExecutionResult();
        result.setExecutionId(Long.parseLong(executionId));
        result.setTestType("WEB_UI");

        List<String> logs = new ArrayList<>();
        List<String> screenshots = new ArrayList<>();

        try {
            logStep(executionId, "Starting Web UI test execution for: " + testCase.getName(), logs);

            // Initialize thread-safe WebDriver
            initializeDriverForThread(getBrowserFromTestData(testData, defaultBrowser), executionId);
            logStep(executionId, "WebDriver initialized: " + defaultBrowser, logs);

            // Capture initial metrics
            captureMetric(result, "browser", defaultBrowser);
            captureMetric(result, "headless", headlessMode);
            captureMetric(result, "environment", testData.getOrDefault("environment", "default"));

            // Execute test based on data structure
            if (testData.containsKey("steps")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> steps = (List<Map<String, Object>>) testData.get("steps");
                result = executeSteps(steps, logs, screenshots, executionId, result);
            } else {
                result = executeSingleAction(testData, logs, screenshots, executionId, result);
            }

            if (result.isSuccess()) {
                logStep(executionId, "Web UI test completed successfully", logs);
            }

            result.setExecutionLogs(String.join("\n", logs));
            captureMetric(result, "total_screenshots", screenshots.size());

        } catch (Exception e) {
            log.error("Web UI test execution {} failed", executionId, e);
            result.setSuccess(false);
            result.setErrorMessage("Web UI execution failed: " + e.getMessage());
            
            // Capture screenshot on error
            if (captureScreenshots) {
                String screenshotPath = captureScreenshot("error_" + executionId, executionId);
                if (screenshotPath != null) {
                    screenshots.add(screenshotPath);
                }
            }

            logStep(executionId, "ERROR: " + e.getMessage(), logs);
            result.setExecutionLogs(String.join("\n", logs));

        } finally {
            result.setScreenshotPaths(screenshots);
            result.markCompleted();
            cleanupDriverForThread(executionId);
        }

        return result;
    }

    /**
     * Execute multiple test cases in parallel
     */
    public CompletableFuture<List<TestExecutionResult>> executeParallel(
            List<TestCase> testCases,
            Map<String, Object> commonTestData,
            String environment,
            int maxParallelThreads) {

        log.info("Starting parallel execution of {} test cases with {} threads", testCases.size(), maxParallelThreads);

        List<CompletableFuture<TestExecutionResult>> futures = new ArrayList<>();

        for (TestCase testCase : testCases) {
            // Create test-specific data by merging common data with test case data
            Map<String, Object> testData = new HashMap<>(commonTestData);
            if (testCase.getTestData() != null && !testCase.getTestData().isEmpty()) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> caseSpecificData = (Map<String, Object>)
                        new com.fasterxml.jackson.databind.ObjectMapper().readValue(testCase.getTestData(), Map.class);
                    testData.putAll(caseSpecificData);
                } catch (Exception e) {
                    log.warn("Failed to parse test data for test case {}: {}", testCase.getName(), e.getMessage());
                }
            }

            CompletableFuture<TestExecutionResult> future = executeAsync(testData, testCase, environment);
            futures.add(future);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(java.util.stream.Collectors.toList()));
    }

    private TestExecutionResult executeSteps(List<Map<String, Object>> steps,
                                                               List<String> logs, List<String> screenshots, String executionId, TestExecutionResult result) {
        result.setSuccess(true);

        for (int i = 0; i < steps.size(); i++) {
            Map<String, Object> step = steps.get(i);
            String action = step.getOrDefault("action", "").toString();
            
            logStep(executionId, String.format("Executing step %d: %s", i + 1, action), logs);

            try {
                switch (action.toLowerCase()) {
                    case "navigate":
                        navigateTo(step.get("url").toString(), executionId);
                        break;
                    case "click":
                        clickElement(step.get("selector").toString(), executionId);
                        break;
                    case "type":
                    case "input":
                        typeText(step.get("selector").toString(), step.get("text").toString(), executionId);
                        break;
                    case "wait":
                        waitForElement(step.get("selector").toString(), executionId);
                        break;
                    case "verify":
                    case "assert":
                        boolean verified = verifyElement(step.get("selector").toString(),
                                                       step.getOrDefault("expectedText", "").toString(), executionId);
                        if (!verified) {
                            result.setSuccess(false);
                            result.setErrorMessage("Verification failed at step " + (i + 1));

                            if (captureScreenshots) {
                                String screenshotPath = captureScreenshot("verification_failed_step_" + (i + 1), executionId);
                                if (screenshotPath != null) {
                                    screenshots.add(screenshotPath);
                                }
                            }
                            return result;
                        }
                        break;
                    case "screenshot":
                        if (captureScreenshots) {
                            String screenshotPath = captureScreenshot("step_" + (i + 1), executionId);
                            if (screenshotPath != null) {
                                screenshots.add(screenshotPath);
                            }
                        }
                        break;
                    default:
                        logStep(executionId, "Unknown action: " + action, logs);
                }

                logStep(executionId, String.format("Step %d completed successfully", i + 1), logs);

            } catch (Exception e) {
                log.error("Step {} execution failed: {}", i + 1, e.getMessage());
                result.setSuccess(false);
                result.setErrorMessage("Step " + (i + 1) + " failed: " + e.getMessage());
                
                if (captureScreenshots) {
                    String screenshotPath = captureScreenshot("error_step_" + (i + 1), executionId);
                    if (screenshotPath != null) {
                        screenshots.add(screenshotPath);
                    }
                }

                return result;
            }
        }

        return result;
    }

    private TestExecutionResult executeSingleAction(Map<String, Object> testData,
                                                                      List<String> logs, List<String> screenshots, String executionId, TestExecutionResult result) {
        result.setSuccess(true);

        try {
            // Navigate to URL if provided
            if (testData.containsKey("url")) {
                navigateTo(testData.get("url").toString(), executionId);
                logs.add("Navigated to: " + testData.get("url"));

                // Take a screenshot after navigation to verify page loaded
                if (captureScreenshots) {
                    String screenshotPath = captureScreenshot("after_navigation", executionId);
                    if (screenshotPath != null) {
                        screenshots.add(screenshotPath);
                        logs.add("Screenshot taken after navigation: " + screenshotPath);
                    }
                }
            }

            // Execute basic login test
            if (testData.containsKey("username") && testData.containsKey("password")) {
                executeLoginTest(testData, logs, executionId);
            }

            // Take final screenshot if enabled
            if (captureScreenshots) {
                String screenshotPath = captureScreenshot("final_result", executionId);
                if (screenshotPath != null) {
                    screenshots.add(screenshotPath);
                    logs.add("Final screenshot taken: " + screenshotPath);
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
        try {
            log.info("Initializing WebDriver for browser: {}", browser);

            // Comprehensive system properties to suppress all CDP and logging warnings
            System.setProperty("webdriver.chrome.silentOutput", "true");
            System.setProperty("webdriver.chrome.logfile", "/dev/null");
            System.setProperty("webdriver.chrome.verboseLogging", "false");
            System.setProperty("webdriver.chrome.args", "--silent --log-level=3");
            System.setProperty("org.openqa.selenium.logging.ignore", "org.openqa.selenium.devtools");
            System.setProperty("selenium.LOGGER", "OFF");

            // Disable specific Selenium logging
            java.util.logging.Logger.getLogger("org.openqa.selenium").setLevel(java.util.logging.Level.OFF);
            java.util.logging.Logger.getLogger("org.openqa.selenium.devtools").setLevel(java.util.logging.Level.OFF);
            java.util.logging.Logger.getLogger("org.openqa.selenium.remote").setLevel(java.util.logging.Level.OFF);

            ChromeOptions chromeOptions = new ChromeOptions();
            FirefoxOptions firefoxOptions = new FirefoxOptions();

            // Enhanced Chrome options for preventing blank pages and Google opening pages
            chromeOptions.addArguments("--no-sandbox");
            chromeOptions.addArguments("--disable-dev-shm-usage");
            chromeOptions.addArguments("--disable-gpu");
            chromeOptions.addArguments("--disable-extensions");
            chromeOptions.addArguments("--disable-web-security");
            chromeOptions.addArguments("--allow-running-insecure-content");
            chromeOptions.addArguments("--disable-background-timer-throttling");
            chromeOptions.addArguments("--disable-backgrounding-occluded-windows");
            chromeOptions.addArguments("--disable-renderer-backgrounding");
            chromeOptions.addArguments("--disable-features=TranslateUI");
            chromeOptions.addArguments("--window-size=1920,1080");

            // Fix blank page and Google opening page issues
            chromeOptions.addArguments("--disable-search-engine-choice-screen");
            chromeOptions.addArguments("--no-first-run");
            chromeOptions.addArguments("--no-default-browser-check");
            chromeOptions.addArguments("--disable-default-apps");
            chromeOptions.addArguments("--disable-background-networking");
            chromeOptions.addArguments("--disable-component-extensions-with-background-pages");
            chromeOptions.addArguments("--start-maximized");

            // Prevent opening with blank or Google pages
            chromeOptions.addArguments("--homepage=about:blank");
            chromeOptions.addArguments("--disable-sync");

            // Complete CDP and DevTools suppression
            chromeOptions.addArguments("--disable-dev-tools");
            chromeOptions.addArguments("--disable-extensions-http-throttling");
            chromeOptions.addArguments("--disable-logging");
            chromeOptions.addArguments("--log-level=3");
            chromeOptions.addArguments("--silent");
            chromeOptions.addArguments("--disable-gpu-logging");
            chromeOptions.addArguments("--disable-blink-features=AutomationControlled");
            chromeOptions.addArguments("--disable-infobars");
            chromeOptions.addArguments("--disable-notifications");
            chromeOptions.addArguments("--disable-popup-blocking");
            chromeOptions.addArguments("--disable-translate");
            chromeOptions.addArguments("--disable-ipc-flooding-protection");

            // Experimental options to completely disable automation detection and CDP
            chromeOptions.setExperimentalOption("useAutomationExtension", false);
            chromeOptions.setExperimentalOption("excludeSwitches",
                java.util.Arrays.asList("enable-automation", "enable-logging"));
            chromeOptions.setExperimentalOption("detach", true);

            // Disable all Chrome logging
            java.util.Map<String, Object> prefs = new java.util.HashMap<>();
            prefs.put("profile.default_content_setting_values.notifications", 2);
            prefs.put("profile.default_content_settings.popups", 0);
            prefs.put("profile.managed_default_content_settings.images", 2);
            prefs.put("homepage_is_newtabpage", false);
            prefs.put("homepage", "about:blank");
            chromeOptions.setExperimentalOption("prefs", prefs);

            // Additional logging suppression via capabilities
            org.openqa.selenium.logging.LoggingPreferences logPrefs = new org.openqa.selenium.logging.LoggingPreferences();
            logPrefs.enable(org.openqa.selenium.logging.LogType.BROWSER, java.util.logging.Level.OFF);
            logPrefs.enable(org.openqa.selenium.logging.LogType.CLIENT, java.util.logging.Level.OFF);
            logPrefs.enable(org.openqa.selenium.logging.LogType.DRIVER, java.util.logging.Level.OFF);
            logPrefs.enable(org.openqa.selenium.logging.LogType.PERFORMANCE, java.util.logging.Level.OFF);
            logPrefs.enable(org.openqa.selenium.logging.LogType.PROFILER, java.util.logging.Level.OFF);
            logPrefs.enable(org.openqa.selenium.logging.LogType.SERVER, java.util.logging.Level.OFF);
            chromeOptions.setCapability("goog:loggingPrefs", logPrefs);

            // Check if running in headless mode or on server environment
            if (headlessMode || isServerEnvironment()) {
                chromeOptions.addArguments("--headless=new");
                chromeOptions.addArguments("--enable-features=NetworkService,NetworkServiceLogging");
                chromeOptions.addArguments("--disable-client-side-phishing-detection");
                chromeOptions.addArguments("--disable-hang-monitor");
                chromeOptions.addArguments("--disable-popup-blocking");
                chromeOptions.addArguments("--disable-prompt-on-repost");
                chromeOptions.addArguments("--metrics-recording-only");
                chromeOptions.addArguments("--safebrowsing-disable-auto-update");
                chromeOptions.addArguments("--enable-automation");
                chromeOptions.addArguments("--password-store=basic");
                chromeOptions.addArguments("--use-mock-keychain");
                log.info("Running in headless mode");
            }

            // Firefox options for Linux
            if (headlessMode || isServerEnvironment()) {
                firefoxOptions.addArguments("--headless");
            }
            firefoxOptions.addPreference("dom.webdriver.enabled", false);
            firefoxOptions.addPreference("useAutomationExtension", false);

            switch (browser.toLowerCase()) {
                case "firefox":
                    log.info("Setting up Firefox WebDriver");
                    try {
                        WebDriverManager.firefoxdriver().setup();
                        threadLocalDriver.set(new FirefoxDriver(firefoxOptions));
                        log.info("Firefox WebDriver initialized successfully");
                    } catch (Exception e) {
                        log.error("Firefox WebDriver initialization failed: {}", e.getMessage());
                        // Fallback to Chrome
                        log.info("Falling back to Chrome WebDriver");
                        setupChromeDriver(chromeOptions);
                    }
                    break;
                case "chrome":
                default:
                    setupChromeDriver(chromeOptions);
                    break;
            }

            WebDriver webDriver = threadLocalDriver.get();
            if (webDriver != null) {
                // Increased timeouts for better reliability
                webDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(implicitWait));
                webDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
                webDriver.manage().timeouts().scriptTimeout(Duration.ofSeconds(30));

                if (!headlessMode && !isServerEnvironment()) {
                    webDriver.manage().window().maximize();
                }
                threadLocalWait.set(new WebDriverWait(webDriver, Duration.ofSeconds(30)));
                log.info("WebDriver configured successfully with timeouts and window settings");
            } else {
                throw new RuntimeException("Failed to initialize any WebDriver");
            }

        } catch (Exception e) {
            log.error("Failed to initialize WebDriver: {}", e.getMessage(), e);
            throw new RuntimeException("WebDriver initialization failed: " + e.getMessage(), e);
        }
    }

    private void setupChromeDriver(ChromeOptions chromeOptions) {
        log.info("Setting up Chrome WebDriver");
        try {
            // Check if Chrome is available
            if (!isChromeAvailable()) {
                log.warn("Chrome browser not detected, attempting to install or use alternative");
                // Try to use Chromium as fallback
                chromeOptions.setBinary(chromiumPath);
            }

            WebDriverManager chromeDriverManager = WebDriverManager.chromedriver();
            chromeDriverManager.clearDriverCache();
            chromeDriverManager.setup();

            log.info("WebDriverManager configured ChromeDriver for Chrome browser");

            WebDriver webDriver = new ChromeDriver(chromeOptions);
            threadLocalDriver.set(webDriver);
            log.info("Chrome WebDriver initialized successfully");
        } catch (Exception e) {
            log.error("Chrome WebDriver initialization failed: {}", e.getMessage());
            throw new RuntimeException("Chrome WebDriver initialization failed", e);
        }
    }

    private boolean isServerEnvironment() {
        if (!autoDetectDisplay) {
            log.debug("Auto display detection disabled; using headlessMode config: {}", headlessMode);
            return headlessMode;
        }
        String display = System.getenv("DISPLAY");
        boolean isHeadless = display == null || display.trim().isEmpty();
        log.debug("Environment check - DISPLAY: {}, isHeadless: {}", display, isHeadless);
        return isHeadless;
    }

    private boolean isChromeAvailable() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"which", "google-chrome"});
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.debug("Google Chrome found via 'which' command");
                return true;
            }

            String[] chromePaths = {
                "/usr/bin/google-chrome",
                "/usr/bin/google-chrome-stable",
                "/usr/bin/chromium-browser",
                "/usr/bin/chromium",
                "/snap/bin/chromium"
            };

            for (String path : chromePaths) {
                File chromeFile = new File(path);
                if (chromeFile.exists() && chromeFile.canExecute()) {
                    log.debug("Chrome/Chromium found at: {}", path);
                    return true;
                }
            }

            log.warn("Chrome browser not found in standard locations");
            return false;
        } catch (Exception e) {
            log.warn("Error checking Chrome availability: {}", e.getMessage());
            return false;
        }
    }

    private void navigateTo(String url, String executionId) {
        WebDriver webDriver = threadLocalDriver.get();
        WebDriverWait driverWait = threadLocalWait.get();
        if (webDriver != null) {
            try {
                log.info("Navigating to URL: {}", url);
                webDriver.get(url);

                // Wait for page to load completely
                driverWait.until(ExpectedConditions.jsReturnsValue("return document.readyState === 'complete'"));

                // Additional wait for dynamic content
                Thread.sleep(2000);

                // Verify the page has loaded by checking the title
                String pageTitle = webDriver.getTitle();
                log.info("Page loaded successfully. Title: {}", pageTitle);

                // Additional verification that we're not on a blank or error page
                String currentUrl = webDriver.getCurrentUrl();
                if (currentUrl.contains("about:blank") || pageTitle.isEmpty()) {
                    log.warn("Detected blank page, retrying navigation...");
                    Thread.sleep(1000);
                    webDriver.get(url);
                    driverWait.until(ExpectedConditions.jsReturnsValue("return document.readyState === 'complete'"));
                    Thread.sleep(2000);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Navigation interrupted: {}", e.getMessage());
                throw new RuntimeException("Navigation interrupted", e);
            } catch (Exception e) {
                log.error("Navigation to {} failed: {}", url, e.getMessage());
                throw new RuntimeException("Failed to navigate to: " + url, e);
            }
        }
    }

    private void clickElement(String selector, String executionId) {
        WebDriver webDriver = threadLocalDriver.get();
        WebDriverWait driverWait = threadLocalWait.get();
        WebElement element = driverWait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(selector)));
        element.click();
        log.info("Clicked element: {}", selector);
    }

    private void typeText(String selector, String text, String executionId) {
        WebDriver webDriver = threadLocalDriver.get();
        WebDriverWait driverWait = threadLocalWait.get();
        WebElement element = driverWait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(selector)));
        element.clear();
        element.sendKeys(text);
        log.info("Typed text '{}' into element: {}", text, selector);
    }

    private void waitForElement(String selector, String executionId) {
        WebDriver webDriver = threadLocalDriver.get();
        WebDriverWait driverWait = threadLocalWait.get();
        driverWait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(selector)));
        log.info("Waited for element: {}", selector);
    }

    private boolean verifyElement(String selector, String expectedText, String executionId) {
        WebDriver webDriver = threadLocalDriver.get();
        WebDriverWait driverWait = threadLocalWait.get();
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

    @SneakyThrows
    private void executeLoginTest(Map<String, Object> testData, List<String> logs, String executionId) {
        WebDriver webDriver = threadLocalDriver.get();
        WebDriverWait driverWait = threadLocalWait.get();
        try {
            String username = testData.get("username").toString();
            String password = testData.get("password").toString();

            log.info("Executing login test with username: {}", username);
            logs.add("Starting login process...");

            // Wait for page to fully load first
            driverWait.until(ExpectedConditions.jsReturnsValue("return document.readyState === 'complete'"));
            Thread.sleep(1000);

            // BlazeDemo specific selectors first, then fallback to common ones
            String[] usernameSelectors = {
                "input[name='email']", // BlazeDemo uses email field
                "input[type='email']",
                "#email",
                "#username",
                "#user",
                "input[name='username']"
            };

            String[] passwordSelectors = {
                "input[name='password']", // BlazeDemo password field
                "#password",
                "#pass",
                "input[type='password']"
            };

            String[] submitSelectors = {
                "input[type='submit']", // BlazeDemo login button
                "button[type='submit']",
                "input[value='Login']",
                "#login",
                ".login-button",
                ".btn-primary"
            };

            boolean loginExecuted = false;
            WebElement userElement = null;
            WebElement passElement = null;
            WebElement submitElement = null;

            // Find username field
            for (String userSelector : usernameSelectors) {
                try {
                    userElement = driverWait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(userSelector)));
                    if (userElement.isDisplayed() && userElement.isEnabled()) {
                        log.info("Found username field with selector: {}", userSelector);
                        logs.add("Found username field: " + userSelector);
                        break;
                    }
                } catch (Exception e) {
                    log.debug("Username selector {} not found: {}", userSelector, e.getMessage());
                }
            }

            // Find password field
            if (userElement != null) {
                for (String passSelector : passwordSelectors) {
                    try {
                        passElement = webDriver.findElement(By.cssSelector(passSelector));
                        if (passElement.isDisplayed() && passElement.isEnabled()) {
                            log.info("Found password field with selector: {}", passSelector);
                            logs.add("Found password field: " + passSelector);
                            break;
                        }
                    } catch (Exception e) {
                        log.debug("Password selector {} not found: {}", passSelector, e.getMessage());
                    }
                }
            }

            // Find submit button
            if (userElement != null && passElement != null) {
                for (String submitSelector : submitSelectors) {
                    try {
                        submitElement = webDriver.findElement(By.cssSelector(submitSelector));
                        if (submitElement.isDisplayed() && submitElement.isEnabled()) {
                            log.info("Found submit button with selector: {}", submitSelector);
                            logs.add("Found submit button: " + submitSelector);
                            break;
                        }
                    } catch (Exception e) {
                        log.debug("Submit selector {} not found: {}", submitSelector, e.getMessage());
                    }
                }
            }

            // Execute login if all elements found
            if (userElement != null && passElement != null && submitElement != null) {
                try {
                    // Clear and enter username
                    userElement.clear();
                    Thread.sleep(500);
                    userElement.sendKeys(username);
                    logs.add("Entered username: " + username);

                    // Clear and enter password
                    passElement.clear();
                    Thread.sleep(500);
                    passElement.sendKeys(password);
                    logs.add("Entered password");

                    // Click submit button
                    Thread.sleep(500);
                    submitElement.click();
                    logs.add("Clicked login button");

                    // Wait for login to process
                    Thread.sleep(3000);

                    // Verify login by checking URL change or page title
                    String currentUrl = webDriver.getCurrentUrl();
                    String pageTitle = webDriver.getTitle();

                    logs.add("After login - URL: " + currentUrl + ", Title: " + pageTitle);

                    // Check if login was successful (URL changed or title changed)
                    if (!currentUrl.contains("login") || pageTitle.toLowerCase().contains("welcome") ||
                        pageTitle.toLowerCase().contains("dashboard") || pageTitle.toLowerCase().contains("home")) {
                        loginExecuted = true;
                        logs.add("Login appears successful - page changed");
                    } else {
                        logs.add("Login may have failed - page didn't change as expected");
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Login execution interrupted: {}", e.getMessage());
                    logs.add("Login execution interrupted: " + e.getMessage());
                    throw new RuntimeException("Login execution interrupted", e);
                } catch (Exception e) {
                    log.error("Error during login execution: {}", e.getMessage());
                    logs.add("Error during login: " + e.getMessage());
                }
            }

            if (!loginExecuted) {
                logs.add("Could not complete login - missing elements or login failed");
                log.warn("Login test could not be completed. Username field: {}, Password field: {}, Submit button: {}",
                    userElement != null, passElement != null, submitElement != null);
            } else {
                logs.add("Login test completed successfully");
                log.info("Login test executed successfully");
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Login test interrupted: {}", e.getMessage());
            logs.add("Login test interrupted: " + e.getMessage());
            throw new RuntimeException("Login test interrupted", e);
        } catch (Exception e) {
            log.error("Login test execution failed", e);
            logs.add("Login test failed with error: " + e.getMessage());
            throw e;
        }
    }

    private String captureScreenshot(String name, String executionId) {
        WebDriver webDriver = threadLocalDriver.get();
        if (webDriver == null) return null;

        try {
            // Wait a moment for any animations/transitions to complete
            Thread.sleep(1000);

            // Create screenshots directory
            Path screenshotDir = Paths.get(reportOutputPath, "screenshots");
            Files.createDirectories(screenshotDir);

            // Generate filename with timestamp and better naming
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("%s_%s.png", name, timestamp);
            File screenshotFile = screenshotDir.resolve(filename).toFile();

            // Verify page is loaded before taking screenshot
            try {
                threadLocalWait.get().until(ExpectedConditions.jsReturnsValue("return document.readyState === 'complete'"));
                Thread.sleep(500); // Additional wait for rendering
            } catch (Exception e) {
                log.warn("Could not verify page readiness before screenshot: {}", e.getMessage());
            }

            // Take screenshot
            TakesScreenshot takesScreenshot = (TakesScreenshot) webDriver;
            File sourceFile = takesScreenshot.getScreenshotAs(OutputType.FILE);
            Files.copy(sourceFile.toPath(), screenshotFile.toPath());

            log.info("Screenshot captured: {} (size: {} bytes)", screenshotFile.getAbsolutePath(), screenshotFile.length());
            return screenshotFile.getAbsolutePath();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Screenshot capture interrupted: {}", e.getMessage());
            return null;
        } catch (IOException e) {
            log.error("Failed to capture screenshot: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Failed to capture screenshot: {}", e.getMessage());
            return null;
        }
    }

    private String getBrowserFromTestData(Map<String, Object> testData, String defaultBrowser) {
        return testData.getOrDefault("browser", defaultBrowser).toString();
    }

    private void cleanupDriver() {
        WebDriver webDriver = threadLocalDriver.get();
        if (webDriver != null) {
            try {
                webDriver.quit();
                log.info("WebDriver cleaned up successfully");
            } catch (Exception e) {
                log.warn("Error during WebDriver cleanup: {}", e.getMessage());
            } finally {
                threadLocalDriver.remove();
                threadLocalWait.remove();
            }
        }
    }

    /**
     * Enhanced driver initialization with better thread management
     */
    private void initializeDriverForThread(TestExecutionConfig config, String executionId) {
        try {
            log.info("Initializing WebDriver for execution {} with browser: {}", executionId, config.getBrowser());

            // Enhanced system properties for better stability
            configureSystemProperties();

            WebDriver webDriver = createWebDriver(config);
            threadLocalDriver.set(webDriver);
            driverPool.put(executionId, webDriver);
            driverLastUsed.put(executionId, LocalDateTime.now());

            // Configure timeouts
            webDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(implicitWait));
            webDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(config.getTimeout()));
            webDriver.manage().timeouts().scriptTimeout(Duration.ofSeconds(30));

            if (!config.isHeadless() && !isServerEnvironment()) {
                webDriver.manage().window().maximize();
            }

            threadLocalWait.set(new WebDriverWait(webDriver, Duration.ofSeconds(config.getTimeout())));
            log.info("WebDriver initialized successfully for execution: {}", executionId);

        } catch (Exception e) {
            log.error("Failed to initialize WebDriver for execution {}: {}", executionId, e.getMessage(), e);
            throw new RuntimeException("WebDriver initialization failed: " + e.getMessage(), e);
        }
    }

    /**
     * Backward compatibility method
     */
    private void initializeDriverForThread(String browser, String executionId) {
        TestExecutionConfig config = new TestExecutionConfig();
        config.setBrowser(browser);
        config.setHeadless(headlessMode);
        config.setEnableScreenshots(captureScreenshots);
        initializeDriverForThread(config, executionId);
    }

    /**
     * Enhanced cleanup with proper resource management
     */
    private void cleanupDriverForThread(String executionId) {
        try {
            WebDriver webDriver = threadLocalDriver.get();
            if (webDriver != null) {
                webDriver.quit();
                log.info("WebDriver cleaned up for execution: {}", executionId);
            }
        } catch (Exception e) {
            log.warn("Error during WebDriver cleanup for execution {}: {}", executionId, e.getMessage());
        } finally {
            threadLocalDriver.remove();
            threadLocalWait.remove();
            driverPool.remove(executionId);
            driverLastUsed.remove(executionId);
        }
    }

    /**
     * Configure system properties for better WebDriver stability
     */
    private void configureSystemProperties() {
        System.setProperty("webdriver.chrome.silentOutput", "true");
        System.setProperty("webdriver.chrome.logfile", "/dev/null");
        System.setProperty("webdriver.chrome.verboseLogging", "false");
        System.setProperty("webdriver.chrome.args", "--silent --log-level=3");
        System.setProperty("org.openqa.selenium.logging.ignore", "org.openqa.selenium.devtools");
        System.setProperty("selenium.LOGGER", "OFF");

        // Disable specific Selenium logging
        java.util.logging.Logger.getLogger("org.openqa.selenium").setLevel(java.util.logging.Level.OFF);
        java.util.logging.Logger.getLogger("org.openqa.selenium.devtools").setLevel(java.util.logging.Level.OFF);
        java.util.logging.Logger.getLogger("org.openqa.selenium.remote").setLevel(java.util.logging.Level.OFF);
    }

    /**
     * Create WebDriver instance based on configuration
     */
    private WebDriver createWebDriver(TestExecutionConfig config) {
        String browser = config.getBrowser().toLowerCase();
        boolean headless = config.isHeadless() || isServerEnvironment();

        switch (browser) {
            case "firefox":
                return createFirefoxDriver(headless);
            case "chrome":
            default:
                return createChromeDriver(headless);
        }
    }

    /**
     * Create Chrome WebDriver with options
     */
    private WebDriver createChromeDriver(boolean headless) {
        ChromeOptions chromeOptions = new ChromeOptions();

        // Basic Chrome options
        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--disable-dev-shm-usage");
        chromeOptions.addArguments("--disable-gpu");
        chromeOptions.addArguments("--disable-extensions");
        chromeOptions.addArguments("--disable-web-security");
        chromeOptions.addArguments("--allow-running-insecure-content");
        chromeOptions.addArguments("--window-size=1920,1080");

        // Headless configuration
        if (headless) {
            chromeOptions.addArguments("--headless=new");
            chromeOptions.addArguments("--disable-gpu");
        }

        // Suppress logging and automation detection
        chromeOptions.addArguments("--disable-logging");
        chromeOptions.addArguments("--log-level=3");
        chromeOptions.addArguments("--silent");
        chromeOptions.setExperimentalOption("useAutomationExtension", false);
        chromeOptions.setExperimentalOption("excludeSwitches", Arrays.asList("enable-automation", "enable-logging"));

        try {
            if (!isChromeAvailable()) {
                chromeOptions.setBinary(chromiumPath);
            }

            WebDriverManager.chromedriver().setup();
            return new ChromeDriver(chromeOptions);
        } catch (Exception e) {
            log.error("Failed to create Chrome WebDriver: {}", e.getMessage());
            throw new RuntimeException("Chrome WebDriver creation failed", e);
        }
    }

    /**
     * Create Firefox WebDriver with options
     */
    private WebDriver createFirefoxDriver(boolean headless) {
        FirefoxOptions firefoxOptions = new FirefoxOptions();

        if (headless) {
            firefoxOptions.addArguments("--headless");
        }

        firefoxOptions.addPreference("dom.webdriver.enabled", false);
        firefoxOptions.addPreference("useAutomationExtension", false);

        try {
            WebDriverManager.firefoxdriver().setup();
            return new FirefoxDriver(firefoxOptions);
        } catch (Exception e) {
            log.error("Failed to create Firefox WebDriver: {}", e.getMessage());
            throw new RuntimeException("Firefox WebDriver creation failed", e);
        }
    }
}
