package org.example.mavendemo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.mavendemo.model.TestResult;
import org.example.mavendemo.service.TestExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for executing test cases.
 * Provides endpoints to trigger test execution and retrieve results.
 */
@RestController
@RequestMapping("/api/tests")
@Tag(name = "Test Execution", description = "APIs for executing test cases and retrieving results")
public class TestExecutionController {

    @Autowired
    private TestExecutionService testExecutionService;

    /**
     * Triggers execution of a test case by ID.
     * 
     * @param id The ID of the test case to execute
     * @return TestResult containing execution details
     */
    @PostMapping("/run/{id}")
    @Operation(summary = "Execute Test Case", description = "Triggers execution of a test case by ID. Supports UI (Selenium) and API (REST-Assured) tests.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Test executed successfully"),
            @ApiResponse(responseCode = "404", description = "Test case not found"),
            @ApiResponse(responseCode = "400", description = "Invalid test case type or execution error"),
            @ApiResponse(responseCode = "500", description = "Internal server error during test execution")
    })
    public ResponseEntity<?> executeTestCase(
            @Parameter(description = "ID of the test case to execute", example = "1") @PathVariable Long id) {

        try {
            // Check if test case exists and is executable
            if (!testExecutionService.isTestCaseExecutable(id)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Test case with ID " + id + " not found or not executable");
                errorResponse.put("supportedTypes", new String[] { "UI", "API" });
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            // Execute the test case
            TestResult result = testExecutionService.executeTestCase(id);

            // Create success response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Test case executed successfully");
            response.put("testResult", result);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            // Handle known runtime exceptions (e.g., test case not found)
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now());

            HttpStatus status = e.getMessage().contains("not found") ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;

            return ResponseEntity.status(status).body(errorResponse);

        } catch (Exception e) {
            // Handle unexpected exceptions
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Internal server error during test execution");
            errorResponse.put("error", e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Gets the latest test result for a specific test case.
     * 
     * @param id The ID of the test case
     * @return Latest TestResult if found
     */
    @GetMapping("/result/{id}")
    @Operation(summary = "Get Latest Test Result", description = "Retrieves the latest test execution result for a specific test case ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Test result retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "No test results found for the test case")
    })
    public ResponseEntity<?> getLatestTestResult(
            @Parameter(description = "ID of the test case", example = "1") @PathVariable Long id) {

        try {
            TestResult result = testExecutionService.getLatestTestResult(id);

            if (result == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "No test results found for test case ID " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("testResult", result);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error retrieving test result");
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Health check endpoint for test execution service.
     * 
     * @return Service status
     */
    @GetMapping("/health")
    @Operation(summary = "Test Execution Service Health Check")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "Test Execution Service");
        response.put("status", "UP");
        response.put("timestamp", java.time.LocalDateTime.now());
        response.put("supportedTestTypes", new String[] { "UI", "API" });

        return ResponseEntity.ok(response);
    }
}
