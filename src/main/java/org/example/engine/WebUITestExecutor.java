package org.example.engine;

import org.example.model.TestCase;
import org.example.model.TestExecution;
import org.example.utils.ScreenshotUtils;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class WebUITestExecutor {

    private static final Logger log = LoggerFactory.getLogger(WebUITestExecutor.class);

    @Autowired
    private WebDriverManager webDriverManager;

    @Autowired
    private ScreenshotUtils screenshotUtils;

    public TestExecution executeWebUITest(TestCase testCase, String environment, String browser) {
        TestExecution execution = new TestExecution();
        execution.setTestCase(testCase);
        execution.setExecutionId(UUID.randomUUID().toString());
        execution.setEnvironment(environment);
        execution.setBrowser(browser);
        execution.setStartTime(LocalDateTime.now());
        execution.setStatus(TestExecution.ExecutionStatus.RUNNING);

        WebDriver driver = null;

        try {
            log.info("Starting web UI test execution for test case: {}", testCase.getName());

            // Create WebDriver instance
            driver = webDriverManager.createDriver(browser, false);

            // Execute the test logic based on test data
            executeTestSteps(driver, testCase, execution);

            execution.setStatus(TestExecution.ExecutionStatus.PASSED);
            log.info("Web UI test passed: {}", testCase.getName());

        } catch (AssertionError e) {
            execution.setStatus(TestExecution.ExecutionStatus.FAILED);
            execution.setErrorMessage(e.getMessage());
            execution.setStackTrace(getStackTrace(e));
            log.error("Web UI test failed: {}", testCase.getName(), e);

            // Capture screenshot on failure
            if (driver != null) {
                String screenshotPath = screenshotUtils.captureScreenshot(driver, execution.getExecutionId());
                execution.setScreenshotPath(screenshotPath);
            }

        } catch (Exception e) {
            execution.setStatus(TestExecution.ExecutionStatus.ERROR);
            execution.setErrorMessage(e.getMessage());
            execution.setStackTrace(getStackTrace(e));
            log.error("Web UI test error: {}", testCase.getName(), e);

            // Capture screenshot on error
            if (driver != null) {
                String screenshotPath = screenshotUtils.captureScreenshot(driver, execution.getExecutionId());
                execution.setScreenshotPath(screenshotPath);
            }

        } finally {
            execution.setEndTime(LocalDateTime.now());
            if (execution.getStartTime() != null && execution.getEndTime() != null) {
                long duration = java.time.Duration.between(execution.getStartTime(), execution.getEndTime()).toMillis();
                execution.setExecutionDuration(duration);
            }

            // Cleanup driver
            webDriverManager.quitDriver();
        }

        return execution;
    }

    private void executeTestSteps(WebDriver driver, TestCase testCase, TestExecution execution) {
        // Parse test data (assuming JSON format)
        // This is a simplified implementation - in practice, you'd have a more sophisticated test step parser
        try {
            // Example: Navigate to URL
            String testData = testCase.getTestData();
            if (testData.contains("\"url\"")) {
                String url = extractValue(testData, "url");
                driver.get(url);
                log.info("Navigated to URL: {}", url);
            }

            // Add more test step implementations based on your requirements
            // For example: element interactions, validations, etc.

            // Verify expected result
            String expectedResult = testCase.getExpectedResult();
            String actualResult = driver.getTitle(); // Simplified - get page title
            execution.setActualResult(actualResult);

            if (!actualResult.contains(expectedResult)) {
                throw new AssertionError("Expected result not found. Expected: " + expectedResult + ", Actual: " + actualResult);
            }

        } catch (Exception e) {
            throw new RuntimeException("Test step execution failed", e);
        }
    }

    private String extractValue(String json, String key) {
        // Simplified JSON parsing - in practice, use Jackson or similar
        String searchKey = "\"" + key + "\":\"";
        int startIndex = json.indexOf(searchKey);
        if (startIndex != -1) {
            startIndex += searchKey.length();
            int endIndex = json.indexOf("\"", startIndex);
            if (endIndex != -1) {
                return json.substring(startIndex, endIndex);
            }
        }
        return "";
    }

    private String getStackTrace(Throwable e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
