package org.example.mavendemo.service;

import org.example.mavendemo.model.TestCase;
import org.example.mavendemo.model.TestResult;
import org.example.mavendemo.repository.TestCaseRepository;
import org.example.mavendemo.repository.TestResultRepository;
import org.example.mavendemo.tests.api.GitHubApiTest;
import org.example.mavendemo.tests.ui.GoogleSearchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service responsible for executing test cases and saving results.
 * Supports both UI (Selenium) and API (REST-Assured) test execution.
 */
@Service
public class TestExecutionService {

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private TestResultRepository testResultRepository;

    @Autowired
    private GoogleSearchTest googleSearchTest;

    @Autowired
    private GitHubApiTest gitHubApiTest;

    // Test type constants
    private static final String UI_TEST_TYPE = "UI";
    private static final String API_TEST_TYPE = "API";

    // Test status constants
    private static final String STATUS_PASSED = "PASSED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String TESTCASE_STATUS_EXECUTED = "Executed";

    /**
     * Executes a test case by ID and saves the result.
     * 
     * @param testCaseId The ID of the test case to execute
     * @return TestResult containing execution details
     * @throws RuntimeException if test case not found or execution fails
     */
    public TestResult executeTest(Long testCaseId) {
        return executeTestCase(testCaseId);
    }

    /**
     * Executes a test case by ID and saves the result.
     * 
     * @param testCaseId The ID of the test case to execute
     * @return TestResult containing execution details
     * @throws RuntimeException if test case not found or execution fails
     */
    public TestResult executeTestCase(Long testCaseId) {
        // Step 1: Fetch TestCase by ID from database
        Optional<TestCase> testCaseOptional = testCaseRepository.findById(testCaseId);
        if (testCaseOptional.isEmpty()) {
            throw new RuntimeException("Test case with ID " + testCaseId + " not found");
        }

        TestCase testCase = testCaseOptional.get();

        // Record start time
        LocalDateTime executionStartTime = LocalDateTime.now();
        long startTimeMillis = System.currentTimeMillis();

        // Step 2: Execute test based on type
        boolean testPassed;
        String notes;
        String testDescription;

        try {
            if (UI_TEST_TYPE.equalsIgnoreCase(testCase.getType())) {
                // Execute UI test (Google Search Test)
                testPassed = googleSearchTest.executeTest();
                testDescription = googleSearchTest.getTestDescription();
                notes = testPassed ? "UI test completed successfully" : "UI test failed";

            } else if (API_TEST_TYPE.equalsIgnoreCase(testCase.getType())) {
                // Execute API test (GitHub API Test)
                testPassed = gitHubApiTest.executeTest();
                testDescription = gitHubApiTest.getTestDescription();
                notes = testPassed ? "API test completed successfully" : "API test failed";

            } else {
                throw new RuntimeException("Unsupported test case type: " + testCase.getType() +
                        ". Supported types are: UI, API");
            }

        } catch (Exception e) {
            testPassed = false;
            testDescription = "Test execution failed";
            notes = "Test execution failed with exception: " + e.getMessage();
        }

        // Calculate execution duration
        long endTimeMillis = System.currentTimeMillis();
        long duration = endTimeMillis - startTimeMillis;

        // Step 3: Create and save TestResult
        TestResult testResult = new TestResult();
        testResult.setTestCaseId(testCaseId);
        testResult.setStatus(testPassed ? STATUS_PASSED : STATUS_FAILED);
        testResult.setExecutedAt(executionStartTime);
        testResult.setDuration(duration);
        testResult.setNotes(notes + " | " + testDescription);

        TestResult savedResult = testResultRepository.save(testResult);

        // Step 4: Update TestCase status
        testCase.setStatus(TESTCASE_STATUS_EXECUTED);
        testCaseRepository.save(testCase);

        return savedResult;
    }

    /**
     * Gets the latest test result for a specific test case.
     * 
     * @param testCaseId The ID of the test case
     * @return TestResult if found, null otherwise
     */
    public TestResult getLatestTestResult(Long testCaseId) {
        return testResultRepository.findByTestCaseId(testCaseId)
                .stream()
                .max((r1, r2) -> r1.getExecutedAt().compareTo(r2.getExecutedAt()))
                .orElse(null);
    }

    /**
     * Checks if a test case exists and is ready for execution.
     * 
     * @param testCaseId The ID of the test case to check
     * @return true if test case exists and can be executed
     */
    public boolean isTestCaseExecutable(Long testCaseId) {
        Optional<TestCase> testCase = testCaseRepository.findById(testCaseId);
        if (testCase.isEmpty()) {
            return false;
        }

        String type = testCase.get().getType();
        return UI_TEST_TYPE.equalsIgnoreCase(type) || API_TEST_TYPE.equalsIgnoreCase(type);
    }
}
