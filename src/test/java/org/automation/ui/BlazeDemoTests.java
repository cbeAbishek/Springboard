package org.automation.ui;

import org.automation.listeners.TestSuiteListener;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.time.Duration;

@Listeners(TestSuiteListener.class)
public class BlazeDemoTests extends BaseTest {

    private static final int DEFAULT_WAIT_SECONDS = 10;

    @Test(description = "Verify Home Page Title")
    public void testHomePageTitle() {
        Reporter.getCurrentTestResult().setAttribute("US_ID", "US101");
        getDriver().get(UiTestMapper.HOME_URL);
        Assert.assertEquals(getDriver().getTitle(), "BlazeDemo");

//        //Intentionally change the expected value to something incorrect:
//        Assert.assertEquals(getDriver().getTitle(), "WrongTitle");
    }

    @Test(description = "Verify Departure and Destination fields exist on Home Page")
    public void testHomePageContainsDepartureAndDestination() {
        Reporter.getCurrentTestResult().setAttribute("US_ID", "US102");
        getDriver().get(UiTestMapper.HOME_URL);
        Assert.assertTrue(getDriver().findElement(By.name("fromPort")).isDisplayed());
        Assert.assertTrue(getDriver().findElement(By.name("toPort")).isDisplayed());
    }

    @Test(description = "Verify Flight Search Navigation from Home Page")
    public void testFlightSearchNavigation() {
        Reporter.getCurrentTestResult().setAttribute("US_ID", "US103");
        getDriver().get(UiTestMapper.HOME_URL);

        // Wait for page to load
        WebDriverWait wait = new WebDriverWait(getDriver(), Duration.ofSeconds(DEFAULT_WAIT_SECONDS));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("fromPort")));

        getDriver().findElement(By.name("fromPort")).sendKeys("Boston");
        getDriver().findElement(By.name("toPort")).sendKeys("New York");
        getDriver().findElement(By.cssSelector("input[type='submit']")).click();
        Assert.assertTrue(getDriver().getTitle().contains("BlazeDemo"));
    }

    @Test(description = "Select First Flight from Flights Page")
    public void testSelectFirstFlight() {
        Reporter.getCurrentTestResult().setAttribute("US_ID", "US104");
        getDriver().get(UiTestMapper.FLIGHTS_URL);
        getDriver().findElement(By.cssSelector("input[type='submit']")).click(); // select first
        Assert.assertTrue(getDriver().getTitle().contains("BlazeDemo"));
    }

    @Test(description = "Verify Purchase Page Contains Form")
    public void testPurchasePageContainsForm() {
        Reporter.getCurrentTestResult().setAttribute("US_ID", "US105");
        getDriver().get(UiTestMapper.PURCHASE_URL);
        Assert.assertTrue(getDriver().findElement(By.id("inputName")).isDisplayed());
    }

    @Test(description = "Fill Purchase Form")
    public void testFillPurchaseForm() {
        Reporter.getCurrentTestResult().setAttribute("US_ID", "US106");
        getDriver().get(UiTestMapper.PURCHASE_URL);
        getDriver().findElement(By.id("inputName")).sendKeys("Test User");
        getDriver().findElement(By.id("address")).sendKeys("123 Test St");
        getDriver().findElement(By.id("city")).sendKeys("Test City");
        getDriver().findElement(By.id("state")).sendKeys("Test State");
        getDriver().findElement(By.id("zipCode")).sendKeys("12345");
        getDriver().findElement(By.id("creditCardNumber")).sendKeys("4111111111111111");
        getDriver().findElement(By.id("creditCardMonth")).sendKeys("12");
        getDriver().findElement(By.id("creditCardYear")).sendKeys("2025");
        getDriver().findElement(By.id("nameOnCard")).sendKeys("Test User");
        getDriver().findElement(By.cssSelector("input[type='submit']")).click();

        // Wait for page to load
        WebDriverWait wait = new WebDriverWait(getDriver(), Duration.ofSeconds(DEFAULT_WAIT_SECONDS));
        wait.until(ExpectedConditions.titleContains("Confirmation"));

        Assert.assertTrue(getDriver().getTitle().contains("BlazeDemo Confirmation"));
    }

    @Test(description = "Purchase Flight")
    public void testPurchaseFlight() {
        Reporter.getCurrentTestResult().setAttribute("US_ID", "US107");
        getDriver().get(UiTestMapper.PURCHASE_URL);

        // Wait for form to be ready
        WebDriverWait wait = new WebDriverWait(getDriver(), Duration.ofSeconds(DEFAULT_WAIT_SECONDS));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("inputName")));

        getDriver().findElement(By.id("inputName")).sendKeys("Test User");
        getDriver().findElement(By.id("address")).sendKeys("123 Test St");
        getDriver().findElement(By.id("city")).sendKeys("Test City");
        getDriver().findElement(By.id("state")).sendKeys("Test State");
        getDriver().findElement(By.id("zipCode")).sendKeys("12345");
        getDriver().findElement(By.id("creditCardNumber")).sendKeys("4111111111111111");
        getDriver().findElement(By.id("creditCardMonth")).sendKeys("12");
        getDriver().findElement(By.id("creditCardYear")).sendKeys("2025");
        getDriver().findElement(By.id("nameOnCard")).sendKeys("Test User");
        getDriver().findElement(By.cssSelector("input[type='submit']")).click();

        // Wait for confirmation page to load
        wait.until(ExpectedConditions.titleContains("Confirmation"));

        Assert.assertTrue(getDriver().getTitle().contains("Confirmation"),
            "Expected title to contain 'Confirmation' but was: " + getDriver().getTitle());
    }

    @Test(description = "Verify Confirmation Page")
    public void testConfirmationPage() {
        Reporter.getCurrentTestResult().setAttribute("US_ID", "US108");
        getDriver().get(UiTestMapper.CONFIRMATION_URL);
        Assert.assertTrue(getDriver().getPageSource().contains("Thank you for your purchase"));
    }

    @Test(description = "Navigate to Home Page")
    public void testNavigateToHomePage() {
        Reporter.getCurrentTestResult().setAttribute("US_ID", "US109");
        getDriver().get(UiTestMapper.HOME_URL);
        Assert.assertEquals(getDriver().getTitle(), "BlazeDemo");
    }

    @Test(description = "Verify Flight Selection Page UI Elements")
    public void testFlightSelectionPage() {
        Reporter.getCurrentTestResult().setAttribute("US_ID", "US110");
        getDriver().get(UiTestMapper.FLIGHTS_URL);
        Assert.assertTrue(getDriver().findElement(By.cssSelector("input[type='submit']")).isDisplayed());
    }
}
