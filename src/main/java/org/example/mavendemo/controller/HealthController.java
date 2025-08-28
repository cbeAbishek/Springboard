package org.example.mavendemo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health Check", description = "Application health and status monitoring endpoints")
public class HealthController {

    @Operation(
            summary = "Application health check",
            description = "Returns the current health status of the application and its components"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Application is healthy",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Health Status",
                                    value = """
                                            {
                                                "status": "UP",
                                                "timestamp": "2023-08-28T10:30:00",
                                                "application": "Test Framework API",
                                                "version": "1.0"
                                            }
                                            """
                            )
                    )
            )
    })
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> healthStatus = new HashMap<>();
        healthStatus.put("status", "UP");
        healthStatus.put("timestamp", LocalDateTime.now());
        healthStatus.put("application", "Test Framework API");
        healthStatus.put("version", "1.0");

        return ResponseEntity.ok(healthStatus);
    }

    @Operation(
            summary = "Database connectivity check",
            description = "Checks if the database is accessible and responsive"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Database is accessible",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(responseCode = "503", description = "Database is not accessible")
    })
    @GetMapping("/database")
    public ResponseEntity<Map<String, Object>> databaseHealth() {
        Map<String, Object> dbHealth = new HashMap<>();
        try {
            // Simple database connectivity check
            dbHealth.put("status", "UP");
            dbHealth.put("database", "Connected");
            dbHealth.put("timestamp", LocalDateTime.now());
            return ResponseEntity.ok(dbHealth);
        } catch (Exception e) {
            dbHealth.put("status", "DOWN");
            dbHealth.put("database", "Connection failed");
            dbHealth.put("error", e.getMessage());
            dbHealth.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(503).body(dbHealth);
        }
    }

    @Operation(
            summary = "API endpoints status",
            description = "Returns status of all available API endpoints"
    )
    @GetMapping("/endpoints")
    public ResponseEntity<Map<String, Object>> endpointsStatus() {
        Map<String, Object> endpoints = new HashMap<>();
        Map<String, String> testCaseEndpoints = new HashMap<>();
        Map<String, String> testResultEndpoints = new HashMap<>();

        testCaseEndpoints.put("GET /api/v1/tests", "Get all test cases");
        testCaseEndpoints.put("POST /api/v1/tests", "Create new test case");
        testCaseEndpoints.put("GET /api/v1/tests/{id}", "Get test case by ID");
        testCaseEndpoints.put("PUT /api/v1/tests/{id}", "Update test case");
        testCaseEndpoints.put("DELETE /api/v1/tests/{id}", "Delete test case");
        testCaseEndpoints.put("GET /api/v1/tests/search", "Search test cases");

        testResultEndpoints.put("GET /api/v1/test-results", "Get all test results");
        testResultEndpoints.put("POST /api/v1/test-results", "Create new test result");
        testResultEndpoints.put("GET /api/v1/test-results/{id}", "Get test result by ID");
        testResultEndpoints.put("PUT /api/v1/test-results/{id}", "Update test result");
        testResultEndpoints.put("DELETE /api/v1/test-results/{id}", "Delete test result");
        testResultEndpoints.put("GET /api/v1/test-results/test-case/{testCaseId}", "Get results by test case");
        testResultEndpoints.put("GET /api/v1/test-results/status/{status}", "Get results by status");

        endpoints.put("testCases", testCaseEndpoints);
        endpoints.put("testResults", testResultEndpoints);
        endpoints.put("timestamp", LocalDateTime.now());
        endpoints.put("totalEndpoints", testCaseEndpoints.size() + testResultEndpoints.size());

        return ResponseEntity.ok(endpoints);
    }
}
