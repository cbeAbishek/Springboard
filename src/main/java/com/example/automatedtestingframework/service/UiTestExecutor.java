package com.example.automatedtestingframework.service;

import com.example.automatedtestingframework.model.Report;
import com.example.automatedtestingframework.model.TestCase;
import com.example.automatedtestingframework.repository.ReportRepository;
import com.example.automatedtestingframework.repository.TestCaseRepository;
import com.example.automatedtestingframework.util.JsonParserUtil;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.InvalidElementStateException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UiTestExecutor {

    private static final Logger log = LoggerFactory.getLogger(UiTestExecutor.class);

    private final JsonParserUtil jsonParserUtil;
    private final ScreenshotService screenshotService;
    private final ReportRepository reportRepository;
    private final TestCaseRepository testCaseRepository;

    public UiTestExecutor(JsonParserUtil jsonParserUtil,
                          ScreenshotService screenshotService,
                          ReportRepository reportRepository,
                          TestCaseRepository testCaseRepository) {
        this.jsonParserUtil = jsonParserUtil;
        this.screenshotService = screenshotService;
        this.reportRepository = reportRepository;
        this.testCaseRepository = testCaseRepository;
        String cachePath = resolveWebDriverCachePath();
        try {
            Files.createDirectories(Path.of(cachePath));
        } catch (IOException e) {
            log.warn("Failed to create WebDriver cache directory {}: {}", cachePath, e.getMessage());
        }
        WebDriverManager.chromedriver()
            .cachePath(cachePath)
            .avoidBrowserDetection()
            .setup();
    }

    public Report execute(TestCase testCase) {
        JsonNode definition = jsonParserUtil.parse(testCase.getDefinitionJson());
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-blink-features=AutomationControlled");
        Optional.ofNullable(System.getenv("CHROME_BIN"))
            .filter(bin -> !bin.isBlank())
            .ifPresent(options::setBinary);
        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));

        OffsetDateTime start = OffsetDateTime.now();
        StringBuilder details = new StringBuilder();
        String status = "PASSED";
        String errorMessage = null;
        String screenshotUrl = null;

        try {
            for (JsonNode step : definition.withArray("steps")) {
                String action = step.path("action").asText();
                switch (action) {
                    case "navigate" -> {
                        String url = step.path("url").asText();
                        driver.get(url);
                        // Wait for page to be ready
                        new WebDriverWait(driver, Duration.ofSeconds(10))
                            .until(d -> ((org.openqa.selenium.JavascriptExecutor) d)
                                .executeScript("return document.readyState").equals("complete"));
                        details.append("Navigated to ").append(url).append('\n');
                    }
                    case "click" -> {
                        WebElement element = findElement(driver, step);
                        // Scroll element into view
                        ((org.openqa.selenium.JavascriptExecutor) driver)
                            .executeScript("arguments[0].scrollIntoView(true);", element);
                        Thread.sleep(300); // Brief pause after scroll
                        element.click();
                        details.append("Clicked element: ").append(step.path("selector").asText()).append('\n');
                    }
                    case "type" -> {
                        WebElement element = findElement(driver, step);
                        if (!element.isEnabled()) {
                            throw new IllegalStateException("Element is disabled and cannot be typed into: " + step.path("selector").asText());
                        }

                        String text = step.path("text").asText(step.path("value").asText(""));

                        if ("select".equalsIgnoreCase(element.getTagName())) {
                            Select select = new Select(element);
                            try {
                                select.selectByVisibleText(text);
                            } catch (Exception ex) {
                                selectByValueFallback(select, text);
                            }
                            details.append("Selected option in: ").append(step.path("selector").asText())
                                .append(" -> ").append(text).append('\n');
                            break;
                        }

                        if (Boolean.parseBoolean(step.path("useJavascript").asText("false"))) {
                            setValueWithJavascript(driver, element, text);
                        } else {
                            try {
                                element.clear();
                            } catch (InvalidElementStateException e) {
                                log.debug("Element clear failed, falling back to JS for selector {}", step.path("selector").asText());
                                setValueWithJavascript(driver, element, "");
                            }
                            element.sendKeys(text);
                        }

                        details.append("Typed text into: ").append(step.path("selector").asText()).append('\n');
                    }
                    case "assertTitle" -> {
                        String expected = step.path("value").asText();
                        if (!driver.getTitle().contains(expected)) {
                            throw new IllegalStateException("Expected title to contain %s but was %s".formatted(expected, driver.getTitle()));
                        }
                        details.append("Asserted title contains ").append(expected).append('\n');
                    }
                    case "wait" -> {
                        long millis = step.path("millis").asLong(500);
                        try {
                            Thread.sleep(millis);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException("Wait interrupted", e);
                        }
                        details.append("Waited for ").append(millis).append("ms").append('\n');
                    }
                    default -> details.append("Skipped unknown action: ").append(action).append('\n');
                }
            }
        } catch (Exception ex) {
            status = "FAILED";
            errorMessage = ex.getMessage();
            log.error("UI test execution failed", ex);
            screenshotUrl = captureAndUpload(driver, testCase);
        } finally {
            driver.quit();
        }

        Report report = new Report();
        report.setProject(testCase.getProject());
        report.setTestCase(testCase);
        report.setStartedAt(start);
        report.setCompletedAt(OffsetDateTime.now());
        report.setStatus(status);
        report.setDetails(details.toString());
        report.setSummary("UI test execution for %s".formatted(testCase.getName()));
        report.setErrorMessage(errorMessage);
        report.setScreenshotUrl(screenshotUrl);

        Report saved = reportRepository.save(report);
        updateTestCaseLastRun(testCase, status, errorMessage);
        return saved;
    }

    private WebElement findElement(WebDriver driver, JsonNode step) {
        String by = step.path("by").asText("css");
        String selector = step.path("selector").asText();
        long timeoutMs = step.path("timeoutMs").asLong(10000);
        if (timeoutMs < 500) {
            timeoutMs = 500;
        }
        Duration timeout = Duration.ofMillis(timeoutMs);

        WebDriverWait wait = new WebDriverWait(driver, timeout);

        try {
            By locator = switch (by) {
                case "id" -> By.id(selector);
                case "name" -> By.name(selector);
                case "xpath" -> By.xpath(selector);
                case "css" -> By.cssSelector(selector);
                default -> By.cssSelector(selector);
            };

            // Wait for element to be present and visible
            return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        } catch (Exception e) {
            // Log page source for debugging
            try {
                String pageSource = driver.getPageSource();
                log.error("Element not found with {} selector: {}. Current URL: {}. Page title: {}",
                    by, selector, driver.getCurrentUrl(), driver.getTitle());
                log.debug("Page source when element not found: {}", pageSource.substring(0, Math.min(500, pageSource.length())));
            } catch (Exception debugEx) {
                log.warn("Could not retrieve page details for debugging", debugEx);
            }

            throw new IllegalStateException(
                String.format("Element not found: %s (by: %s). URL: %s. Make sure the selector matches an element on the page.",
                    selector, by, driver.getCurrentUrl())
            );
        }
    }

    private String captureAndUpload(WebDriver driver, TestCase testCase) {
        try {
            byte[] bytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            String url = screenshotService.uploadScreenshot(bytes, "ui-failure-" + testCase.getId() + "-" + System.currentTimeMillis() + ".png");
            if (url != null) {
                log.info("Screenshot uploaded successfully: {}", url);
            }
            return url;
        } catch (Exception ex) {
            log.error("Failed to capture screenshot", ex);
            return null;
        }
    }

    private void selectByValueFallback(Select select, String value) {
        List<WebElement> options = select.getOptions();
        for (WebElement option : options) {
            if (value.equalsIgnoreCase(option.getAttribute("value")) || value.equalsIgnoreCase(option.getText())) {
                select.selectByValue(option.getAttribute("value"));
                return;
            }
        }
        throw new IllegalStateException("Option '" + value + "' not found for select element");
    }

    private void setValueWithJavascript(WebDriver driver, WebElement element, String value) {
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].value = arguments[1];" +
                "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
                "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
            element, value
        );
    }

    private void updateTestCaseLastRun(TestCase testCase, String status, String error) {
        testCase.setLastRunAt(OffsetDateTime.now());
        testCase.setLastRunStatus(status);
        testCase.setLastErrorMessage(error);
        testCaseRepository.save(testCase);
    }

    private String resolveWebDriverCachePath() {
        String property = System.getProperty("wdm.cachePath");
        if (property != null && !property.isBlank()) {
            return property;
        }
        String env = System.getenv("WDM_CACHE_PATH");
        if (env != null && !env.isBlank()) {
            return env;
        }
        return "/tmp/webdriver";
    }
}
