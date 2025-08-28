package org.example.mavendemo;

import org.example.mavendemo.model.TestCase;
import org.example.mavendemo.model.TestResult;
import org.example.mavendemo.repository.TestCaseRepository;
import org.example.mavendemo.repository.TestResultRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class TestDataLoader {

    @Bean
    CommandLineRunner run(TestCaseRepository testCaseRepo, TestResultRepository testResultRepo) {
        return args -> {
            // Create a TestCase
            TestCase testCase = new TestCase("Login Test", "UI", "Checks login functionality", "Pending");
            testCase = testCaseRepo.save(testCase);

            System.out.println("Saved TestCase: " + testCase.getId() + " -> " + testCase.getName());

            // Create a TestResult linked to that TestCase
            TestResult result = new TestResult(testCase.getId(), "PASSED", LocalDateTime.now());
            result = testResultRepo.save(result);

            System.out.println("Saved TestResult: " + result.getId() + " for TestCaseId=" + result.getTestCaseId());
        };
    }
}
