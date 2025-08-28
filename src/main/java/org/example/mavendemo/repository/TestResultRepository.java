package org.example.mavendemo.repository;

import org.example.mavendemo.model.TestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestResultRepository extends JpaRepository<TestResult, Long> {

    // Find all test results for a specific test case
    List<TestResult> findByTestCaseId(Long testCaseId);

    // Find all test results by status
    List<TestResult> findByStatus(String status);

    // Find test results ordered by execution date (most recent first)
    List<TestResult> findAllByOrderByExecutedAtDesc();
}
