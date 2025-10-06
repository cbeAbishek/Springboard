package org.automation.reports.controller;

import org.automation.reports.model.TestReport;
import org.automation.reports.model.TestReportDetail;
import org.automation.reports.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for Test Report Management
 * Provides endpoints for viewing and filtering reports
 */
@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    @Autowired
    private ReportService reportService;

    /**
     * Get all reports
     */
    @GetMapping
    public ResponseEntity<List<TestReport>> getAllReports() {
        List<TestReport> reports = reportService.getAllReports();
        return ResponseEntity.ok(reports);
    }

    /**
     * Get report by ID
     */
    @GetMapping("/{reportId}")
    public ResponseEntity<TestReport> getReportById(@PathVariable String reportId) {
        Optional<TestReport> report = reportService.getReportById(reportId);
        return report.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get test details for a specific report
     */
    @GetMapping("/{reportId}/details")
    public ResponseEntity<List<TestReportDetail>> getTestDetails(@PathVariable String reportId) {
        List<TestReportDetail> details = reportService.getTestDetails(reportId);
        return ResponseEntity.ok(details);
    }

    /**
     * Get individual test detail by ID
     */
    @GetMapping("/test-detail/{detailId}")
    public ResponseEntity<TestReportDetail> getTestDetailById(@PathVariable Long detailId) {
        Optional<TestReportDetail> detail = reportService.getTestDetailById(detailId);
        return detail.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get test detail by report ID and test name
     */
    @GetMapping("/{reportId}/test/{testName}")
    public ResponseEntity<TestReportDetail> getTestDetailByName(
            @PathVariable String reportId,
            @PathVariable String testName) {
        Optional<TestReportDetail> detail = reportService.getTestDetailByName(reportId, testName);
        return detail.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get filtered reports with multiple criteria
     */
    @GetMapping("/filter")
    public ResponseEntity<List<TestReport>> getFilteredReports(
            @RequestParam(required = false) String suiteType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) String browser,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        List<TestReport> reports = reportService.getFilteredReports(
            suiteType, status, createdBy, browser, startDate, endDate);
        return ResponseEntity.ok(reports);
    }

    /**
     * Get aggregated report with all test executions
     */
    @GetMapping("/aggregated")
    public ResponseEntity<Map<String, Object>> getAggregatedReport() {
        Map<String, Object> aggregated = reportService.getAggregatedReport();
        return ResponseEntity.ok(aggregated);
    }

    /**
     * Get failed tests with screenshots
     */
    @GetMapping("/failures/screenshots")
    public ResponseEntity<List<TestReportDetail>> getFailedTestsWithScreenshots() {
        List<TestReportDetail> failures = reportService.getFailedTestsWithScreenshots();
        return ResponseEntity.ok(failures);
    }

    /**
     * Get statistics summary
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = reportService.getStatisticsSummary();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get reports by suite type
     */
    @GetMapping("/suite/{suiteType}")
    public ResponseEntity<List<TestReport>> getReportsBySuiteType(
            @PathVariable String suiteType,
            @RequestParam(required = false) String status) {

        List<TestReport> reports = reportService.getReportsBySuiteType(suiteType, status);
        return ResponseEntity.ok(reports);
    }

    /**
     * Get reports by date range
     */
    @GetMapping("/daterange")
    public ResponseEntity<List<TestReport>> getReportsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        List<TestReport> reports = reportService.getReportsByDateRange(startDate, endDate);
        return ResponseEntity.ok(reports);
    }

    /**
     * Delete old reports (cleanup endpoint)
     */
    @DeleteMapping("/cleanup/{daysToKeep}")
    public ResponseEntity<Map<String, Object>> cleanupOldReports(@PathVariable int daysToKeep) {
        int deletedCount = reportService.deleteOldReports(daysToKeep);
        Map<String, Object> response = Map.of(
            "message", "Cleanup completed",
            "deletedReports", deletedCount
        );
        return ResponseEntity.ok(response);
    }
}
