package org.example.mavendemo;

import org.example.mavendemo.model.TestCase;
import org.example.mavendemo.model.TestResult;
import org.example.mavendemo.repository.TestCaseRepository;
import org.example.mavendemo.repository.TestResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class TestDataLoader implements CommandLineRunner {

    private final TestCaseRepository testCaseRepository;
    private final TestResultRepository testResultRepository;

    @Autowired
    public TestDataLoader(TestCaseRepository testCaseRepository, TestResultRepository testResultRepository) {
        this.testCaseRepository = testCaseRepository;
        this.testResultRepository = testResultRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Check if data already exists to avoid duplicates
        if (testCaseRepository.count() == 0) {
            loadSampleData();
            System.out.println("✅ Sample test data loaded into MySQL database");
        } else {
            System.out.println("✅ Database already contains data - skipping sample data load");
        }
    }

    private void loadSampleData() {
        // Create sample test cases
        TestCase loginTest = new TestCase("Login Test", "Functional",
            "Test user login functionality with valid credentials", "Active");
        TestCase registrationTest = new TestCase("Registration Test", "Functional",
            "Test user registration with valid data", "Active");
        TestCase passwordResetTest = new TestCase("Password Reset Test", "Functional",
            "Test password reset functionality", "Active");
        TestCase apiPerformanceTest = new TestCase("API Response Time Test", "Performance",
            "Test API response time under normal load", "Active");
        TestCase dbConnectionTest = new TestCase("Database Connection Test", "Integration",
            "Test database connectivity and queries", "Active");

        // Save test cases
        loginTest = testCaseRepository.save(loginTest);
        registrationTest = testCaseRepository.save(registrationTest);
        passwordResetTest = testCaseRepository.save(passwordResetTest);
        apiPerformanceTest = testCaseRepository.save(apiPerformanceTest);
        dbConnectionTest = testCaseRepository.save(dbConnectionTest);

        // Create sample test results
        TestResult result1 = new TestResult(loginTest.getId(), "PASSED", LocalDateTime.now().minusDays(1));
        result1.setNotes("Login test completed successfully");
        result1.setDuration(1200L);

        TestResult result2 = new TestResult(registrationTest.getId(), "PASSED", LocalDateTime.now().minusDays(1));
        result2.setNotes("Registration test completed successfully");
        result2.setDuration(1800L);

        TestResult result3 = new TestResult(passwordResetTest.getId(), "FAILED", LocalDateTime.now().minusDays(1));
        result3.setNotes("Password reset email not sent");
        result3.setDuration(2500L);

        TestResult result4 = new TestResult(apiPerformanceTest.getId(), "PASSED", LocalDateTime.now().minusDays(1));
        result4.setNotes("API response time within acceptable limits");
        result4.setDuration(800L);

        TestResult result5 = new TestResult(dbConnectionTest.getId(), "PASSED", LocalDateTime.now().minusDays(1));
        result5.setNotes("Database connection established successfully");
        result5.setDuration(500L);

        // Save test results
        testResultRepository.save(result1);
        testResultRepository.save(result2);
        testResultRepository.save(result3);
        testResultRepository.save(result4);
        testResultRepository.save(result5);

        System.out.println("✅ Loaded 5 test cases and 5 test results into MySQL database");
    }
}
