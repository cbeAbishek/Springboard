package org.example.mavendemo.tests.api;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.springframework.stereotype.Component;

/**
 * Sample REST-Assured API test for GitHub API.
 * This test calls GET https://api.github.com and validates
 * that the response status code is 200.
 */
@Component
public class GitHubApiTest {

    private static final String GITHUB_API_URL = "https://api.github.com";
    private static final int EXPECTED_STATUS_CODE = 200;
    private static final int TIMEOUT_SECONDS = 10;

    /**
     * Executes the GitHub API test.
     * 
     * @return true if test passes, false if test fails
     */
    public boolean executeTest() {
        try {
            // Configure RestAssured
            RestAssured.config = RestAssured.config()
                    .httpClient(RestAssured.config().getHttpClientConfig()
                            .setParam("http.connection.timeout", TIMEOUT_SECONDS * 1000)
                            .setParam("http.socket.timeout", TIMEOUT_SECONDS * 1000));

            // Step 1: Make GET request to GitHub API
            Response response = RestAssured
                    .given()
                    .header("User-Agent", "Spring Boot Test Framework")
                    .header("Accept", "application/vnd.github.v3+json")
                    .when()
                    .get(GITHUB_API_URL)
                    .then()
                    .extract()
                    .response();

            // Step 2: Validate response status code
            int actualStatusCode = response.getStatusCode();
            boolean testResult = (actualStatusCode == EXPECTED_STATUS_CODE);

            if (testResult) {
                System.out.println("GitHub API Test PASSED - Status Code: " + actualStatusCode);
            } else {
                System.err.println("GitHub API Test FAILED - Expected: " + EXPECTED_STATUS_CODE +
                        ", Actual: " + actualStatusCode);
            }

            // Optional: Log response details for debugging
            System.out.println("Response Time: " + response.getTime() + "ms");
            System.out.println("Content Type: " + response.getContentType());

            return testResult;

        } catch (Exception e) {
            System.err.println("GitHub API Test failed with exception: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get test details
     * 
     * @return String description of the test
     */
    public String getTestDescription() {
        return "GitHub API Test - Calls GET " + GITHUB_API_URL +
                " and validates status code is " + EXPECTED_STATUS_CODE;
    }
}
