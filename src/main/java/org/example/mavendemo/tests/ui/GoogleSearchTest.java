package org.example.mavendemo.tests.ui;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Sample Selenium UI test for Google Search functionality.
 * This test opens Google homepage, searches for 'Spring Boot Testing',
 * and validates that the page title contains 'Spring Boot'.
 */
@Component
public class GoogleSearchTest {

    private static final String GOOGLE_URL = "https://www.google.com";
    private static final String SEARCH_TERM = "Spring Boot Testing";
    private static final String EXPECTED_TITLE_PART = "Spring Boot";
    private static final int TIMEOUT_SECONDS = 10;

    /**
     * Executes the Google search test.
     * 
     * @return true if test passes, false if test fails
     */
    public boolean executeTest() {
        WebDriver driver = null;
        try {
            // Set up Chrome driver with options
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless"); // Run in headless mode for CI/CD
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-gpu");
            options.addArguments("--window-size=1920,1080");

            driver = new ChromeDriver(options);
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(TIMEOUT_SECONDS));

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(TIMEOUT_SECONDS));

            // Step 1: Open Google homepage
            driver.get(GOOGLE_URL);

            // Accept cookies if present
            try {
                WebElement acceptButton = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//button[contains(text(), 'Accept all') or contains(text(), 'I agree')]")));
                acceptButton.click();
            } catch (Exception e) {
                // Cookie banner might not appear, continue with test
            }

            // Step 2: Find search box and perform search
            WebElement searchBox = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.name("q")));
            searchBox.clear();
            searchBox.sendKeys(SEARCH_TERM);
            searchBox.submit();

            // Step 3: Wait for search results page to load
            wait.until(ExpectedConditions.titleContains(SEARCH_TERM));

            // Step 4: Validate page title contains expected text
            String pageTitle = driver.getTitle();
            boolean testResult = pageTitle.toLowerCase().contains(EXPECTED_TITLE_PART.toLowerCase());

            return testResult;

        } catch (Exception e) {
            System.err.println("Google Search Test failed with exception: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            // Clean up: close browser
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception e) {
                    System.err.println("Error closing driver: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Get test details
     * 
     * @return String description of the test
     */
    public String getTestDescription() {
        return "Google Search Test - Searches for '" + SEARCH_TERM +
                "' and validates page title contains '" + EXPECTED_TITLE_PART + "'";
    }
}
