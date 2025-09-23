package org.example.repository;

import org.example.model.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

    List<TestCase> findByTestType(TestCase.TestType testType);

    List<TestCase> findByIsActiveTrue();

    List<TestCase> findByPriority(TestCase.Priority priority);

    List<TestCase> findByPriorityIn(List<TestCase.Priority> priorities);

    List<TestCase> findByTestTypeAndIsActiveTrue(TestCase.TestType testType);

    List<TestCase> findByNameContaining(String name);

    List<TestCase> findByTestSuite(String testSuite);

    List<TestCase> findByCategoryAndIsActiveTrue(String category);

    List<TestCase> findByTestSuiteAndIsActiveTrue(String testSuite);

    List<TestCase> findByEnvironmentAndIsActiveTrue(String environment);

    @Query("SELECT tc FROM TestCase tc WHERE tc.tags LIKE %:tag% AND tc.isActive = true")
    List<TestCase> findByTagsContaining(String tag);

    @Query("SELECT tc FROM TestCase tc WHERE tc.testType = 'UI' AND tc.name LIKE '%BlazeDemo%' AND tc.isActive = true")
    List<TestCase> findBlazeDemo();

    @Query("SELECT tc FROM TestCase tc WHERE tc.testType = 'UI' AND tc.name LIKE '%ReqRes%' AND tc.isActive = true")
    List<TestCase> findReqRes();

    @Query("SELECT tc FROM TestCase tc WHERE tc.priority IN ('HIGH', 'CRITICAL') AND tc.isActive = true")
    List<TestCase> findRegressionTests();

    @Query("SELECT COUNT(tc) FROM TestCase tc WHERE tc.isActive = true")
    long countActiveTestCases();

    @Query("SELECT COUNT(tc) FROM TestCase tc WHERE tc.testType = :testType AND tc.isActive = true")
    long countByTestTypeAndIsActiveTrue(TestCase.TestType testType);
}
