package org.example.mavendemo.service;

import org.example.mavendemo.model.TestCase;
import org.example.mavendemo.repository.TestCaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service  // Marks this class as a Spring Service component
public class TestCaseServiceImpl implements TestCaseService {

    private final TestCaseRepository testCaseRepository;

    // Constructor injection for the repository (best practice)
    @Autowired
    public TestCaseServiceImpl(TestCaseRepository testCaseRepository) {
        this.testCaseRepository = testCaseRepository;
    }

    // Save or update a test case by delegating to the repository
    @Override
    public TestCase saveTestCase(TestCase testCase) {
        return testCaseRepository.save(testCase);
    }

    // Find a test case by ID, return Optional to handle nulls gracefully
    @Override
    public Optional<TestCase> getTestCaseById(Long id) {
        return testCaseRepository.findById(id);
    }

    // Get all test cases from the database
    @Override
    public List<TestCase> getAllTestCases() {
        return testCaseRepository.findAll();
    }

    // Delete a test case by ID
    @Override
    public void deleteTestCase(Long id) {
        testCaseRepository.deleteById(id);
    }
}
