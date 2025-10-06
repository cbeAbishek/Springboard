package org.automation.dashboard;

import org.automation.analytics.service.AnalyticsService;
import org.automation.analytics.model.ExecutionLog;
import org.automation.reports.model.TestReport;
import org.automation.reports.model.TestReportDetail;
import org.automation.reports.repository.TestReportRepository;
import org.automation.reports.repository.TestReportDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Main controller for the Test Dashboard web interface
 */
@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired(required = false)
    private TestReportRepository testReportRepository;

    @Autowired(required = false)
    private TestReportDetailRepository testReportDetailRepository;

    @GetMapping({"", "/"})
    public String dashboard(Model model) {
        try {
            // Get test execution statistics
            Map<String, Object> stats = analyticsService.getTestExecutionStats();
            model.addAttribute("stats", stats);

            // Get recent test executions
            List<ExecutionLog> recentExecutions = analyticsService.getRecentExecutions(10);
            model.addAttribute("recentExecutions", recentExecutions);

            // Get last updated timestamp
            String lastUpdated = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            model.addAttribute("lastUpdated", lastUpdated);

            return "dashboard/index";
        } catch (Exception e) {
            logger.error("Error loading dashboard: {}", e.getMessage());
            model.addAttribute("error", "Failed to load dashboard data");
            return "error";
        }
    }

    @GetMapping("/reports")
    public String reports(Model model,
                         @RequestParam(required = false) String suiteType,
                         @RequestParam(required = false) String status,
                         @RequestParam(required = false) String browser,
                         @RequestParam(required = false) String startDate,
                         @RequestParam(required = false) String endDate) {
        try {
            List<TestReport> reports;

            if (testReportRepository != null) {
                // Apply filters if provided
                LocalDateTime start = startDate != null && !startDate.isEmpty() ?
                    LocalDateTime.parse(startDate) : null;
                LocalDateTime end = endDate != null && !endDate.isEmpty() ?
                    LocalDateTime.parse(endDate) : null;

                reports = testReportRepository.findReportsWithFilters(
                    suiteType, status, browser, start, end
                );
            } else {
                reports = new ArrayList<>();
            }

            model.addAttribute("reports", reports);
            model.addAttribute("selectedSuiteType", suiteType);
            model.addAttribute("selectedStatus", status);
            model.addAttribute("selectedBrowser", browser);

            return "dashboard/reports";
        } catch (Exception e) {
            logger.error("Error loading reports: {}", e.getMessage());
            model.addAttribute("error", "Failed to load reports");
            return "error";
        }
    }

    @GetMapping("/test-manager")
    public String testManager(Model model) {
        try {
            // Get available test classes
            List<String> uiTests = getAvailableTestClasses("ui");
            List<String> apiTests = getAvailableTestClasses("api");

            model.addAttribute("uiTests", uiTests);
            model.addAttribute("apiTests", apiTests);

            return "dashboard/test-manager";
        } catch (Exception e) {
            logger.error("Error loading test manager: {}", e.getMessage());
            model.addAttribute("error", "Failed to load test manager");
            return "error";
        }
    }

    @PostMapping("/run-tests")
    @ResponseBody
    public ResponseEntity<Map<String, String>> runTests(@RequestBody Map<String, Object> request) {
        Map<String, String> response = new HashMap<>();

        try {
            String suite = (String) request.get("suite");
            String browser = (String) request.getOrDefault("browser", "chrome");
            boolean headless = (boolean) request.getOrDefault("headless", false);

            // Execute tests asynchronously
            String executionId = analyticsService.executeTestSuite(suite, browser, headless);

            response.put("status", "success");
            response.put("message", "Test execution started");
            response.put("executionId", executionId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error starting test execution: {}", e.getMessage());
            response.put("status", "error");
            response.put("message", "Failed to start test execution: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/execution-status/{executionId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getExecutionStatus(@PathVariable String executionId) {
        try {
            Map<String, Object> status = analyticsService.getExecutionStatus(executionId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("Error getting execution status: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/upload-test")
    @ResponseBody
    public ResponseEntity<Map<String, String>> uploadTest(@RequestParam("file") MultipartFile file,
                                                          @RequestParam("testType") String testType) {
        Map<String, String> response = new HashMap<>();

        try {
            if (file.isEmpty()) {
                response.put("status", "error");
                response.put("message", "Please select a file to upload");
                return ResponseEntity.badRequest().body(response);
            }

            // Save the uploaded test file
            String fileName = file.getOriginalFilename();
            String targetDir = testType.equals("ui") ? "src/test/java/org/automation/ui/" : "src/test/java/org/automation/api/";
            Path targetPath = Paths.get(targetDir + fileName);

            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, file.getBytes());

            response.put("status", "success");
            response.put("message", "Test file uploaded successfully");
            response.put("fileName", fileName);

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            logger.error("Error uploading test file: {}", e.getMessage());
            response.put("status", "error");
            response.put("message", "Failed to upload test file: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/api/stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getApiStats() {
        try {
            Map<String, Object> stats = analyticsService.getTestExecutionStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error getting API stats: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private List<String> getAvailableReports(String type) {
        try {
            Path reportsDir = Paths.get("artifacts/reports");
            if (Files.exists(reportsDir)) {
                return Files.walk(reportsDir)
                    .filter(path -> path.toString().endsWith("." + type))
                    .map(path -> path.getFileName().toString())
                    .toList();
            }
        } catch (IOException e) {
            logger.warn("Error reading reports directory: {}", e.getMessage());
        }
        return List.of();
    }

    private List<String> getAvailableTestClasses(String type) {
        try {
            Path testDir = Paths.get("src/test/java/org/automation/" + type);
            if (Files.exists(testDir)) {
                return Files.walk(testDir)
                    .filter(path -> path.toString().endsWith(".java"))
                    .map(path -> path.getFileName().toString().replace(".java", ""))
                    .toList();
            }
        } catch (IOException e) {
            logger.warn("Error reading test directory: {}", e.getMessage());
        }
        return List.of();
    }

    @GetMapping("/analytics")
    public String analytics(Model model) {
        try {
            // Get cumulative test statistics
            Map<String, Object> stats = analyticsService.getTestExecutionStats();
            model.addAttribute("stats", stats);

            return "dashboard/analytics";
        } catch (Exception e) {
            logger.error("Error loading analytics: {}", e.getMessage());
            model.addAttribute("error", "Failed to load analytics data");
            return "error";
        }
    }

    @GetMapping("/api/test-matrix")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTestMatrix(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "all") String suite,
            @RequestParam(defaultValue = "all") String status) {

        try {
            Map<String, Object> matrixData = analyticsService.getTestMatrix(days, suite, status);
            return ResponseEntity.ok(matrixData);
        } catch (Exception e) {
            logger.error("Error getting test matrix: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/failure-heatmap")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getFailureHeatmap(
            @RequestParam(defaultValue = "30") int days) {

        try {
            List<Map<String, Object>> heatmapData = analyticsService.getFailureHeatmap(days);
            return ResponseEntity.ok(heatmapData);
        } catch (Exception e) {
            logger.error("Error getting failure heatmap: {}", e.getMessage());
            return ResponseEntity.badRequest().body(List.of());
        }
    }

    @GetMapping("/api/export/csv")
    public void exportToCsv(HttpServletResponse response) {
        try {
            response.setContentType("text/csv");
            response.setHeader("Content-Disposition", "attachment; filename=test-results.csv");

            String csvData = analyticsService.exportTestDataToCsv();
            response.getWriter().write(csvData);
            response.getWriter().flush();
        } catch (Exception e) {
            logger.error("Error exporting to CSV: {}", e.getMessage());
        }
    }

    @GetMapping("/api/export/excel")
    public void exportToExcel(HttpServletResponse response) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=test-results.xlsx");

            byte[] excelData = analyticsService.exportTestDataToExcel();
            response.getOutputStream().write(excelData);
            response.getOutputStream().flush();
        } catch (Exception e) {
            logger.error("Error exporting to Excel: {}", e.getMessage());
        }
    }

    @GetMapping("/reports/{reportId}")
    public String viewReport(@PathVariable String reportId, Model model) {
        try {
            if (testReportRepository != null && testReportDetailRepository != null) {
                TestReport report = testReportRepository.findByReportId(reportId)
                    .orElseThrow(() -> new RuntimeException("Report not found: " + reportId));

                List<TestReportDetail> details = testReportDetailRepository.findByReport_ReportId(reportId);
                List<TestReportDetail> failedTests = testReportDetailRepository.findFailedTestsWithScreenshots(reportId);

                model.addAttribute("report", report);
                model.addAttribute("testDetails", details);
                model.addAttribute("failedTests", failedTests);

                return "dashboard/report-detail";
            } else {
                model.addAttribute("error", "Report system not initialized");
                return "error";
            }
        } catch (Exception e) {
            logger.error("Error loading report {}: {}", reportId, e.getMessage());
            model.addAttribute("error", "Failed to load report: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/reports/aggregated")
    public String aggregatedReport(Model model) {
        try {
            if (testReportRepository != null) {
                List<TestReport> allReports = testReportRepository.findAllOrderByExecutionDateDesc();
                model.addAttribute("reports", allReports);

                // Calculate overall statistics
                int totalTests = allReports.stream().mapToInt(TestReport::getTotalTests).sum();
                int totalPassed = allReports.stream().mapToInt(TestReport::getPassedTests).sum();
                int totalFailed = allReports.stream().mapToInt(TestReport::getFailedTests).sum();
                int totalSkipped = allReports.stream().mapToInt(TestReport::getSkippedTests).sum();
                double overallSuccessRate = totalTests > 0 ? (totalPassed * 100.0 / totalTests) : 0;

                model.addAttribute("totalTests", totalTests);
                model.addAttribute("totalPassed", totalPassed);
                model.addAttribute("totalFailed", totalFailed);
                model.addAttribute("totalSkipped", totalSkipped);
                model.addAttribute("overallSuccessRate", overallSuccessRate);

                return "dashboard/aggregated-report";
            } else {
                model.addAttribute("error", "Report repository not available");
                return "error";
            }
        } catch (Exception e) {
            logger.error("Error loading aggregated report: {}", e.getMessage());
            model.addAttribute("error", "Failed to load aggregated report");
            return "error";
        }
    }

    @GetMapping("/api/reports")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getReportsApi(
            @RequestParam(required = false) String suiteType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String browser) {
        try {
            if (testReportRepository != null) {
                List<TestReport> reports = testReportRepository.findReportsWithFilters(
                    suiteType, status, browser, null, null
                );

                List<Map<String, Object>> reportData = reports.stream()
                    .map(this::convertReportToMap)
                    .collect(Collectors.toList());

                return ResponseEntity.ok(reportData);
            }
            return ResponseEntity.ok(new ArrayList<>());
        } catch (Exception e) {
            logger.error("Error getting reports via API: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ArrayList<>());
        }
    }

    @GetMapping("/api/reports/{reportId}/details")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getReportDetails(@PathVariable String reportId) {
        try {
            if (testReportRepository != null && testReportDetailRepository != null) {
                TestReport report = testReportRepository.findByReportId(reportId)
                    .orElseThrow(() -> new RuntimeException("Report not found"));

                List<TestReportDetail> details = testReportDetailRepository.findByReport_ReportId(reportId);

                Map<String, Object> response = new HashMap<>();
                response.put("report", convertReportToMap(report));
                response.put("details", details.stream()
                    .map(this::convertDetailToMap)
                    .collect(Collectors.toList()));

                return ResponseEntity.ok(response);
            }
            return ResponseEntity.badRequest().body(Map.of("error", "Repository not available"));
        } catch (Exception e) {
            logger.error("Error getting report details: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/reports/{reportId}/download")
    public void downloadReport(@PathVariable String reportId, HttpServletResponse response) {
        try {
            if (testReportRepository != null) {
                TestReport report = testReportRepository.findByReportId(reportId)
                    .orElseThrow(() -> new RuntimeException("Report not found"));

                String reportPath = report.getReportPath() + "/report.html";
                Path filePath = Paths.get(reportPath);

                if (Files.exists(filePath)) {
                    response.setContentType("text/html");
                    response.setHeader("Content-Disposition",
                        "attachment; filename=" + reportId + "_report.html");

                    Files.copy(filePath, response.getOutputStream());
                    response.getOutputStream().flush();
                } else {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Report file not found");
                }
            }
        } catch (Exception e) {
            logger.error("Error downloading report: {}", e.getMessage());
        }
    }

    private Map<String, Object> convertReportToMap(TestReport report) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", report.getId());
        map.put("reportId", report.getReportId());
        map.put("reportName", report.getReportName());
        map.put("executionDate", report.getExecutionDate());
        map.put("suiteType", report.getSuiteType());
        map.put("browser", report.getBrowser());
        map.put("totalTests", report.getTotalTests());
        map.put("passedTests", report.getPassedTests());
        map.put("failedTests", report.getFailedTests());
        map.put("skippedTests", report.getSkippedTests());
        map.put("successRate", report.getSuccessRate());
        map.put("durationMs", report.getDurationMs());
        map.put("status", report.getStatus());
        map.put("createdBy", report.getCreatedBy());
        map.put("reportPath", report.getReportPath());
        return map;
    }

    private Map<String, Object> convertDetailToMap(TestReportDetail detail) {
        Map<String, Object> map = new HashMap<>();
        map.put("testName", detail.getTestName());
        map.put("testClass", detail.getTestClass());
        map.put("status", detail.getStatus());
        map.put("testType", detail.getTestType());
        map.put("durationMs", detail.getDurationMs());
        map.put("errorMessage", detail.getErrorMessage());
        map.put("screenshotPath", detail.getScreenshotPath());
        map.put("startTime", detail.getStartTime());
        return map;
    }
}
