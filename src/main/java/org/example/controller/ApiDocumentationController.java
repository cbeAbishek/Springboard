package org.example.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/docs")
@CrossOrigin(origins = "*")
public class ApiDocumentationController {

    @GetMapping("/endpoints")
    public ResponseEntity<Map<String, Object>> getApiEndpoints() {
        Map<String, Object> documentation = new HashMap<>();
        List<Map<String, Object>> endpoints = new ArrayList<>();

        // Dashboard endpoints
        endpoints.add(createEndpoint("GET", "/api/dashboard/metrics", "Get dashboard metrics including test counts, success rates, and system stats"));
        endpoints.add(createEndpoint("GET", "/api/dashboard/recent-activity", "Get recent test activity and batch executions"));
        endpoints.add(createEndpoint("GET", "/api/dashboard/performance-summary", "Get system performance summary and trends"));
        endpoints.add(createEndpoint("GET", "/api/dashboard/environment-status", "Get status of different test environments"));

        // Test Case endpoints
        endpoints.add(createEndpoint("GET", "/api/testcases", "Get all active test cases"));
        endpoints.add(createEndpoint("GET", "/api/testcases/{id}", "Get specific test case by ID"));
        endpoints.add(createEndpoint("POST", "/api/testcases", "Create a new test case"));
        endpoints.add(createEndpoint("PUT", "/api/testcases/{id}", "Update an existing test case"));
        endpoints.add(createEndpoint("DELETE", "/api/testcases/{id}", "Soft delete a test case"));
        endpoints.add(createEndpoint("GET", "/api/testcases/suite/{testSuite}", "Get test cases by test suite"));
        endpoints.add(createEndpoint("GET", "/api/testcases/environment/{environment}", "Get test cases by environment"));
        endpoints.add(createEndpoint("GET", "/api/testcases/meta/testsuites", "Get available test suite names"));
        endpoints.add(createEndpoint("GET", "/api/testcases/meta/environments", "Get available environment names"));

        // Test Execution endpoints
        endpoints.add(createEndpoint("POST", "/api/execution/batch", "Execute test batch with parallel execution"));
        endpoints.add(createEndpoint("POST", "/api/execution/single/{testCaseId}", "Execute a single test case"));
        endpoints.add(createEndpoint("GET", "/api/execution/batch/{batchId}", "Get batch execution status"));
        endpoints.add(createEndpoint("GET", "/api/execution/batch/{batchId}/executions", "Get all executions in a batch"));
        endpoints.add(createEndpoint("GET", "/api/execution/batches", "Get all test batches"));
        endpoints.add(createEndpoint("GET", "/api/execution/batches/recent", "Get recent test batches"));
        endpoints.add(createEndpoint("GET", "/api/execution/batches/active", "Get currently running batches"));
        endpoints.add(createEndpoint("GET", "/api/execution/batches/status/{status}", "Get batches by status"));
        endpoints.add(createEndpoint("GET", "/api/execution/executions", "Get all test executions"));
        endpoints.add(createEndpoint("GET", "/api/execution/batch/{batchId}/progress", "Get real-time batch progress"));
        endpoints.add(createEndpoint("POST", "/api/execution/batch/{batchId}/cancel", "Cancel a running batch"));
        endpoints.add(createEndpoint("GET", "/api/execution/parallel/status", "Get parallel execution status"));

        // Schedule endpoints
        endpoints.add(createEndpoint("GET", "/api/schedules", "Get all test schedules"));
        endpoints.add(createEndpoint("GET", "/api/schedules/active", "Get active schedules only"));
        endpoints.add(createEndpoint("POST", "/api/schedules", "Create a new test schedule"));
        endpoints.add(createEndpoint("PUT", "/api/schedules/{id}", "Update an existing schedule"));
        endpoints.add(createEndpoint("DELETE", "/api/schedules/{id}", "Delete a test schedule"));
        endpoints.add(createEndpoint("POST", "/api/schedules/{id}/activate", "Activate a schedule"));
        endpoints.add(createEndpoint("POST", "/api/schedules/{id}/deactivate", "Deactivate a schedule"));
        endpoints.add(createEndpoint("GET", "/api/schedules/{id}/preview", "Preview next execution times"));
        endpoints.add(createEndpoint("POST", "/api/schedules/{id}/trigger", "Trigger schedule immediately"));
        endpoints.add(createEndpoint("POST", "/api/schedules/{id}/execute", "Execute schedule now"));
        endpoints.add(createEndpoint("GET", "/api/schedules/next", "List upcoming scheduled executions"));
        endpoints.add(createEndpoint("PATCH", "/api/schedules/{id}/status", "Update schedule status"));

        // Health and monitoring endpoints
        endpoints.add(createEndpoint("GET", "/api/actuator/health", "Get system health status"));
        endpoints.add(createEndpoint("GET", "/api/actuator/info", "Get application information"));
        endpoints.add(createEndpoint("GET", "/api/actuator/metrics", "Get system metrics"));

        // Reports endpoints
        endpoints.add(createEndpoint("GET", "/api/reports", "Get available test reports"));
        endpoints.add(createEndpoint("POST", "/api/reports/generate", "Generate a new test report"));
        endpoints.add(createEndpoint("GET", "/api/reports/{reportId}", "Get specific report"));
        endpoints.add(createEndpoint("GET", "/api/reports/batch/{batchId}", "Generate report for specific batch"));

        // Analytics endpoints
        endpoints.add(createEndpoint("GET", "/api/analytics/trends", "Get test execution trends"));
        endpoints.add(createEndpoint("GET", "/api/analytics/performance", "Get performance analytics"));
        endpoints.add(createEndpoint("POST", "/api/analytics/regression", "Run regression analysis"));

        // Validation endpoints
        endpoints.add(createEndpoint("GET", "/api/validation/health", "Validate system health"));
        endpoints.add(createEndpoint("POST", "/api/validation/framework", "Validate framework integrity"));

        documentation.put("endpoints", endpoints);
        documentation.put("version", "2.1.0");
        documentation.put("description", "Springboard Test Automation Framework API Documentation");
        documentation.put("timestamp", LocalDateTime.now());
        documentation.put("baseUrl", "/api");

        return ResponseEntity.ok(documentation);
    }

    @GetMapping("/swagger")
    public ResponseEntity<Map<String, Object>> getSwaggerInfo() {
        Map<String, Object> swagger = new HashMap<>();
        swagger.put("swagger", "2.0");
        swagger.put("info", Map.of(
            "title", "Springboard Test Automation Framework API",
            "version", "2.1.0",
            "description", "Enterprise Test Automation Framework with Parallel Execution Support"
        ));
        swagger.put("basePath", "/api");
        swagger.put("produces", List.of("application/json"));
        swagger.put("consumes", List.of("application/json"));

        return ResponseEntity.ok(swagger);
    }

    @GetMapping("/openapi")
    public ResponseEntity<Map<String, Object>> getOpenApiSpec() {
        Map<String, Object> openapi = new HashMap<>();
        openapi.put("openapi", "3.0.3");
        openapi.put("info", Map.of(
            "title", "Springboard Test Automation Framework API",
            "version", "2.1.0",
            "description", "Enterprise Test Automation Framework with Parallel Execution Support",
            "contact", Map.of(
                "name", "Springboard Framework Team",
                "email", "support@springboard.com"
            )
        ));

        Map<String, Object> servers = Map.of(
            "url", "http://localhost:8080/api",
            "description", "Development server"
        );
        openapi.put("servers", List.of(servers));

        return ResponseEntity.ok(openapi);
    }

    private Map<String, Object> createEndpoint(String method, String path, String description) {
        Map<String, Object> endpoint = new HashMap<>();
        endpoint.put("method", method);
        endpoint.put("path", path);
        endpoint.put("description", description);

        // Add example parameters for common endpoints
        if (path.contains("{id}")) {
            endpoint.put("parameters", Map.of("id", "Path parameter - Resource ID"));
        }
        if (path.contains("{batchId}")) {
            endpoint.put("parameters", Map.of("batchId", "Path parameter - Batch ID"));
        }
        if (path.contains("{testCaseId}")) {
            endpoint.put("parameters", Map.of("testCaseId", "Path parameter - Test Case ID"));
        }

        return endpoint;
    }
}
