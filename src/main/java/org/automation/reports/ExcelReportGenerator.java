package org.automation.reports;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Generates Excel reports from test execution results
 */
public class ExcelReportGenerator {

    private static final Logger logger = LoggerFactory.getLogger(ExcelReportGenerator.class);
    private static final String OUTPUT_DIR = "artifacts/reports";
    private static final String EXCEL_FILE = OUTPUT_DIR + "/test-report.xlsx";

    public static void generateReport() {
        try {
            // Create output directory if it doesn't exist
            Path outputPath = Paths.get(OUTPUT_DIR);
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
            }

            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Test Results");

                // Create header row
                Row headerRow = sheet.createRow(0);
                CellStyle headerStyle = workbook.createCellStyle();
                Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);

                String[] headers = {"Test Name", "Status", "Duration (ms)", "Timestamp", "Error Message"};
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(headerStyle);
                }

                // Note: In a real implementation, you would read test results from TestNG
                // or a database and write them here
                Row dataRow = sheet.createRow(1);
                dataRow.createCell(0).setCellValue("Sample Test");
                dataRow.createCell(1).setCellValue("PASS");
                dataRow.createCell(2).setCellValue(1000);
                dataRow.createCell(3).setCellValue(System.currentTimeMillis());
                dataRow.createCell(4).setCellValue("");

                // Auto-size columns
                for (int i = 0; i < headers.length; i++) {
                    sheet.autoSizeColumn(i);
                }

                // Write to file
                try (FileOutputStream outputStream = new FileOutputStream(EXCEL_FILE)) {
                    workbook.write(outputStream);
                }

                logger.info("Excel report generated successfully: {}", EXCEL_FILE);
            }

        } catch (IOException e) {
            logger.error("Error generating Excel report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate Excel report", e);
        }
    }
}
