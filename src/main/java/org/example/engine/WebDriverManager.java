package org.example.engine;

import static io.github.bonigarcia.wdm.WebDriverManager.*;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.io.File;

@Component
public class WebDriverManager {

    private static final Logger log = LoggerFactory.getLogger(WebDriverManager.class);
    private final ConcurrentHashMap<String, WebDriver> driverInstances = new ConcurrentHashMap<>();

    public WebDriver createDriver(String browserType, boolean headless) {
        String threadId = Thread.currentThread().getName();
        WebDriver driver = null;

        try {
            // Comprehensive CDP and logging suppression
            System.setProperty("webdriver.chrome.silentOutput", "true");
            System.setProperty("webdriver.chrome.logfile", "/dev/null");
            System.setProperty("webdriver.chrome.verboseLogging", "false");
            System.setProperty("webdriver.chrome.args", "--silent --log-level=3");
            System.setProperty("org.openqa.selenium.logging.ignore", "org.openqa.selenium.devtools");
            System.setProperty("selenium.LOGGER", "OFF");

            // Disable Java logging for Selenium
            java.util.logging.Logger.getLogger("org.openqa.selenium").setLevel(java.util.logging.Level.OFF);
            java.util.logging.Logger.getLogger("org.openqa.selenium.devtools").setLevel(java.util.logging.Level.OFF);
            java.util.logging.Logger.getLogger("org.openqa.selenium.remote").setLevel(java.util.logging.Level.OFF);

            switch (browserType.toLowerCase()) {
                case "chrome":
                    chromedriver().setup();
                    ChromeOptions chromeOptions = new ChromeOptions();
                    // Ubuntu-specific Chrome configuration
                    String chromeBinary = "/usr/bin/google-chrome";
                    if (new File(chromeBinary).exists()) {
                        chromeOptions.setBinary(chromeBinary);
                    }

                    if (headless) {
                        chromeOptions.addArguments("--headless=new");
                    }

                    // Essential arguments for Ubuntu/Linux
                    chromeOptions.addArguments("--no-sandbox");
                    chromeOptions.addArguments("--disable-dev-shm-usage");
                    chromeOptions.addArguments("--disable-gpu");
                    chromeOptions.addArguments("--disable-extensions");
                    chromeOptions.addArguments("--disable-web-security");
                    chromeOptions.addArguments("--allow-running-insecure-content");
                    chromeOptions.addArguments("--disable-features=VizDisplayCompositor");
                    chromeOptions.addArguments("--remote-debugging-port=9222");
                    chromeOptions.addArguments("--window-size=1920,1080");

                    // Complete CDP and DevTools suppression (enhanced)
                    chromeOptions.addArguments("--disable-dev-tools");
                    chromeOptions.addArguments("--disable-logging");
                    chromeOptions.addArguments("--disable-background-networking");
                    chromeOptions.addArguments("--disable-default-apps");
                    chromeOptions.addArguments("--disable-sync");
                    chromeOptions.addArguments("--no-first-run");
                    chromeOptions.addArguments("--disable-features=TranslateUI");
                    chromeOptions.addArguments("--disable-extensions-http-throttling");
                    chromeOptions.addArguments("--disable-client-side-phishing-detection");
                    chromeOptions.addArguments("--disable-blink-features=AutomationControlled");
                    chromeOptions.addArguments("--disable-infobars");
                    chromeOptions.addArguments("--disable-notifications");
                    chromeOptions.addArguments("--disable-popup-blocking");
                    chromeOptions.addArguments("--disable-translate");
                    chromeOptions.addArguments("--disable-ipc-flooding-protection");

                    // Advanced logging suppression
                    chromeOptions.addArguments("--log-level=3");
                    chromeOptions.addArguments("--silent");
                    chromeOptions.addArguments("--disable-gpu-logging");

                    // Experimental options to completely disable automation detection and CDP
                    chromeOptions.setExperimentalOption("useAutomationExtension", false);
                    chromeOptions.setExperimentalOption("excludeSwitches",
                        java.util.Arrays.asList("enable-automation", "enable-logging"));
                    chromeOptions.setExperimentalOption("detach", true);

                    // Disable all Chrome logging via preferences
                    java.util.Map<String, Object> prefs = new java.util.HashMap<>();
                    prefs.put("profile.default_content_setting_values.notifications", 2);
                    prefs.put("profile.default_content_settings.popups", 0);
                    prefs.put("profile.managed_default_content_settings.images", 2);
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

                    // Set display for headless operation
                    if (System.getenv("DISPLAY") == null) {
                        chromeOptions.addArguments("--display=:99");
                    }

                    driver = new ChromeDriver(chromeOptions);
                    break;

                case "firefox":
                    firefoxdriver().setup();
                    FirefoxOptions firefoxOptions = new FirefoxOptions();
                    if (headless) {
                        firefoxOptions.addArguments("--headless");
                    }
                    // Add Ubuntu-specific Firefox options
                    firefoxOptions.addArguments("--no-sandbox");
                    firefoxOptions.addArguments("--disable-dev-shm-usage");
                    driver = new FirefoxDriver(firefoxOptions);
                    break;

                case "edge":
                    edgedriver().setup();
                    EdgeOptions edgeOptions = new EdgeOptions();
                    if (headless) {
                        edgeOptions.addArguments("--headless");
                    }
                    edgeOptions.addArguments("--no-sandbox");
                    edgeOptions.addArguments("--disable-dev-shm-usage");
                    driver = new EdgeDriver(edgeOptions);
                    break;

                default:
                    throw new IllegalArgumentException("Unsupported browser: " + browserType);
            }

            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));

            // Only maximize if not headless
            if (!headless) {
                driver.manage().window().maximize();
            }

            driverInstances.put(threadId, driver);
            log.info("Created {} driver for thread {} (headless: {})", browserType, threadId, headless);

        } catch (Exception e) {
            log.error("Failed to create {} driver: {}", browserType, e.getMessage(), e);
            throw new RuntimeException("Driver creation failed for " + browserType, e);
        }

        return driver;
    }

    public WebDriver getDriver() {
        String threadId = Thread.currentThread().getName();
        return driverInstances.get(threadId);
    }

    public void quitDriver() {
        String threadId = Thread.currentThread().getName();
        WebDriver driver = driverInstances.get(threadId);
        if (driver != null) {
            try {
                driver.quit();
                driverInstances.remove(threadId);
                log.info("Quit driver for thread {}", threadId);
            } catch (Exception e) {
                log.error("Error quitting driver: {}", e.getMessage());
            }
        }
    }

    public void quitAllDrivers() {
        driverInstances.forEach((threadId, driver) -> {
            try {
                driver.quit();
                log.info("Quit driver for thread {}", threadId);
            } catch (Exception e) {
                log.error("Error quitting driver for thread {}: {}", threadId, e.getMessage());
            }
        });
        driverInstances.clear();
    }
}
