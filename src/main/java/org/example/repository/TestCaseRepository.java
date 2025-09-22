package org.example.repository;

import org.example.model.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

    List<TestCase> findByIsActiveTrue();

    List<TestCase> findByTestSuiteAndIsActiveTrue(String testSuite);

    List<TestCase> findByEnvironmentAndIsActiveTrue(String environment);

    List<TestCase> findByTestTypeAndIsActiveTrue(TestCase.TestType testType);

    @Query("SELECT tc FROM TestCase tc WHERE tc.testSuite = :testSuite AND tc.environment = :environment AND tc.isActive = true")
    List<TestCase> findByTestSuiteAndEnvironment(@Param("testSuite") String testSuite, @Param("environment") String environment);

    @Query("SELECT tc FROM TestCase tc WHERE tc.priority = :priority AND tc.isActive = true ORDER BY tc.createdAt DESC")
    List<TestCase> findByPriority(@Param("priority") TestCase.Priority priority);

    // Add missing methods called by DashboardController
    @Query("SELECT COUNT(tc) FROM TestCase tc WHERE tc.isActive = true")
    long countActiveTestCases();

    @Query("SELECT COUNT(tc) FROM TestCase tc WHERE tc.isActive = false")
    long countInactiveTestCases();

    @Query("SELECT tc FROM TestCase tc ORDER BY tc.createdAt DESC")
    List<TestCase> findAllOrderByCreatedAtDesc();

    @Query("SELECT tc FROM TestCase tc WHERE tc.isActive = false")
    List<TestCase> findByIsActiveFalse();
}
