package org.automation.reports;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Generates HTML reports from test execution results
 */
public class HtmlReportGenerator {

    private static final Logger logger = LoggerFactory.getLogger(HtmlReportGenerator.class);
    private static final String OUTPUT_DIR = "artifacts/reports";
    private static final String HTML_FILE = OUTPUT_DIR + "/test-report.html";

    public static void generateReport() {
        try {
            // Create output directory if it doesn't exist
            Path outputPath = Paths.get(OUTPUT_DIR);
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
            }

            try (FileWriter writer = new FileWriter(HTML_FILE)) {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                writer.write("<!DOCTYPE html>\n");
                writer.write("<html lang=\"en\">\n");
                writer.write("<head>\n");
                writer.write("    <meta charset=\"UTF-8\">\n");
                writer.write("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
                writer.write("    <title>Test Execution Report</title>\n");
                writer.write("    <style>\n");
                writer.write("        body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }\n");
                writer.write("        .container { max-width: 1200px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n");
                writer.write("        h1 { color: #333; border-bottom: 3px solid #4CAF50; padding-bottom: 10px; }\n");
                writer.write("        table { width: 100%; border-collapse: collapse; margin-top: 20px; }\n");
                writer.write("        th, td { padding: 12px; text-align: left; border-bottom: 1px solid #ddd; }\n");
                writer.write("        th { background-color: #4CAF50; color: white; }\n");
                writer.write("        tr:hover { background-color: #f5f5f5; }\n");
                writer.write("        .pass { color: #4CAF50; font-weight: bold; }\n");
                writer.write("        .fail { color: #f44336; font-weight: bold; }\n");
                writer.write("        .skip { color: #FF9800; font-weight: bold; }\n");
                writer.write("        .timestamp { color: #666; font-size: 0.9em; }\n");
                writer.write("    </style>\n");
                writer.write("</head>\n");
                writer.write("<body>\n");
                writer.write("    <div class=\"container\">\n");
                writer.write("        <h1>Test Execution Report</h1>\n");
                writer.write("        <p class=\"timestamp\">Generated: " + timestamp + "</p>\n");
                writer.write("        <table>\n");
                writer.write("            <thead>\n");
                writer.write("                <tr>\n");
                writer.write("                    <th>Test Name</th>\n");
                writer.write("                    <th>Status</th>\n");
                writer.write("                    <th>Duration (ms)</th>\n");
                writer.write("                    <th>Timestamp</th>\n");
                writer.write("                    <th>Error Message</th>\n");
                writer.write("                </tr>\n");
                writer.write("            </thead>\n");
                writer.write("            <tbody>\n");

                // Note: In a real implementation, you would read test results from TestNG
                // or a database and write them here
                writer.write("                <tr>\n");
                writer.write("                    <td>Sample Test</td>\n");
                writer.write("                    <td class=\"pass\">PASS</td>\n");
                writer.write("                    <td>1000</td>\n");
                writer.write("                    <td>" + timestamp + "</td>\n");
                writer.write("                    <td>-</td>\n");
                writer.write("                </tr>\n");

                writer.write("            </tbody>\n");
                writer.write("        </table>\n");
                writer.write("    </div>\n");
                writer.write("</body>\n");
                writer.write("</html>\n");

                logger.info("HTML report generated successfully: {}", HTML_FILE);
            }

        } catch (IOException e) {
            logger.error("Error generating HTML report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate HTML report", e);
        }
    }
}
