package org.example.mavendemo.controller;

import org.example.mavendemo.model.TestCase;
import org.example.mavendemo.service.TestCaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/tests")  // Base path for all endpoints in this controller
public class TestCaseController {

    private final TestCaseService testCaseService;

    @Autowired
    public TestCaseController(TestCaseService testCaseService) {
        this.testCaseService = testCaseService;
    }

    // POST /tests - Create new test case
    @PostMapping
    public ResponseEntity<TestCase> createTestCase(@RequestBody TestCase testCase) {
        TestCase savedTestCase = testCaseService.saveTestCase(testCase);
        return ResponseEntity.ok(savedTestCase);
    }

    // GET /tests/{id} - Fetch test case by ID
    @GetMapping("/{id}")
    public ResponseEntity<TestCase> getTestCaseById(@PathVariable Long id) {
        Optional<TestCase> testCaseOpt = testCaseService.getTestCaseById(id);

        return testCaseOpt
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // GET /tests - Fetch all test cases
    @GetMapping
    public ResponseEntity<List<TestCase>> getAllTestCases() {
        List<TestCase> testCases = testCaseService.getAllTestCases();
        return ResponseEntity.ok(testCases);
    }
}

