package org.automation.reports.service;

import org.automation.reports.model.TestReport;
import org.automation.reports.model.TestReportDetail;
import org.automation.reports.repository.TestReportRepository;
import org.automation.reports.repository.TestReportDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing test reports and analytics
 */
@Service
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

    @Autowired
    private TestReportRepository reportRepository;

    @Autowired
    private TestReportDetailRepository detailRepository;

    /**
     * Get all reports ordered by date
     */
    public List<TestReport> getAllReports() {
        return reportRepository.findAllOrderByDateDesc();
    }

    /**
     * Get report by ID
     */
    public Optional<TestReport> getReportById(String reportId) {
        return reportRepository.findByReportId(reportId);
    }

    /**
     * Get reports by date range
     */
    public List<TestReport> getReportsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return reportRepository.findByDateRange(startDate, endDate);
    }

    /**
     * Get reports by suite type with optional status filter
     */
    public List<TestReport> getReportsBySuiteType(String suiteType, String status) {
        if (status != null && !status.isEmpty()) {
            return reportRepository.findBySuiteTypeAndStatus(suiteType, status);
        }
        return reportRepository.findBySuiteType(suiteType);
    }

    /**
     * Get test details for a specific report
     */
    public List<TestReportDetail> getTestDetails(String reportId) {
        return detailRepository.findByReportReportId(reportId);
    }

    /**
     * Get aggregated report containing all test executions
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAggregatedReport() {
        Map<String, Object> aggregatedReport = new HashMap<>();

        List<TestReport> allReports = reportRepository.findAll();

        // Overall statistics
        int totalReports = allReports.size();
        int totalTests = allReports.stream().mapToInt(TestReport::getTotalTests).sum();
        int totalPassed = allReports.stream().mapToInt(TestReport::getPassedTests).sum();
        int totalFailed = allReports.stream().mapToInt(TestReport::getFailedTests).sum();
        int totalSkipped = allReports.stream().mapToInt(TestReport::getSkippedTests).sum();

        double overallSuccessRate = totalTests > 0 ? (totalPassed * 100.0 / totalTests) : 0.0;

        aggregatedReport.put("totalReports", totalReports);
        aggregatedReport.put("totalTests", totalTests);
        aggregatedReport.put("totalPassed", totalPassed);
        aggregatedReport.put("totalFailed", totalFailed);
        aggregatedReport.put("totalSkipped", totalSkipped);
        aggregatedReport.put("overallSuccessRate", String.format("%.2f", overallSuccessRate));

        // Group by suite type
        Map<String, Long> bySuiteType = allReports.stream()
            .collect(Collectors.groupingBy(TestReport::getSuiteType, Collectors.counting()));
        aggregatedReport.put("reportsBySuiteType", bySuiteType);

        // Group by status
        Map<String, Long> byStatus = allReports.stream()
            .collect(Collectors.groupingBy(TestReport::getStatus, Collectors.counting()));
        aggregatedReport.put("reportsByStatus", byStatus);

        // Recent reports (last 10)
        List<TestReport> recentReports = allReports.stream()
            .sorted(Comparator.comparing(TestReport::getExecutionDate).reversed())
            .limit(10)
            .collect(Collectors.toList());
        aggregatedReport.put("recentReports", recentReports);

        // Success rate trend (last 7 days)
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<TestReport> recentCompletedReports = reportRepository.findRecentByStatus("COMPLETED", sevenDaysAgo);
        aggregatedReport.put("recentTrend", calculateTrend(recentCompletedReports));

        logger.info("Generated aggregated report with {} total reports", totalReports);

        return aggregatedReport;
    }

    /**
     * Get filtered reports based on multiple criteria
     */
    public List<TestReport> getFilteredReports(
            String suiteType,
            String status,
            String createdBy,
            String browser,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        List<TestReport> reports = reportRepository.findAll();

        return reports.stream()
            .filter(r -> suiteType == null || suiteType.isEmpty() || r.getSuiteType().equals(suiteType))
            .filter(r -> status == null || status.isEmpty() || r.getStatus().equals(status))
            .filter(r -> createdBy == null || createdBy.isEmpty() || r.getCreatedBy().equals(createdBy))
            .filter(r -> browser == null || browser.isEmpty() || r.getBrowser() != null && r.getBrowser().equals(browser))
            .filter(r -> startDate == null || r.getExecutionDate().isAfter(startDate) || r.getExecutionDate().isEqual(startDate))
            .filter(r -> endDate == null || r.getExecutionDate().isBefore(endDate) || r.getExecutionDate().isEqual(endDate))
            .sorted(Comparator.comparing(TestReport::getExecutionDate).reversed())
            .collect(Collectors.toList());
    }

    /**
     * Get failed tests with screenshots
     */
    public List<TestReportDetail> getFailedTestsWithScreenshots() {
        return detailRepository.findAllWithScreenshots().stream()
            .filter(d -> "FAIL".equalsIgnoreCase(d.getStatus()) || "FAILED".equalsIgnoreCase(d.getStatus()))
            .collect(Collectors.toList());
    }

    /**
     * Calculate success rate trend
     */
    private List<Map<String, Object>> calculateTrend(List<TestReport> reports) {
        Map<LocalDateTime, List<TestReport>> reportsByDate = reports.stream()
            .collect(Collectors.groupingBy(r -> r.getExecutionDate().toLocalDate().atStartOfDay()));

        List<Map<String, Object>> trend = new ArrayList<>();
        reportsByDate.forEach((date, dateReports) -> {
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.toLocalDate().toString());
            dayData.put("totalTests", dateReports.stream().mapToInt(TestReport::getTotalTests).sum());
            dayData.put("passedTests", dateReports.stream().mapToInt(TestReport::getPassedTests).sum());
            dayData.put("failedTests", dateReports.stream().mapToInt(TestReport::getFailedTests).sum());

            int total = dateReports.stream().mapToInt(TestReport::getTotalTests).sum();
            int passed = dateReports.stream().mapToInt(TestReport::getPassedTests).sum();
            double successRate = total > 0 ? (passed * 100.0 / total) : 0.0;
            dayData.put("successRate", String.format("%.2f", successRate));

            trend.add(dayData);
        });

        return trend.stream()
            .sorted(Comparator.comparing(d -> (String) d.get("date")))
            .collect(Collectors.toList());
    }

    /**
     * Get statistics summary
     */
    public Map<String, Object> getStatisticsSummary() {
        Map<String, Object> stats = new HashMap<>();

        long totalReports = reportRepository.count();
        long completedReports = reportRepository.countCompleted();
        Double avgSuccessRate = reportRepository.getAverageSuccessRate();

        long totalPassed = detailRepository.countByStatus("PASS");
        long totalFailed = detailRepository.countByStatus("FAIL");
        long totalSkipped = detailRepository.countByStatus("SKIP");

        stats.put("totalReports", totalReports);
        stats.put("completedReports", completedReports);
        stats.put("averageSuccessRate", avgSuccessRate != null ? String.format("%.2f", avgSuccessRate) : "0.00");
        stats.put("totalPassedTests", totalPassed);
        stats.put("totalFailedTests", totalFailed);
        stats.put("totalSkippedTests", totalSkipped);

        return stats;
    }

    /**
     * Save or update report
     */
    @Transactional
    public TestReport saveReport(TestReport report) {
        return reportRepository.save(report);
    }

    /**
     * Save test detail
     */
    @Transactional
    public TestReportDetail saveTestDetail(TestReportDetail detail) {
        return detailRepository.save(detail);
    }

    /**
     * Delete old reports (cleanup)
     */
    @Transactional
    public int deleteOldReports(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        List<TestReport> oldReports = reportRepository.findAll().stream()
            .filter(r -> r.getExecutionDate().isBefore(cutoffDate))
            .collect(Collectors.toList());

        int count = oldReports.size();
        reportRepository.deleteAll(oldReports);
        logger.info("Deleted {} old reports (older than {} days)", count, daysToKeep);

        return count;
    }
}
