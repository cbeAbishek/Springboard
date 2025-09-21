package org.example.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.Data;

import java.util.*;

@RestController
@RequestMapping("/api/docs")
@CrossOrigin(origins = "*")
public class ApiDocumentationController {

    @GetMapping("/endpoints")
    public ResponseEntity<ApiDocumentation> getAllEndpoints() {
        ApiDocumentation documentation = new ApiDocumentation();
        documentation.setTitle("Test Automation Framework API Documentation");
        documentation.setVersion("2.0");
        documentation.setDescription("Complete API documentation for the test automation framework - Updated with all actual endpoints");

        List<EndpointInfo> endpoints = new ArrayList<>();

        // Test Execution Endpoints
        endpoints.add(new EndpointInfo(
            "POST", "/api/execution/batch", "Execute Test Batch",
            "Start execution of a test batch with specified test suite and environment",
            Map.of(
                "testSuite", "string - Name of the test suite to execute",
                "environment", "string - Target environment (dev, staging, prod)",
                "parallelThreads", "integer - Number of parallel threads (default: 1)"
            ),
            "BatchResponse with batchId, status, and message"
        ));

        endpoints.add(new EndpointInfo(
            "POST", "/api/execution/single/{testCaseId}", "Execute Single Test",
            "Execute a single test case by ID with environment parameter",
            Map.of(
                "testCaseId", "long - ID of the test case to execute (path parameter)",
                "environment", "string - Target environment (query parameter, required)"
            ),
            "TestExecution object with execution details"
        ));

        endpoints.add(new EndpointInfo(
            "GET", "/api/execution/batch/{batchId}", "Get Batch Status",
            "Get the status and details of a specific test batch",
            Map.of("batchId", "string - Unique identifier of the batch (path parameter)"),
            "TestBatch object with batch details and status"
        ));

        endpoints.add(new EndpointInfo(
            "GET", "/api/execution/batch/{batchId}/executions", "Get Batch Executions",
            "Get all test executions for a specific batch",
            Map.of("batchId", "string - Unique identifier of the batch (path parameter)"),
            "List of TestExecution objects"
        ));

        endpoints.add(new EndpointInfo(
            "GET", "/api/execution/batch/{batchId}/progress", "Get Batch Progress",
            "Get detailed progress information for a specific batch including pass/fail counts",
            Map.of("batchId", "string - Unique identifier of the batch (path parameter)"),
            "BatchProgressResponse with detailed progress metrics"
        ));

        endpoints.add(new EndpointInfo(
            "GET", "/api/execution/batches", "Get All Batches",
            "Retrieve all test batches ordered by creation date (newest first)",
            Map.of(),
            "List of TestBatch objects ordered by creation date"
        ));

        endpoints.add(new EndpointInfo(
            "GET", "/api/execution/batches/recent", "Get Recent Batches",
            "Retrieve recent test batches with optional limit and time filter",
            Map.of(
                "limit", "integer - Maximum number of batches to return (default: 10, max: 100)",
                "hours", "integer - Number of hours to look back (default: 24)"
            ),
            "List of recent TestBatch objects"
        ));

        endpoints.add(new EndpointInfo(
            "GET", "/api/execution/batches/status/{status}", "Get Batches by Status",
            "Retrieve test batches filtered by status",
            Map.of("status", "string - Batch status (SCHEDULED, RUNNING, COMPLETED, CANCELLED, FAILED)"),
            "List of TestBatch objects with specified status"
        ));

        endpoints.add(new EndpointInfo(
            "GET", "/api/execution/batches/active", "Get Active Batches",
            "Retrieve currently active test batches (SCHEDULED or RUNNING)",
            Map.of(),
            "List of active TestBatch objects"
        ));

        endpoints.add(new EndpointInfo(
            "GET", "/api/execution/executions", "Get All Executions",
            "Retrieve all test executions across all batches",
            Map.of(),
            "List of TestExecution objects"
        ));

        // Test Case Management Endpoints
        endpoints.add(new EndpointInfo(
            "GET", "/api/testcases", "Get All Test Cases",
            "Retrieve all active test cases",
            Map.of(),
            "List of active TestCase objects"
        ));

        endpoints.add(new EndpointInfo(
            "GET", "/api/testcases/{id}", "Get Test Case",
            "Get a specific test case by ID",
            Map.of("id", "long - ID of the test case (path parameter)"),
            "TestCase object or 404 if not found"
        ));

        endpoints.add(new EndpointInfo(
            "POST", "/api/testcases", "Create Test Case",
            "Create a new test case",
            Map.of(
                "name", "string - Name of the test case (required)",
                "description", "string - Description of the test case",
                "testType", "enum - Type of test (API, UI, INTEGRATION)",
                "testData", "string - Test data in JSON format",
                "expectedResult", "string - Expected test result",
                "priority", "enum - Priority level (HIGH, MEDIUM, LOW, CRITICAL)",
                "testSuite", "string - Test suite name",
                "environment", "string - Target environment"
            ),
            "Created TestCase object with generated ID"
        ));

        endpoints.add(new EndpointInfo(
            "PUT", "/api/testcases/{id}", "Update Test Case",
            "Update an existing test case",
            Map.of(
                "id", "long - ID of the test case to update (path parameter)",
                "name", "string - Updated name",
                "description", "string - Updated description",
                "testType", "enum - Updated test type",
                "testData", "string - Updated test data",
                "expectedResult", "string - Updated expected result",
                "priority", "enum - Updated priority",
                "testSuite", "string - Updated test suite",
                "environment", "string - Updated environment"
            ),
            "Updated TestCase object or 404 if not found"
        ));

        endpoints.add(new EndpointInfo(
            "DELETE", "/api/testcases/{id}", "Delete Test Case",
            "Soft delete a test case (marks as inactive)",
            Map.of("id", "long - ID of the test case to delete (path parameter)"),
            "No content (204) or 404 if not found"
        ));

        endpoints.add(new EndpointInfo(
            "GET", "/api/testcases/suite/{testSuite}", "Get Test Cases by Suite",
            "Get all active test cases for a specific test suite",
            Map.of("testSuite", "string - Name of the test suite (path parameter)"),
            "List of TestCase objects filtered by test suite"
        ));

        endpoints.add(new EndpointInfo(
            "GET", "/api/testcases/environment/{environment}", "Get Test Cases by Environment",
            "Get all active test cases for a specific environment",
            Map.of("environment", "string - Environment name (path parameter)"),
            "List of TestCase objects filtered by environment"
        ));

        endpoints.add(new EndpointInfo(
            "GET", "/api/testcases/meta/testsuites", "Get Available Test Suites",
            "Get list of all unique test suites from active test cases",
            Map.of(),
            "List of distinct test suite names"
        ));

        endpoints.add(new EndpointInfo(
            "GET", "/api/testcases/meta/environments", "Get Available Environments",
            "Get list of all unique environments from active test cases",
            Map.of(),
            "List of distinct environment names"
        ));

        // Schedule Management Endpoints
        endpoints.add(new EndpointInfo(
            "GET", "/api/schedules", "Get All Schedules",
            "Retrieve all test schedules as DTO objects",
            Map.of(),
            "List of ScheduleDTO objects"
        ));

        endpoints.add(new EndpointInfo(
            "GET", "/api/schedules/active", "Get Active Schedules",
            "Retrieve only active test schedules as DTO objects",
            Map.of(),
            "List of active ScheduleDTO objects"
        ));

        endpoints.add(new EndpointInfo(
            "POST", "/api/schedules", "Create Schedule",
            "Create a new test schedule with validation",
            Map.of(
                "scheduleName", "string - Name of the schedule (required)",
                "cronExpression", "string - Valid cron expression for scheduling (required)",
                "testSuite", "string - Test suite to execute",
                "environment", "string - Target environment",
                "parallelThreads", "integer - Number of parallel threads (default: 1)",
                "enabled", "boolean - Whether schedule is active (default: true)"
            ),
            "Response object with success status and created schedule DTO"
        ));

        endpoints.add(new EndpointInfo(
            "PUT", "/api/schedules/{id}", "Update Schedule",
            "Update an existing test schedule with flexible validation",
            Map.of(
                "id", "long - ID of the schedule to update (path parameter)",
                "scheduleName", "string - Updated schedule name (required if cronExpression provided)",
                "cronExpression", "string - Updated cron expression (required if scheduleName provided)",
                "testSuite", "string - Updated test suite",
                "environment", "string - Updated environment",
                "parallelThreads", "integer - Updated parallel threads",
                "enabled", "boolean - Updated active status (can be used alone for toggle)"
            ),
            "Response object with success status and updated schedule DTO"
        ));

        endpoints.add(new EndpointInfo(
            "DELETE", "/api/schedules/{id}", "Delete Schedule",
            "Soft delete a test schedule (marks as inactive and unschedules)",
            Map.of("id", "long - ID of the schedule to delete (path parameter)"),
            "No content (204) or 404 if not found"
        ));

        endpoints.add(new EndpointInfo(
            "POST", "/api/schedules/{id}/activate", "Activate Schedule",
            "Activate a test schedule and schedule it in Quartz",
            Map.of("id", "long - ID of the schedule to activate (path parameter)"),
            "Activated TestSchedule object or 404 if not found"
        ));

        endpoints.add(new EndpointInfo(
            "POST", "/api/schedules/{id}/deactivate", "Deactivate Schedule",
            "Deactivate a test schedule and unschedule it from Quartz",
            Map.of("id", "long - ID of the schedule to deactivate (path parameter)"),
            "Deactivated TestSchedule object or 404 if not found"
        ));

        endpoints.add(new EndpointInfo(
            "GET", "/api/schedules/{id}/preview", "Preview Schedule Executions",
            "Preview next execution times for a schedule based on its cron expression",
            Map.of(
                "id", "long - ID of the schedule (path parameter)",
                "count", "integer - Number of next executions to preview (default: 5, max: 20)"
            ),
            "List of next execution timestamps or 404 if schedule not found"
        ));

        endpoints.add(new EndpointInfo(
            "POST", "/api/schedules/{id}/execute", "Execute Schedule Now",
            "Trigger immediate execution of a scheduled test",
            Map.of("id", "long - ID of the schedule to execute (path parameter)"),
            "BatchResponse with execution details or 404 if schedule not found"
        ));

        // Reporting Endpoints
        endpoints.add(new EndpointInfo(
            "POST", "/api/reports/generate/{batchId}", "Generate All Reports (Deprecated)",
            "Legacy endpoint - redirects to comprehensive report API",
            Map.of("batchId", "string - Unique identifier of the batch (path parameter)"),
            "Redirect message to use /comprehensive/* endpoints"
        ));

        endpoints.add(new EndpointInfo(
            "POST", "/api/reports/html/{batchId}", "Generate HTML Report (Deprecated)",
            "Legacy endpoint - use comprehensive report API instead",
            Map.of("batchId", "string - Unique identifier of the batch (path parameter)"),
            "ReportResult object with redirect message"
        ));

        endpoints.add(new EndpointInfo(
            "GET", "/api/reports/list", "List Available Reports",
            "Get list of all generated report files with metadata",
            Map.of(),
            "List of ReportInfo objects with file details, sorted by creation time"
        ));

        endpoints.add(new EndpointInfo(
            "GET", "/api/reports/download/{filename}", "Download Report File",
            "Download a specific report file",
            Map.of("filename", "string - Name of the report file (path parameter)"),
            "File download response or 404 if file not found"
        ));

        // Dashboard Endpoints
        endpoints.add(new EndpointInfo(
            "GET", "/api/dashboard/metrics", "Get Dashboard Metrics",
            "Retrieve comprehensive dashboard metrics including counts and success rates",
            Map.of(),
            "Dashboard metrics object with test cases, batches, executions, and schedules statistics"
        ));

        endpoints.add(new EndpointInfo(
            "GET", "/api/dashboard/recent-activity", "Get Recent Activity",
            "Retrieve recent activity including recent batches and executions from the last 24 hours",
            Map.of(),
            "Recent activity object with recent batches, execution counts, and timestamp information"
        ));

        // Analytics Endpoints (if AnalyticsController exists)
        endpoints.add(new EndpointInfo(
            "GET", "/api/analytics/trends", "Get Trend Analysis",
            "Get test trend analysis for a specified date range",
            Map.of(
                "fromDate", "datetime - Start date for analysis (query parameter, ISO format)",
                "toDate", "datetime - End date for analysis (query parameter, ISO format)"
            ),
            "TestTrendAnalysis object with trend data"
        ));

        endpoints.add(new EndpointInfo(
            "GET", "/api/analytics/regression/{environment}", "Get Regression Metrics",
            "Get regression metrics for a specific environment",
            Map.of(
                "environment", "string - Environment name (path parameter)",
                "days", "integer - Number of days to analyze (query parameter, default: 30)"
            ),
            "RegressionMetrics object with regression data"
        ));

        // Validation Endpoints
        endpoints.add(new EndpointInfo(
            "GET", "/api/validation/framework", "Validate Framework",
            "Perform comprehensive framework validation with real data to ensure system integrity",
            Map.of(),
            "ValidationResult object with validation status, checks performed, and any issues found"
        ));

        endpoints.add(new EndpointInfo(
            "GET", "/api/validation/health", "Health Check",
            "Perform a basic health check of the validation service",
            Map.of(),
            "String message confirming validation service status"
        ));

        // Demo Data Endpoints (if MockDataService endpoints exist)
        endpoints.add(new EndpointInfo(
            "GET", "/api/demo/test-cases", "Get Demo Test Cases",
            "Retrieve sample test cases for demonstration purposes",
            Map.of(),
            "List of sample TestCase objects with realistic data"
        ));

        endpoints.add(new EndpointInfo(
            "GET", "/api/demo/executions", "Get Demo Executions",
            "Retrieve sample test executions for demonstration purposes",
            Map.of(),
            "List of sample execution objects with status and timing data"
        ));

        endpoints.add(new EndpointInfo(
            "GET", "/api/demo/batches", "Get Demo Batches",
            "Retrieve sample test batches for demonstration purposes",
            Map.of(),
            "List of sample batch objects with various statuses"
        ));

        endpoints.add(new EndpointInfo(
            "GET", "/api/demo/sample-data", "Get All Sample Data",
            "Retrieve comprehensive sample data including test cases, executions, batches, metrics, and environments",
            Map.of(),
            "Comprehensive object containing all sample data for demo purposes"
        ));

        endpoints.add(new EndpointInfo(
            "POST", "/api/demo/create-sample-test", "Create Sample Test",
            "Create a sample test case for demonstration purposes",
            Map.of("testData", "object - Test case data in JSON format"),
            "Response object with success status, test ID, and confirmation message"
        ));

        // API Documentation Endpoint
        endpoints.add(new EndpointInfo(
            "GET", "/api/docs/endpoints", "Get API Documentation",
            "Get complete API documentation with all available endpoints (this endpoint)",
            Map.of(),
            "ApiDocumentation object with all endpoint information"
        ));

        documentation.setEndpoints(endpoints);
        documentation.setTotalEndpoints(endpoints.size());

        return ResponseEntity.ok(documentation);
    }

    @Data
    public static class ApiDocumentation {
        private String title;
        private String version;
        private String description;
        private int totalEndpoints;
        private List<EndpointInfo> endpoints;
    }

    @Data
    public static class EndpointInfo {
        private String method;
        private String path;
        private String name;
        private String description;
        private Map<String, String> parameters;
        private String response;

        public EndpointInfo(String method, String path, String name, String description,
                           Map<String, String> parameters, String response) {
            this.method = method;
            this.path = path;
            this.name = name;
            this.description = description;
            this.parameters = parameters;
            this.response = response;
        }
    }
}
