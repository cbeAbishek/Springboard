package org.example.reporting;

import org.example.model.TestBatch;
import org.example.model.TestCase;
import org.example.model.TestExecution;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class ReportGenerator {

    private static final Logger log = LoggerFactory.getLogger(ReportGenerator.class);

    @Value("${automation.framework.reporting.outputPath:test-reports/}")
    private String reportOutputPath;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    /**
     * Generate comprehensive HTML report
     */
    public String generateHTMLReport(TestBatch batch, List<TestExecution> executions) {
        try {
            Path reportDir = Paths.get(reportOutputPath);
            Files.createDirectories(reportDir);

            String timestamp = LocalDateTime.now().format(dateFormatter);
            String fileName = String.format("TestReport_%s_%s.html", batch.getBatchId(), timestamp);
            Path reportPath = reportDir.resolve(fileName);

            StringBuilder html = new StringBuilder();

            // HTML Header
            html.append("<!DOCTYPE html>\n<html>\n<head>\n")
                .append("<meta charset='UTF-8'>\n")
                .append("<title>Test Execution Report - ").append(batch.getName()).append("</title>\n")
                .append("<style>\n")
                .append(getReportCSS())
                .append("</style>\n")
                .append("</head>\n<body>\n");

            // Report Header
            html.append("<div class='header'>\n")
                .append("<h1>Test Execution Report</h1>\n")
                .append("<h2>").append(batch.getName()).append("</h2>\n")
                .append("<div class='batch-info'>\n")
                .append("<p><strong>Batch ID:</strong> ").append(batch.getBatchId()).append("</p>\n")
                .append("<p><strong>Environment:</strong> ").append(batch.getEnvironment()).append("</p>\n")
                .append("<p><strong>Execution Time:</strong> ").append(batch.getCreatedAt()).append("</p>\n")
                .append("<p><strong>Status:</strong> <span class='status-").append(batch.getStatus().toString().toLowerCase()).append("'>")
                .append(batch.getStatus()).append("</span></p>\n")
                .append("</div>\n</div>\n");

            // Summary Statistics
            html.append(generateSummarySection(batch, executions));

            // Test Results Table
            html.append(generateTestResultsTable(executions));

            // Screenshots Section
            html.append(generateScreenshotsSection(executions));

            // API Test Results Section
            html.append(generateAPIResultsSection(executions));

            // Footer
            html.append("</div>\n</body>\n</html>");

            Files.write(reportPath, html.toString().getBytes());

            String absolutePath = reportPath.toAbsolutePath().toString();
            log.info("HTML report generated: {}", absolutePath);
            return absolutePath;

        } catch (Exception e) {
            log.error("Failed to generate HTML report", e);
            return null;
        }
    }

    /**
     * Generate CSV report
     */
    public String generateCSVReport(TestBatch batch, List<TestExecution> executions) {
        try {
            Path reportDir = Paths.get(reportOutputPath);
            Files.createDirectories(reportDir);

            String timestamp = LocalDateTime.now().format(dateFormatter);
            String fileName = String.format("TestReport_%s_%s.csv", batch.getBatchId(), timestamp);
            Path reportPath = reportDir.resolve(fileName);

            try (FileWriter writer = new FileWriter(reportPath.toFile());
                 CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

                // CSV Headers
                csvPrinter.printRecord("Test Case ID", "Test Name", "Test Type", "Status",
                                     "Start Time", "End Time", "Duration (ms)", "Environment",
                                     "Error Message", "Screenshot Path");

                // CSV Data
                for (TestExecution execution : executions) {
                    csvPrinter.printRecord(
                        execution.getTestCase().getId(),
                        execution.getTestCase().getName(),
                        execution.getTestCase().getTestType(),
                        execution.getStatus(),
                        execution.getStartTime(),
                        execution.getEndTime(),
                        execution.getExecutionDuration(),
                        execution.getEnvironment(),
                        execution.getErrorMessage() != null ? execution.getErrorMessage().substring(0, Math.min(200, execution.getErrorMessage().length())) : "",
                        execution.getScreenshotPath()
                    );
                }
            }

            String absolutePath = reportPath.toAbsolutePath().toString();
            log.info("CSV report generated: {}", absolutePath);
            return absolutePath;

        } catch (Exception e) {
            log.error("Failed to generate CSV report", e);
            return null;
        }
    }

    /**
     * Generate XML report (JUnit format)
     */
    public String generateXMLReport(TestBatch batch, List<TestExecution> executions) {
        try {
            Path reportDir = Paths.get(reportOutputPath);
            Files.createDirectories(reportDir);

            String timestamp = LocalDateTime.now().format(dateFormatter);
            String fileName = String.format("TestReport_%s_%s.xml", batch.getBatchId(), timestamp);
            Path reportPath = reportDir.resolve(fileName);

            StringBuilder xml = new StringBuilder();

            // XML Header
            xml.append("<?xml version='1.0' encoding='UTF-8'?>\n");
            xml.append("<testsuite name='").append(batch.getName()).append("' ");
            xml.append("tests='").append(executions.size()).append("' ");
            xml.append("failures='").append(batch.getFailedTests()).append("' ");
            xml.append("skipped='").append(batch.getSkippedTests()).append("' ");
            xml.append("time='").append(calculateTotalDuration(executions)).append("'>\n");

            // Test Cases
            for (TestExecution execution : executions) {
                xml.append("  <testcase classname='").append(execution.getTestCase().getTestType()).append("' ");
                xml.append("name='").append(escapeXml(execution.getTestCase().getName())).append("' ");
                xml.append("time='").append(execution.getExecutionDuration() != null ? execution.getExecutionDuration() / 1000.0 : 0).append("'");

                if (execution.getStatus() == TestExecution.ExecutionStatus.FAILED) {
                    xml.append(">\n");
                    xml.append("    <failure message='").append(escapeXml(execution.getErrorMessage())).append("'>");
                    xml.append(escapeXml(execution.getStackTrace()));
                    xml.append("</failure>\n");
                    xml.append("  </testcase>\n");
                } else if (execution.getStatus() == TestExecution.ExecutionStatus.SKIPPED) {
                    xml.append(">\n");
                    xml.append("    <skipped/>\n");
                    xml.append("  </testcase>\n");
                } else {
                    xml.append("/>\n");
                }
            }

            xml.append("</testsuite>\n");

            Files.write(reportPath, xml.toString().getBytes());

            String absolutePath = reportPath.toAbsolutePath().toString();
            log.info("XML report generated: {}", absolutePath);
            return absolutePath;

        } catch (Exception e) {
            log.error("Failed to generate XML report", e);
            return null;
        }
    }

    /**
     * Generate summary section for HTML report
     */
    private String generateSummarySection(TestBatch batch, List<TestExecution> executions) {
        StringBuilder html = new StringBuilder();

        html.append("<div class='summary'>\n")
            .append("<h3>Test Summary</h3>\n")
            .append("<div class='summary-cards'>\n");

        // Total Tests Card
        html.append("<div class='card'>\n")
            .append("<h4>Total Tests</h4>\n")
            .append("<p class='number'>").append(executions.size()).append("</p>\n")
            .append("</div>\n");

        // Passed Tests Card
        long passedCount = executions.stream().mapToLong(e -> e.getStatus() == TestExecution.ExecutionStatus.PASSED ? 1 : 0).sum();
        html.append("<div class='card passed'>\n")
            .append("<h4>Passed</h4>\n")
            .append("<p class='number'>").append(passedCount).append("</p>\n")
            .append("</div>\n");

        // Failed Tests Card
        long failedCount = executions.stream().mapToLong(e -> e.getStatus() == TestExecution.ExecutionStatus.FAILED ? 1 : 0).sum();
        html.append("<div class='card failed'>\n")
            .append("<h4>Failed</h4>\n")
            .append("<p class='number'>").append(failedCount).append("</p>\n")
            .append("</div>\n");

        // Skipped Tests Card
        long skippedCount = executions.stream().mapToLong(e -> e.getStatus() == TestExecution.ExecutionStatus.SKIPPED ? 1 : 0).sum();
        html.append("<div class='card skipped'>\n")
            .append("<h4>Skipped</h4>\n")
            .append("<p class='number'>").append(skippedCount).append("</p>\n")
            .append("</div>\n");

        // Pass Rate Card
        double passRate = executions.size() > 0 ? (double) passedCount / executions.size() * 100 : 0;
        html.append("<div class='card'>\n")
            .append("<h4>Pass Rate</h4>\n")
            .append("<p class='number'>").append(String.format("%.1f%%", passRate)).append("</p>\n")
            .append("</div>\n");

        html.append("</div>\n</div>\n");

        return html.toString();
    }

    /**
     * Generate test results table
     */
    private String generateTestResultsTable(List<TestExecution> executions) {
        StringBuilder html = new StringBuilder();

        html.append("<div class='test-results'>\n")
            .append("<h3>Test Results</h3>\n")
            .append("<table class='results-table'>\n")
            .append("<thead>\n")
            .append("<tr>\n")
            .append("<th>Test Case</th>\n")
            .append("<th>Type</th>\n")
            .append("<th>Status</th>\n")
            .append("<th>Duration</th>\n")
            .append("<th>Start Time</th>\n")
            .append("<th>Screenshot</th>\n")
            .append("<th>Error</th>\n")
            .append("</tr>\n")
            .append("</thead>\n")
            .append("<tbody>\n");

        for (TestExecution execution : executions) {
            html.append("<tr class='").append(execution.getStatus().toString().toLowerCase()).append("'>\n")
                .append("<td>").append(execution.getTestCase().getName()).append("</td>\n")
                .append("<td>").append(execution.getTestCase().getTestType()).append("</td>\n")
                .append("<td><span class='status-").append(execution.getStatus().toString().toLowerCase()).append("'>")
                .append(execution.getStatus()).append("</span></td>\n")
                .append("<td>").append(formatDuration(execution.getExecutionDuration())).append("</td>\n")
                .append("<td>").append(execution.getStartTime()).append("</td>\n");

            // Screenshot link
            if (execution.getScreenshotPath() != null) {
                String screenshotName = Paths.get(execution.getScreenshotPath()).getFileName().toString();
                html.append("<td><a href='screenshots/").append(screenshotName).append("' target='_blank'>View</a></td>\n");
            } else {
                html.append("<td>-</td>\n");
            }

            // Error message
            if (execution.getErrorMessage() != null) {
                String shortError = execution.getErrorMessage().length() > 100 ?
                    execution.getErrorMessage().substring(0, 100) + "..." : execution.getErrorMessage();
                html.append("<td title='").append(escapeHtml(execution.getErrorMessage())).append("'>")
                    .append(escapeHtml(shortError)).append("</td>\n");
            } else {
                html.append("<td>-</td>\n");
            }

            html.append("</tr>\n");
        }

        html.append("</tbody>\n</table>\n</div>\n");

        return html.toString();
    }

    /**
     * Generate screenshots section
     */
    private String generateScreenshotsSection(List<TestExecution> executions) {
        StringBuilder html = new StringBuilder();

        List<TestExecution> executionsWithScreenshots = executions.stream()
            .filter(e -> e.getScreenshotPath() != null)
            .collect(java.util.stream.Collectors.toList());

        if (!executionsWithScreenshots.isEmpty()) {
            html.append("<div class='screenshots'>\n")
                .append("<h3>Screenshots</h3>\n")
                .append("<div class='screenshot-gallery'>\n");

            for (TestExecution execution : executionsWithScreenshots) {
                String screenshotName = Paths.get(execution.getScreenshotPath()).getFileName().toString();
                html.append("<div class='screenshot-item'>\n")
                    .append("<h5>").append(execution.getTestCase().getName()).append("</h5>\n")
                    .append("<a href='screenshots/").append(screenshotName).append("' target='_blank'>\n")
                    .append("<img src='screenshots/").append(screenshotName).append("' alt='Screenshot' class='screenshot-thumb'>\n")
                    .append("</a>\n")
                    .append("<p>Status: ").append(execution.getStatus()).append("</p>\n")
                    .append("</div>\n");
            }

            html.append("</div>\n</div>\n");
        }

        return html.toString();
    }

    /**
     * Generate API results section
     */
    private String generateAPIResultsSection(List<TestExecution> executions) {
        StringBuilder html = new StringBuilder();

        List<TestExecution> apiExecutions = executions.stream()
            .filter(e -> e.getTestCase().getTestType() == TestCase.TestType.API ||
                        e.getTestCase().getName().contains("ReqRes"))
            .collect(java.util.stream.Collectors.toList());

        if (!apiExecutions.isEmpty()) {
            html.append("<div class='api-results'>\n")
                .append("<h3>API Test Results</h3>\n")
                .append("<table class='api-table'>\n")
                .append("<thead>\n")
                .append("<tr>\n")
                .append("<th>API Test</th>\n")
                .append("<th>Method</th>\n")
                .append("<th>Status</th>\n")
                .append("<th>Response Time</th>\n")
                .append("<th>Result</th>\n")
                .append("</tr>\n")
                .append("</thead>\n")
                .append("<tbody>\n");

            for (TestExecution execution : apiExecutions) {
                // Parse test data to extract API info
                String method = "GET";
                String responseTime = formatDuration(execution.getExecutionDuration());

                try {
                    Map<String, Object> testData = objectMapper.readValue(execution.getTestCase().getTestData(), Map.class);
                    method = (String) testData.getOrDefault("method", "GET");
                } catch (Exception e) {
                    // Use default
                }

                html.append("<tr class='").append(execution.getStatus().toString().toLowerCase()).append("'>\n")
                    .append("<td>").append(execution.getTestCase().getName()).append("</td>\n")
                    .append("<td>").append(method).append("</td>\n")
                    .append("<td><span class='status-").append(execution.getStatus().toString().toLowerCase()).append("'>")
                    .append(execution.getStatus()).append("</span></td>\n")
                    .append("<td>").append(responseTime).append("</td>\n")
                    .append("<td>").append(execution.getActualResult() != null ? execution.getActualResult() : "-").append("</td>\n")
                    .append("</tr>\n");
            }

            html.append("</tbody>\n</table>\n</div>\n");
        }

        return html.toString();
    }

    /**
     * Get CSS for HTML report
     */
    private String getReportCSS() {
        return """
            body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }
            .header { background: #fff; padding: 20px; border-radius: 8px; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
            .header h1 { color: #333; margin: 0; }
            .header h2 { color: #666; margin: 10px 0; }
            .batch-info p { margin: 5px 0; }
            .summary { background: #fff; padding: 20px; border-radius: 8px; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
            .summary-cards { display: flex; gap: 20px; flex-wrap: wrap; }
            .card { flex: 1; min-width: 150px; padding: 20px; border-radius: 8px; text-align: center; background: #f8f9fa; }
            .card.passed { background: #d4edda; }
            .card.failed { background: #f8d7da; }
            .card.skipped { background: #ffeaa7; }
            .card h4 { margin: 0 0 10px 0; color: #333; }
            .card .number { font-size: 2em; font-weight: bold; margin: 0; }
            .test-results { background: #fff; padding: 20px; border-radius: 8px; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
            .results-table { width: 100%; border-collapse: collapse; }
            .results-table th, .results-table td { padding: 12px; text-align: left; border-bottom: 1px solid #ddd; }
            .results-table th { background-color: #f8f9fa; font-weight: bold; }
            .results-table tr.passed { background-color: #f8fff8; }
            .results-table tr.failed { background-color: #fff8f8; }
            .results-table tr.skipped { background-color: #fffef8; }
            .status-passed { color: #28a745; font-weight: bold; }
            .status-failed { color: #dc3545; font-weight: bold; }
            .status-skipped { color: #ffc107; font-weight: bold; }
            .status-running { color: #007bff; font-weight: bold; }
            .screenshots { background: #fff; padding: 20px; border-radius: 8px; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
            .screenshot-gallery { display: flex; gap: 20px; flex-wrap: wrap; }
            .screenshot-item { text-align: center; border: 1px solid #ddd; padding: 10px; border-radius: 8px; }
            .screenshot-thumb { max-width: 200px; max-height: 150px; border: 1px solid #ccc; }
            .api-results { background: #fff; padding: 20px; border-radius: 8px; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
            .api-table { width: 100%; border-collapse: collapse; }
            .api-table th, .api-table td { padding: 12px; text-align: left; border-bottom: 1px solid #ddd; }
            .api-table th { background-color: #f8f9fa; font-weight: bold; }
            a { color: #007bff; text-decoration: none; }
            a:hover { text-decoration: underline; }
            """;
    }

    // Utility methods
    private String formatDuration(Long duration) {
        if (duration == null) return "-";
        if (duration < 1000) return duration + "ms";
        return String.format("%.2fs", duration / 1000.0);
    }

    private long calculateTotalDuration(List<TestExecution> executions) {
        return executions.stream()
            .mapToLong(e -> e.getExecutionDuration() != null ? e.getExecutionDuration() : 0)
            .sum();
    }

    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&apos;");
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;");
    }
}
