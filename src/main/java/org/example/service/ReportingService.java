package org.example.service;

import org.example.model.TestBatch;
import org.example.model.TestExecution;
import org.example.repository.TestBatchRepository;
import org.example.repository.TestExecutionRepository;
import org.example.reporting.ReportGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class ReportingService {

    private static final Logger log = LoggerFactory.getLogger(ReportingService.class);

    @Autowired
    private ReportGenerator reportGenerator;

    @Autowired
    private TestBatchRepository testBatchRepository;

    @Autowired
    private TestExecutionRepository testExecutionRepository;

    @Async("reportingExecutor")
    public CompletableFuture<ReportResult> generateAllReports(String batchId) {
        try {
            TestBatch batch = testBatchRepository.findByBatchId(batchId)
                    .orElseThrow(() -> new RuntimeException("Batch not found: " + batchId));

            List<TestExecution> executions = testExecutionRepository.findByTestBatchId(batch.getId());

            ReportResult result = new ReportResult();
            result.setBatchId(batchId);

            // Generate HTML report
            String htmlReport = reportGenerator.generateHtmlReport(batch, executions);
            result.setHtmlReportPath(htmlReport);

            // Generate CSV report
            String csvReport = reportGenerator.generateCsvReport(batch, executions);
            result.setCsvReportPath(csvReport);

            // Generate JUnit report
            String junitReport = reportGenerator.generateJunitReport(batch, executions);
            result.setJunitReportPath(junitReport);

            log.info("All reports generated successfully for batch: {}", batchId);
            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            log.error("Failed to generate reports for batch: {}", batchId, e);
            throw new RuntimeException("Report generation failed", e);
        }
    }

    public ReportResult generateHtmlReport(String batchId) {
        try {
            TestBatch batch = testBatchRepository.findByBatchId(batchId)
                    .orElseThrow(() -> new RuntimeException("Batch not found: " + batchId));

            List<TestExecution> executions = testExecutionRepository.findByTestBatchId(batch.getId());
            String htmlReport = reportGenerator.generateHtmlReport(batch, executions);

            ReportResult result = new ReportResult();
            result.setBatchId(batchId);
            result.setHtmlReportPath(htmlReport);

            return result;
        } catch (Exception e) {
            log.error("Failed to generate HTML report for batch: {}", batchId, e);
            throw new RuntimeException("HTML report generation failed", e);
        }
    }

    public static class ReportResult {
        private String batchId;
        private String htmlReportPath;
        private String csvReportPath;
        private String junitReportPath;

        // Getters and setters
        public String getBatchId() { return batchId; }
        public void setBatchId(String batchId) { this.batchId = batchId; }

        public String getHtmlReportPath() { return htmlReportPath; }
        public void setHtmlReportPath(String htmlReportPath) { this.htmlReportPath = htmlReportPath; }

        public String getCsvReportPath() { return csvReportPath; }
        public void setCsvReportPath(String csvReportPath) { this.csvReportPath = csvReportPath; }

        public String getJunitReportPath() { return junitReportPath; }
        public void setJunitReportPath(String junitReportPath) { this.junitReportPath = junitReportPath; }
    }
}
