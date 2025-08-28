package org.example.mavendemo.controller;

import org.example.mavendemo.model.TestCase;
import org.example.mavendemo.service.TestCaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/tests")
@Tag(name = "Test Cases", description = "Operations for managing test cases")
@Validated
public class TestCaseController {

    private final TestCaseService testCaseService;

    @Autowired
    public TestCaseController(TestCaseService testCaseService) {
        this.testCaseService = testCaseService;
    }

    @Operation(
            summary = "Create a new test case",
            description = "Creates a new test case with the provided details. All fields except ID are required."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Test case created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TestCase.class),
                            examples = @ExampleObject(
                                    name = "Example Test Case",
                                    value = """
                                            {
                                                "id": 1,
                                                "name": "Login Test",
                                                "type": "Functional",
                                                "description": "Test user login functionality",
                                                "status": "Active"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<TestCase> createTestCase(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Test case details to create",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = TestCase.class),
                            examples = @ExampleObject(
                                    name = "New Test Case",
                                    value = """
                                            {
                                                "name": "User Registration Test",
                                                "type": "Functional",
                                                "description": "Test user registration with valid data",
                                                "status": "Active"
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody TestCase testCase) {
        TestCase savedTestCase = testCaseService.saveTestCase(testCase);
        return ResponseEntity.ok(savedTestCase);
    }

    @Operation(
            summary = "Get test case by ID",
            description = "Retrieves a specific test case by its unique identifier"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Test case found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TestCase.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Test case not found"),
            @ApiResponse(responseCode = "400", description = "Invalid ID format")
    })
    @GetMapping("/{id}")
    public ResponseEntity<TestCase> getTestCaseById(
            @Parameter(description = "Unique identifier of the test case", example = "1", required = true)
            @PathVariable Long id) {
        Optional<TestCase> testCaseOpt = testCaseService.getTestCaseById(id);

        return testCaseOpt
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Get all test cases",
            description = "Retrieves a list of all test cases in the system"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "List of test cases retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    type = "array",
                                    implementation = TestCase.class
                            )
                    )
            ),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<List<TestCase>> getAllTestCases() {
        List<TestCase> testCases = testCaseService.getAllTestCases();
        return ResponseEntity.ok(testCases);
    }

    @Operation(
            summary = "Update an existing test case",
            description = "Updates an existing test case with new information"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Test case updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TestCase.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Test case not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PutMapping("/{id}")
    public ResponseEntity<TestCase> updateTestCase(
            @Parameter(description = "ID of the test case to update", example = "1", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Updated test case details",
                    required = true
            )
            @Valid @RequestBody TestCase testCase) {
        Optional<TestCase> existingTestCase = testCaseService.getTestCaseById(id);
        if (existingTestCase.isPresent()) {
            testCase.setId(id);
            TestCase updatedTestCase = testCaseService.saveTestCase(testCase);
            return ResponseEntity.ok(updatedTestCase);
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(
            summary = "Delete a test case",
            description = "Deletes a test case by its ID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Test case deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Test case not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTestCase(
            @Parameter(description = "ID of the test case to delete", example = "1", required = true)
            @PathVariable Long id) {
        Optional<TestCase> existingTestCase = testCaseService.getTestCaseById(id);
        if (existingTestCase.isPresent()) {
            testCaseService.deleteTestCase(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(
            summary = "Search test cases by status",
            description = "Retrieves test cases filtered by their status"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Test cases retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    type = "array",
                                    implementation = TestCase.class
                            )
                    )
            )
    })
    @GetMapping("/search")
    public ResponseEntity<List<TestCase>> getTestCasesByStatus(
            @Parameter(description = "Status to filter by", example = "Active")
            @RequestParam(required = false) String status,
            @Parameter(description = "Type to filter by", example = "Functional")
            @RequestParam(required = false) String type) {
        List<TestCase> testCases = testCaseService.getAllTestCases();

        if (status != null) {
            testCases = testCases.stream()
                    .filter(tc -> status.equalsIgnoreCase(tc.getStatus()))
                    .toList();
        }

        if (type != null) {
            testCases = testCases.stream()
                    .filter(tc -> type.equalsIgnoreCase(tc.getType()))
                    .toList();
        }

        return ResponseEntity.ok(testCases);
    }
}
