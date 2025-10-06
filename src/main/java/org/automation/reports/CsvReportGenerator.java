package org.automation.reports;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Generates CSV reports from test execution results
 */
public class CsvReportGenerator {

    private static final Logger logger = LoggerFactory.getLogger(CsvReportGenerator.class);
    private static final String OUTPUT_DIR = "artifacts/reports";
    private static final String CSV_FILE = OUTPUT_DIR + "/test-report.csv";

    public static void generateReport() {
        try {
            // Create output directory if it doesn't exist
            Path outputPath = Paths.get(OUTPUT_DIR);
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
            }

            try (FileWriter writer = new FileWriter(CSV_FILE)) {
                // Write CSV header
                writer.append("Test Name,Status,Duration (ms),Timestamp,Error Message\n");

                // Note: In a real implementation, you would read test results from TestNG
                // or a database and write them here
                writer.append("Sample Test,PASS,1000," + System.currentTimeMillis() + ",\n");

                logger.info("CSV report generated successfully: {}", CSV_FILE);
            }

        } catch (IOException e) {
            logger.error("Error generating CSV report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate CSV report", e);
        }
    }
}
