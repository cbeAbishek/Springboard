package org.example.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.Status;
import org.example.model.TestBatch;
import org.example.model.TestExecution;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class ReportGenerator {

    private static final Logger log = LoggerFactory.getLogger(ReportGenerator.class);
    private static final String REPORTS_DIR = "test-reports/";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    public String generateHtmlReport(TestBatch batch, List<TestExecution> executions) {
        try {
            // Create reports directory
            Files.createDirectories(Paths.get(REPORTS_DIR));

            String reportName = String.format("TestReport_%s_%s.html",
                    batch.getBatchId(), batch.getStartTime().format(DATE_FORMAT));
            String reportPath = REPORTS_DIR + reportName;

            // Configure ExtentReports
            ExtentSparkReporter sparkReporter = new ExtentSparkReporter(reportPath);
            sparkReporter.config().setDocumentTitle("Automation Test Report");
            sparkReporter.config().setReportName("Regression Test Results");

            ExtentReports extent = new ExtentReports();
            extent.attachReporter(sparkReporter);

            // Add system info
            extent.setSystemInfo("Environment", batch.getEnvironment());
            extent.setSystemInfo("Batch ID", batch.getBatchId());
            extent.setSystemInfo("Start Time", batch.getStartTime().toString());
            extent.setSystemInfo("End Time", batch.getEndTime() != null ? batch.getEndTime().toString() : "Running");
            extent.setSystemInfo("Total Tests", String.valueOf(batch.getTotalTests()));
            extent.setSystemInfo("Passed", String.valueOf(batch.getPassedTests()));
            extent.setSystemInfo("Failed", String.valueOf(batch.getFailedTests()));
            extent.setSystemInfo("Skipped", String.valueOf(batch.getSkippedTests()));

            // Add test results
            for (TestExecution execution : executions) {
                ExtentTest test = extent.createTest(execution.getTestCase().getName())
                        .assignCategory(execution.getTestCase().getTestType().toString())
                        .assignAuthor("Automation Framework");

                switch (execution.getStatus()) {
                    case PASSED:
                        test.log(Status.PASS, "Test passed successfully");
                        break;
                    case FAILED:
                        test.log(Status.FAIL, "Test failed: " + execution.getErrorMessage());
                        if (execution.getScreenshotPath() != null) {
                            test.addScreenCaptureFromPath(execution.getScreenshotPath());
                        }
                        break;
                    case ERROR:
                        test.log(Status.INFO, "Test error: " + execution.getErrorMessage());
                        break;
                    case SKIPPED:
                        test.log(Status.SKIP, "Test skipped");
                        break;
                }

                test.info("Duration: " + execution.getExecutionDuration() + "ms");
                test.info("Environment: " + execution.getEnvironment());
                if (execution.getBrowser() != null) {
                    test.info("Browser: " + execution.getBrowser());
                }
            }

            extent.flush();
            log.info("HTML report generated: {}", reportPath);
            return reportPath;

        } catch (Exception e) {
            log.error("Failed to generate HTML report", e);
            return null;
        }
    }

    public String generateCsvReport(TestBatch batch, List<TestExecution> executions) {
        try {
            Files.createDirectories(Paths.get(REPORTS_DIR));

            String reportName = String.format("TestReport_%s_%s.csv",
                    batch.getBatchId(), batch.getStartTime().format(DATE_FORMAT));
            String reportPath = REPORTS_DIR + reportName;

            try (FileWriter writer = new FileWriter(reportPath)) {
                // Write CSV header
                writer.append("Test Case,Test Type,Status,Start Time,End Time,Duration (ms),Environment,Browser,Error Message\n");

                // Write test results
                for (TestExecution execution : executions) {
                    writer.append(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%d,\"%s\",\"%s\",\"%s\"\n",
                            execution.getTestCase().getName(),
                            execution.getTestCase().getTestType(),
                            execution.getStatus(),
                            execution.getStartTime(),
                            execution.getEndTime(),
                            execution.getExecutionDuration(),
                            execution.getEnvironment(),
                            execution.getBrowser() != null ? execution.getBrowser() : "",
                            execution.getErrorMessage() != null ? execution.getErrorMessage().replace("\"", "\"\"") : ""
                    ));
                }
            }

            log.info("CSV report generated: {}", reportPath);
            return reportPath;

        } catch (IOException e) {
            log.error("Failed to generate CSV report", e);
            return null;
        }
    }

    public String generateJunitReport(TestBatch batch, List<TestExecution> executions) {
        try {
            Files.createDirectories(Paths.get(REPORTS_DIR));

            String reportName = String.format("TestReport_%s_%s.xml",
                    batch.getBatchId(), batch.getStartTime().format(DATE_FORMAT));
            String reportPath = REPORTS_DIR + reportName;

            StringBuilder xml = new StringBuilder();
            xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            xml.append(String.format("<testsuite name=\"%s\" tests=\"%d\" failures=\"%d\" errors=\"%d\" skipped=\"%d\" time=\"%.3f\">\n",
                    batch.getBatchName(),
                    batch.getTotalTests(),
                    batch.getFailedTests(),
                    0, // errors counted separately in our model
                    batch.getSkippedTests(),
                    calculateTotalDuration(executions) / 1000.0
            ));

            for (TestExecution execution : executions) {
                xml.append(String.format("  <testcase name=\"%s\" classname=\"%s\" time=\"%.3f\"",
                        execution.getTestCase().getName(),
                        execution.getTestCase().getTestType(),
                        execution.getExecutionDuration() / 1000.0
                ));

                if (execution.getStatus() == TestExecution.ExecutionStatus.FAILED) {
                    xml.append(">\n");
                    xml.append(String.format("    <failure message=\"%s\">%s</failure>\n",
                            escapeXml(execution.getErrorMessage()),
                            escapeXml(execution.getStackTrace())
                    ));
                    xml.append("  </testcase>\n");
                } else if (execution.getStatus() == TestExecution.ExecutionStatus.SKIPPED) {
                    xml.append(">\n    <skipped/>\n  </testcase>\n");
                } else {
                    xml.append("/>\n");
                }
            }

            xml.append("</testsuite>");

            Files.write(Paths.get(reportPath), xml.toString().getBytes());

            log.info("JUnit report generated: {}", reportPath);
            return reportPath;

        } catch (Exception e) {
            log.error("Failed to generate JUnit report", e);
            return null;
        }
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
}
