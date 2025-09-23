package org.example.engine;

import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Parameters;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base class for UI tests with WebDriver setup and utilities
 */
public class BaseUITest {

    protected WebDriver driver;
    protected Logger testLogger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private WebDriverManager webDriverManager;

    private boolean isHeadlessEnv() {
        // CI env variable or system property/headless hints
        if (System.getenv() != null) return true;
        if ("true".equalsIgnoreCase(System.getenv("CI_HEADLESS"))) return true;
        if (Boolean.getBoolean("headless")) return true;
        return false;
    }

    @BeforeMethod
    @Parameters({"browser"})
    public void setUp(String browser) {
        boolean headless = isHeadlessEnv();
        String resolvedBrowser = browser != null ? browser : "chrome";
        try {
            if (webDriverManager != null) {
                driver = webDriverManager.createDriver(resolvedBrowser, headless);
            } else {
                driver = createFallbackDriver(headless);
            }
            testLogger.info("Starting UI test: {} with browser: {} (headless: {})", this.getClass().getSimpleName(), resolvedBrowser, headless);
        } catch (Exception e) {
            testLogger.error("Failed to set up WebDriver (primary path)", e);
            driver = createFallbackDriver(headless);
        }
    }

    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            try {
                driver.quit();
                testLogger.info("Completed UI test: {}", this.getClass().getSimpleName());
            } catch (Exception e) {
                testLogger.error("Error during WebDriver cleanup", e);
            }
        }
    }

    private WebDriver createFallbackDriver(boolean headless) {
        try {
            testLogger.warn("Using fallback WebDriver creation (headless: {})", headless);
            return new WebDriverManager().createDriver("chrome", headless);
        } catch (Exception e) {
            testLogger.error("Failed to create fallback driver", e);
            throw new RuntimeException("Cannot create WebDriver instance", e);
        }
    }
}
