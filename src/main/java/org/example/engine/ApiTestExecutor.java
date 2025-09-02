package org.example.engine;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.example.model.TestCase;
import org.example.model.TestExecution;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Component
public class ApiTestExecutor {

    private static final Logger log = LoggerFactory.getLogger(ApiTestExecutor.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TestExecution executeApiTest(TestCase testCase, String environment) {
        TestExecution execution = new TestExecution();
        execution.setTestCase(testCase);
        execution.setExecutionId(UUID.randomUUID().toString());
        execution.setEnvironment(environment);
        execution.setStartTime(LocalDateTime.now());
        execution.setStatus(TestExecution.ExecutionStatus.RUNNING);

        try {
            log.info("Starting API test execution for test case: {}", testCase.getName());

            // Parse test data
            Map<String, Object> testData = parseTestData(testCase.getTestData());

            // Execute API call
            Response response = executeApiCall(testData);

            // Validate response
            validateResponse(response, testCase.getExpectedResult(), execution);

            execution.setStatus(TestExecution.ExecutionStatus.PASSED);
            log.info("API test passed: {}", testCase.getName());

        } catch (AssertionError e) {
            execution.setStatus(TestExecution.ExecutionStatus.FAILED);
            execution.setErrorMessage(e.getMessage());
            execution.setStackTrace(getStackTrace(e));
            log.error("API test failed: {}", testCase.getName(), e);

        } catch (Exception e) {
            execution.setStatus(TestExecution.ExecutionStatus.ERROR);
            execution.setErrorMessage(e.getMessage());
            execution.setStackTrace(getStackTrace(e));
            log.error("API test error: {}", testCase.getName(), e);

        } finally {
            execution.setEndTime(LocalDateTime.now());
            if (execution.getStartTime() != null && execution.getEndTime() != null) {
                long duration = java.time.Duration.between(execution.getStartTime(), execution.getEndTime()).toMillis();
                execution.setExecutionDuration(duration);
            }
        }

        return execution;
    }

    private Response executeApiCall(Map<String, Object> testData) {
        RequestSpecification request = RestAssured.given();

        // Set base URI
        if (testData.containsKey("baseUri")) {
            request.baseUri((String) testData.get("baseUri"));
        }

        // Set headers
        if (testData.containsKey("headers")) {
            Map<String, String> headers = (Map<String, String>) testData.get("headers");
            headers.forEach(request::header);
        }

        // Set query parameters
        if (testData.containsKey("queryParams")) {
            Map<String, String> queryParams = (Map<String, String>) testData.get("queryParams");
            queryParams.forEach(request::queryParam);
        }

        // Set request body
        if (testData.containsKey("body")) {
            request.body(testData.get("body"));
        }

        // Set content type
        if (testData.containsKey("contentType")) {
            request.contentType((String) testData.get("contentType"));
        }

        // Execute request based on method
        String method = (String) testData.get("method");
        String endpoint = (String) testData.get("endpoint");

        Response response;
        switch (method.toUpperCase()) {
            case "GET":
                response = request.get(endpoint);
                break;
            case "POST":
                response = request.post(endpoint);
                break;
            case "PUT":
                response = request.put(endpoint);
                break;
            case "DELETE":
                response = request.delete(endpoint);
                break;
            case "PATCH":
                response = request.patch(endpoint);
                break;
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }

        log.info("API call executed: {} {}, Status: {}", method, endpoint, response.getStatusCode());
        return response;
    }

    private void validateResponse(Response response, String expectedResult, TestExecution execution) {
        try {
            Map<String, Object> expectedData = parseTestData(expectedResult);

            // Store actual result
            execution.setActualResult(response.asString());

            // Validate status code
            if (expectedData.containsKey("statusCode")) {
                int expectedStatusCode = (Integer) expectedData.get("statusCode");
                if (response.getStatusCode() != expectedStatusCode) {
                    throw new AssertionError("Status code mismatch. Expected: " + expectedStatusCode +
                                           ", Actual: " + response.getStatusCode());
                }
            }

            // Validate response body
            if (expectedData.containsKey("responseBody")) {
                String expectedBody = (String) expectedData.get("responseBody");
                String actualBody = response.asString();
                if (!actualBody.contains(expectedBody)) {
                    throw new AssertionError("Response body validation failed. Expected to contain: " + expectedBody);
                }
            }

            // Validate response time
            if (expectedData.containsKey("maxResponseTime")) {
                long maxResponseTime = (Long) expectedData.get("maxResponseTime");
                if (response.getTime() > maxResponseTime) {
                    throw new AssertionError("Response time exceeded. Expected: < " + maxResponseTime +
                                           "ms, Actual: " + response.getTime() + "ms");
                }
            }

        } catch (Exception e) {
            throw new AssertionError("Response validation failed: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> parseTestData(String jsonData) {
        try {
            return objectMapper.readValue(jsonData, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse test data: " + e.getMessage(), e);
        }
    }

    private String getStackTrace(Throwable e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
