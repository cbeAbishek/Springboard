package org.example.mavendemo.controller;

import org.example.mavendemo.model.TestResult;
import org.example.mavendemo.repository.TestResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/test-results")
@Tag(name = "Test Results", description = "Operations for managing test execution results")
public class TestResultController {

    private final TestResultRepository testResultRepository;

    @Autowired
    public TestResultController(TestResultRepository testResultRepository) {
        this.testResultRepository = testResultRepository;
    }

    @Operation(
            summary = "Create a new test result",
            description = "Records the result of a test execution with execution details"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Test result created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TestResult.class),
                            examples = @ExampleObject(
                                    name = "Example Test Result",
                                    value = """
                                            {
                                                "id": 1,
                                                "testCaseId": 1,
                                                "status": "PASSED",
                                                "executedAt": "2023-08-28T10:30:00",
                                                "notes": "Test executed successfully",
                                                "duration": 1500
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<TestResult> createTestResult(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Test result details to record",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = TestResult.class),
                            examples = @ExampleObject(
                                    name = "New Test Result",
                                    value = """
                                            {
                                                "testCaseId": 1,
                                                "status": "PASSED",
                                                "executedAt": "2023-08-28T10:30:00",
                                                "notes": "All assertions passed",
                                                "duration": 2000
                                            }
                                            """
                            )
                    )
            )
            @RequestBody TestResult testResult) {
        if (testResult.getExecutedAt() == null) {
            testResult.setExecutedAt(LocalDateTime.now());
        }
        TestResult savedResult = testResultRepository.save(testResult);
        return ResponseEntity.ok(savedResult);
    }

    @Operation(
            summary = "Get test result by ID",
            description = "Retrieves a specific test result by its unique identifier"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Test result found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TestResult.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Test result not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<TestResult> getTestResultById(
            @Parameter(description = "Unique identifier of the test result", example = "1", required = true)
            @PathVariable Long id) {
        Optional<TestResult> testResult = testResultRepository.findById(id);
        return testResult
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Get all test results",
            description = "Retrieves a list of all test results in the system"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "List of test results retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    type = "array",
                                    implementation = TestResult.class
                            )
                    )
            )
    })
    @GetMapping
    public ResponseEntity<List<TestResult>> getAllTestResults() {
        List<TestResult> testResults = testResultRepository.findAll();
        return ResponseEntity.ok(testResults);
    }

    @Operation(
            summary = "Get test results by test case ID",
            description = "Retrieves all test results for a specific test case"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Test results for the test case retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    type = "array",
                                    implementation = TestResult.class
                            )
                    )
            )
    })
    @GetMapping("/test-case/{testCaseId}")
    public ResponseEntity<List<TestResult>> getTestResultsByTestCaseId(
            @Parameter(description = "ID of the test case", example = "1", required = true)
            @PathVariable Long testCaseId) {
        List<TestResult> testResults = testResultRepository.findByTestCaseId(testCaseId);
        return ResponseEntity.ok(testResults);
    }

    @Operation(
            summary = "Get test results by status",
            description = "Retrieves test results filtered by execution status"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Test results filtered by status retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    type = "array",
                                    implementation = TestResult.class
                            )
                    )
            )
    })
    @GetMapping("/status/{status}")
    public ResponseEntity<List<TestResult>> getTestResultsByStatus(
            @Parameter(description = "Execution status to filter by", example = "PASSED", required = true)
            @PathVariable String status) {
        List<TestResult> testResults = testResultRepository.findByStatus(status);
        return ResponseEntity.ok(testResults);
    }

    @Operation(
            summary = "Update a test result",
            description = "Updates an existing test result with new information"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Test result updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TestResult.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Test result not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<TestResult> updateTestResult(
            @Parameter(description = "ID of the test result to update", example = "1", required = true)
            @PathVariable Long id,
            @RequestBody TestResult testResult) {
        Optional<TestResult> existingResult = testResultRepository.findById(id);
        if (existingResult.isPresent()) {
            testResult.setId(id);
            TestResult updatedResult = testResultRepository.save(testResult);
            return ResponseEntity.ok(updatedResult);
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(
            summary = "Delete a test result",
            description = "Deletes a test result by its ID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Test result deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Test result not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTestResult(
            @Parameter(description = "ID of the test result to delete", example = "1", required = true)
            @PathVariable Long id) {
        Optional<TestResult> existingResult = testResultRepository.findById(id);
        if (existingResult.isPresent()) {
            testResultRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
