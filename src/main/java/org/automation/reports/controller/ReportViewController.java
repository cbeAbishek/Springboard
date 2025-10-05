package org.automation.reports.controller;

import org.automation.reports.model.TestReport;
import org.automation.reports.model.TestReportDetail;
import org.automation.reports.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Web Controller for Test Reports Dashboard
 */
@Controller
@RequestMapping("/reports")
public class ReportViewController {

    @Autowired
    private ReportService reportService;

    /**
     * Main reports page with filters
     */
    @GetMapping
    public String getReportsPage(
            @RequestParam(required = false) String suiteType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) String browser,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Model model) {

        List<TestReport> reports;

        // Apply filters if any are provided
        if (suiteType != null || status != null || createdBy != null || browser != null || startDate != null || endDate != null) {
            reports = reportService.getFilteredReports(suiteType, status, createdBy, browser, startDate, endDate);
        } else {
            reports = reportService.getAllReports();
        }

        // Get statistics
        Map<String, Object> stats = reportService.getStatisticsSummary();

        model.addAttribute("reports", reports);
        model.addAttribute("stats", stats);
        model.addAttribute("suiteType", suiteType);
        model.addAttribute("status", status);
        model.addAttribute("createdBy", createdBy);
        model.addAttribute("browser", browser);

        return "reports";
    }

    /**
     * Report details page
     */
    @GetMapping("/{reportId}")
    public String getReportDetailsPage(@PathVariable String reportId, Model model) {
        Optional<TestReport> reportOpt = reportService.getReportById(reportId);

        if (reportOpt.isEmpty()) {
            model.addAttribute("error", "Report not found: " + reportId);
            return "error";
        }

        TestReport report = reportOpt.get();
        List<TestReportDetail> details = reportService.getTestDetails(reportId);

        model.addAttribute("report", report);
        model.addAttribute("testDetails", details);

        return "report-details";
    }

    /**
     * Aggregated report page
     */
    @GetMapping("/aggregated")
    public String getAggregatedReportPage(Model model) {
        Map<String, Object> aggregated = reportService.getAggregatedReport();
        model.addAttribute("aggregated", aggregated);
        return "aggregated-report";
    }

    /**
     * Failed tests with screenshots page
     */
    @GetMapping("/failures")
    public String getFailuresPage(Model model) {
        List<TestReportDetail> failures = reportService.getFailedTestsWithScreenshots();
        model.addAttribute("failures", failures);
        return "failures";
    }
}
