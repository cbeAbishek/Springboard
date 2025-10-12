package org.example.service;

import org.example.model.TestExecution;
import org.example.model.TestBatch;
import org.example.reporting.ReportGenerator;
import org.example.repository.TestExecutionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    @Autowired
    private ReportGenerator reportGenerator;

    @Autowired
    private TestExecutionRepository testExecutionRepository;

    public String generateComprehensiveReport(List<Long> executionIds, String reportFormat, String reportType) throws IOException {
        List<TestExecution> executions = testExecutionRepository.findAllById(executionIds);
        
        // Create a dummy TestBatch for report generation
        TestBatch dummyBatch = createDummyBatch(executions, reportType);

        switch (reportFormat.toLowerCase()) {
            case "html":
                return reportGenerator.generateHTMLReport(dummyBatch, executions);
            case "csv":
                return reportGenerator.generateCSVReport(dummyBatch, executions);
            case "xml":
                return reportGenerator.generateXMLReport(dummyBatch, executions);
            default:
                throw new IllegalArgumentException("Unsupported report format: " + reportFormat);
        }
    }

    public String generateLatestExecutionsReport(String reportFormat, int limit) throws IOException {
        List<TestExecution> latestExecutions = testExecutionRepository.findTop10ByOrderByStartTimeDesc();
        
        if (limit > 0 && latestExecutions.size() > limit) {
            latestExecutions = latestExecutions.subList(0, limit);
        }

        return generateComprehensiveReport(
            latestExecutions.stream().map(TestExecution::getId).collect(java.util.stream.Collectors.toList()),
            reportFormat,
            "latest_executions"
        );
    }

    public String generateExecutionsByStatus(TestExecution.ExecutionStatus status, String reportFormat) throws IOException {
        List<TestExecution> executions = testExecutionRepository.findByStatus(status);

        return generateComprehensiveReport(
            executions.stream().map(TestExecution::getId).collect(java.util.stream.Collectors.toList()),
            reportFormat,
            "executions_by_status_" + status.toString().toLowerCase()
        );
    }

    public String generateExecutionsByEnvironment(String environment, String reportFormat) throws IOException {
        List<TestExecution> executions = testExecutionRepository.findByEnvironment(environment);

        return generateComprehensiveReport(
            executions.stream().map(TestExecution::getId).collect(java.util.stream.Collectors.toList()),
            reportFormat,
            "executions_by_environment_" + environment
        );
    }

    // Add missing methods for ReportController
    public String generateFailedTestsReport(String reportFormat) throws IOException {
        return generateExecutionsByStatus(TestExecution.ExecutionStatus.FAILED, reportFormat);
    }

    public String generateEnvironmentReport(String environment, String reportFormat) throws IOException {
        return generateExecutionsByEnvironment(environment, reportFormat);
    }

    private TestBatch createDummyBatch(List<TestExecution> executions, String reportType) {
        TestBatch batch = new TestBatch();
        batch.setBatchId("REPORT_" + System.currentTimeMillis());
        batch.setName("Generated Report: " + reportType);
        batch.setDescription("Report generated from " + executions.size() + " executions");
        batch.setStatus(TestBatch.BatchStatus.COMPLETED);
        batch.setTotalTests(executions.size());

        long passed = executions.stream().mapToLong(e -> e.getStatus() == TestExecution.ExecutionStatus.PASSED ? 1 : 0).sum();
        long failed = executions.stream().mapToLong(e -> e.getStatus() == TestExecution.ExecutionStatus.FAILED ? 1 : 0).sum();
        long skipped = executions.stream().mapToLong(e -> e.getStatus() == TestExecution.ExecutionStatus.SKIPPED ? 1 : 0).sum();

        batch.setPassedTests((int) passed);
        batch.setFailedTests((int) failed);
        batch.setSkippedTests((int) skipped);
        batch.setEnvironment(executions.isEmpty() ? "unknown" : executions.get(0).getEnvironment());
        batch.setCreatedBy("report_service");

        return batch;
    }
}
