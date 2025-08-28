package org.example.mavendemo.service;

import org.example.mavendemo.model.TestCase;

import java.util.List;
import java.util.Optional;

public interface TestCaseService {

    // Save a TestCase object (for creating or updating)
    TestCase saveTestCase(TestCase testCase);

    // Fetch a TestCase by its ID (if found, return it wrapped in Optional)
    Optional<TestCase> getTestCaseById(Long id);

    // Fetch all TestCases from database
    List<TestCase> getAllTestCases();
}

