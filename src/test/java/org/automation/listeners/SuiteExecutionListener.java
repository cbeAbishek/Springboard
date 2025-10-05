package org.automation.listeners;

import org.automation.ui.BaseTest;
import org.automation.reports.ReportManager;
import org.automation.reports.model.TestReport;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Suite-level listener to manage report lifecycle
 */
public class SuiteExecutionListener extends BaseTest implements ISuiteListener {

    private static final Logger logger = LoggerFactory.getLogger(SuiteExecutionListener.class);
    private static ReportManager reportManager;

    static {
        try {
            reportManager = ReportManager.getInstance();
        } catch (Exception e) {
            logger.warn("Could not initialize ReportManager", e);
        }
    }

    @Override
    public void onStart(ISuite suite) {
        String suiteName = suite.getName();
        logger.info("=================================================");
        logger.info("Suite Execution Started: {}", suiteName);
        logger.info("=================================================");

        // Initialize report for this suite
        if (reportManager != null) {
            try {
                String suiteType = determineSuiteType(suiteName);
                String executionMode = detectExecutionMode();

                TestReport report = reportManager.initializeReport(suiteType, executionMode);
                report.setReportName(suiteName);

                // Set browser if available
                String browser = System.getProperty("browser");
                if (browser != null) {
                    report.setBrowser(browser);
                }

                logger.info("Report initialized with ID: {}", report.getReportId());
                logger.info("Report will be stored at: {}", report.getReportPath());

            } catch (Exception e) {
                logger.error("Failed to initialize report for suite: {}", suiteName, e);
            }
        }
    }

    @Override
    public void onFinish(ISuite suite) {
        String suiteName = suite.getName();
        logger.info("=================================================");
        logger.info("Suite Execution Finished: {}", suiteName);
        logger.info("=================================================");

        // Take a screenshot of the last browser state at the end of suite
        if (getDriver() != null) {
            try {
                takeScreenshot("Suite_" + suiteName + "_END");
            } catch (Exception e) {
                logger.warn("Failed to capture end-of-suite screenshot", e);
            }
        }

        // Finalize report
        if (reportManager != null) {
            try {
                reportManager.finalizeReport();
                logger.info("Report finalized successfully");
            } catch (Exception e) {
                logger.error("Failed to finalize report for suite: {}", suiteName, e);
            }
        }
    }

    /**
     * Determine suite type from suite name
     */
    private String determineSuiteType(String suiteName) {
        if (suiteName == null) return "UNKNOWN";

        String lowerName = suiteName.toLowerCase();
        if (lowerName.contains("ui") || lowerName.contains("web") || lowerName.contains("selenium")) {
            return "UI";
        } else if (lowerName.contains("api") || lowerName.contains("rest")) {
            return "API";
        } else if (lowerName.contains("all") || lowerName.contains("full")) {
            return "ALL";
        }
        return "SPECIFIC";
    }

    /**
     * Detect execution mode (UI or CMD)
     */
    private String detectExecutionMode() {
        // Check if running from Maven/command line
        String maven = System.getProperty("maven.home");
        if (maven != null) {
            return "CMD";
        }

        // Check for UI indicators
        if (System.getProperty("spring.application.name") != null) {
            return "UI";
        }

        // Check if running from IDE
        String javaCommand = System.getProperty("sun.java.command", "");
        if (javaCommand.contains("org.testng")) {
            return "CMD";
        }

        return "CMD"; // Default to CMD
    }
}
