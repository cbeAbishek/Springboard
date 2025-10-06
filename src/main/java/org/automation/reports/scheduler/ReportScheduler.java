package org.automation.reports.scheduler;

import org.automation.reports.service.ReportService;
import org.automation.reports.config.ReportConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled tasks for report management
 */
@Component
public class ReportScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ReportScheduler.class);

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportConfig reportConfig;

    /**
     * Cleanup old reports daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldReports() {
        if (reportConfig.isAutoCleanup()) {
            logger.info("Starting scheduled cleanup of old reports...");
            try {
                int deletedCount = reportService.deleteOldReports(reportConfig.getRetentionDays());
                logger.info("Cleanup completed. Deleted {} old reports.", deletedCount);
            } catch (Exception e) {
                logger.error("Error during scheduled report cleanup", e);
            }
        }
    }

    /**
     * Log report statistics every hour
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void logReportStatistics() {
        try {
            var stats = reportService.getStatisticsSummary();
            logger.info("Report Statistics - Total Reports: {}, Avg Success Rate: {}%",
                stats.get("totalReports"), stats.get("averageSuccessRate"));
        } catch (Exception e) {
            logger.error("Error logging report statistics", e);
        }
    }
}
