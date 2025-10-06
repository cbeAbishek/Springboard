package org.automation.listeners;

import org.automation.reports.CsvReportGenerator;
import org.automation.reports.ExcelReportGenerator;
import org.automation.reports.HtmlReportGenerator;
import org.testng.ISuite;
import org.testng.ISuiteListener;

public class ReportListener implements ISuiteListener {

    @Override
    public void onFinish(ISuite suite) {
        try {
            System.out.println("[ReportListener] ✅ Generating reports...");

            // Try to generate reports, but don't fail if database is unavailable
            try {
                HtmlReportGenerator.generateReport();
                System.out.println("[ReportListener] 📊 HTML report generated successfully!");
            } catch (Exception e) {
                System.out.println("[ReportListener] ⚠️ HTML report skipped (Database not available)");
            }

            try {
                CsvReportGenerator.generateReport();
                System.out.println("[ReportListener] 📊 CSV report generated successfully!");
            } catch (Exception e) {
                System.out.println("[ReportListener] ⚠️ CSV report skipped (Database not available)");
            }

            try {
                ExcelReportGenerator.generateReport();
                System.out.println("[ReportListener] 📊 Excel report generated successfully!");
            } catch (Exception e) {
                System.out.println("[ReportListener] ⚠️ Excel report skipped (Database not available)");
            }

            System.out.println("[ReportListener] ℹ️ Allure results available in: allure-results/");
            System.out.println("[ReportListener] ℹ️ Generate Allure report with: mvn allure:report or mvn allure:serve");

        } catch (Exception e) {
            System.err.println("[ReportListener] ❌ Error during report generation: " + e.getMessage());
        }
    }
}
