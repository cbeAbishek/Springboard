package org.example.tests;

import org.example.engine.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testng.Assert;
import org.testng.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootTest
public class WebDriverTest {

    private static final Logger log = LoggerFactory.getLogger(WebDriverTest.class);

    @Autowired
    private WebDriverManager webDriverManager;

    private WebDriver driver;

    @BeforeMethod
    public void setUp() {
        log.info("Setting up WebDriver for test");
        driver = webDriverManager.createDriver("chrome", true); // headless mode
    }

    @Test(priority = 1)
    public void testWebDriverCreation() {
        log.info("Testing WebDriver creation");
        Assert.assertNotNull(driver, "WebDriver should not be null");
        log.info("WebDriver created successfully");
    }

    @Test(priority = 2)
    public void testBasicNavigation() {
        log.info("Testing basic navigation");
        try {
            driver.get("https://www.google.com");
            String title = driver.getTitle();
            Assert.assertTrue(title.contains("Google"), "Page title should contain 'Google'");
            log.info("Navigation test passed. Page title: {}", title);
        } catch (Exception e) {
            log.error("Navigation test failed", e);
            throw e;
        }
    }

    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            webDriverManager.quitDriver();
            log.info("WebDriver closed successfully");
        }
    }
}
