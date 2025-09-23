package org.example.analytics;

import org.example.model.TestExecution;
import org.example.repository.TestExecutionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    @Autowired
    private TestExecutionRepository testExecutionRepository;

    public Map<String, Object> getExecutionTrends(LocalDateTime startDate, LocalDateTime endDate) {
        List<TestExecution> executions = testExecutionRepository.findByDateRange(startDate, endDate);

        Map<String, Object> trends = new HashMap<>();
        trends.put("totalExecutions", executions.size());

        long passed = executions.stream()
            .mapToLong(e -> e.getStatus() == TestExecution.ExecutionStatus.PASSED ? 1 : 0)
            .sum();
        long failed = executions.stream()
            .mapToLong(e -> e.getStatus() == TestExecution.ExecutionStatus.FAILED ? 1 : 0)
            .sum();
        long skipped = executions.stream()
            .mapToLong(e -> e.getStatus() == TestExecution.ExecutionStatus.SKIPPED ? 1 : 0)
            .sum();

        trends.put("passed", passed);
        trends.put("failed", failed);
        trends.put("skipped", skipped);
        trends.put("passRate", executions.size() > 0 ? (double) passed / executions.size() * 100 : 0);

        // Group by test type
        Map<String, Long> byTestType = executions.stream()
            .collect(Collectors.groupingBy(
                e -> e.getTestCase().getTestType().toString(),
                Collectors.counting()
            ));
        trends.put("byTestType", byTestType);

        // Group by environment
        Map<String, Long> byEnvironment = executions.stream()
            .collect(Collectors.groupingBy(
                TestExecution::getEnvironment,
                Collectors.counting()
            ));
        trends.put("byEnvironment", byEnvironment);

        return trends;
    }

    public Map<String, Object> getTestCaseAnalytics() {
        Map<String, Object> analytics = new HashMap<>();

        List<TestExecution> allExecutions = testExecutionRepository.findAll();

        // Most failing test cases
        Map<String, Long> failingTests = allExecutions.stream()
            .filter(e -> e.getStatus() == TestExecution.ExecutionStatus.FAILED)
            .collect(Collectors.groupingBy(
                e -> e.getTestCase().getName(),
                Collectors.counting()
            ));
        analytics.put("mostFailingTests", failingTests);

        // Average execution duration by test type
        Map<String, Double> avgDurationByType = allExecutions.stream()
            .filter(e -> e.getExecutionDuration() != null)
            .collect(Collectors.groupingBy(
                e -> e.getTestCase().getTestType().toString(),
                Collectors.averagingLong(TestExecution::getExecutionDuration)
            ));
        analytics.put("avgDurationByType", avgDurationByType);

        return analytics;
    }

    public Map<String, Object> getPerformanceMetrics(LocalDateTime startDate, LocalDateTime endDate) {
        List<TestExecution> executions = testExecutionRepository.findByDateRange(startDate, endDate);

        Map<String, Object> metrics = new HashMap<>();

        // Average execution time
        double avgExecutionTime = executions.stream()
            .filter(e -> e.getExecutionDuration() != null)
            .mapToLong(TestExecution::getExecutionDuration)
            .average()
            .orElse(0.0);
        metrics.put("avgExecutionTime", avgExecutionTime);

        // Longest running tests
        List<Map<String, Object>> longestTests = executions.stream()
            .filter(e -> e.getExecutionDuration() != null)
            .sorted((e1, e2) -> Long.compare(e2.getExecutionDuration(), e1.getExecutionDuration()))
            .limit(10)
            .map(e -> {
                Map<String, Object> testInfo = new HashMap<>();
                testInfo.put("testName", e.getTestCase().getName());
                testInfo.put("duration", e.getExecutionDuration());
                testInfo.put("environment", e.getEnvironment());
                return testInfo;
            })
            .collect(Collectors.toList());
        metrics.put("longestRunningTests", longestTests);

        return metrics;
    }
}
