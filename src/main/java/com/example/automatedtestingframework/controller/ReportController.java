package com.example.automatedtestingframework.controller;

import com.example.automatedtestingframework.model.Project;
import com.example.automatedtestingframework.model.GeneratedReport;
import com.example.automatedtestingframework.model.TestCaseType;
import com.example.automatedtestingframework.model.User;
import com.example.automatedtestingframework.repository.ProjectRepository;
import com.example.automatedtestingframework.repository.UserRepository;
import com.example.automatedtestingframework.service.ReportingService;
import com.example.automatedtestingframework.service.ReportExportService;
import com.example.automatedtestingframework.service.dto.ReportAnalytics;
import com.example.automatedtestingframework.util.JsonParserUtil;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.format.annotation.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.Nullable;

@Controller
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    private final ReportingService reportingService;
    private final ReportExportService reportExportService;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final JsonParserUtil jsonParserUtil;

    public ReportController(ReportingService reportingService,
                            ReportExportService reportExportService,
                            ProjectRepository projectRepository,
                            UserRepository userRepository,
                            JsonParserUtil jsonParserUtil) {
        this.reportingService = reportingService;
        this.reportExportService = reportExportService;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.jsonParserUtil = jsonParserUtil;
    }

    @GetMapping("/reports")
    public String reports(@AuthenticationPrincipal UserDetails principal,
                          @RequestParam(name = "projectId", required = false) Long projectId,
                          @RequestParam(name = "type", required = false) TestCaseType type,
                          @RequestParam(name = "status", required = false) String status,
                          @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                          @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                          @RequestParam(name = "page", defaultValue = "0") int page,
                          @RequestParam(name = "size", defaultValue = "20") int size,
                          Model model) {
        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        List<Project> projects = projectRepository.findByOwner(user);
        model.addAttribute("projects", projects);

        Project project = null;
        if (projectId != null) {
            project = projects.stream()
                .filter(candidate -> candidate.getId().equals(projectId))
                .findFirst()
                .orElse(null);
        }
        if (project == null && !projects.isEmpty()) {
            project = projects.get(0);
        }
        if (project == null) {
            model.addAttribute("message", "Please create a project to view reports.");
            return "reports";
        }
        model.addAttribute("selectedProjectId", project.getId());
    String normalizedStatus = cleanse(status);
    OffsetDateTime fromDate = toStartOfDay(from);
    OffsetDateTime toDate = toEndOfDay(to);
    Page<com.example.automatedtestingframework.model.Report> reports = reportingService.fetchReports(project, type, normalizedStatus, fromDate, toDate, page, size);
        model.addAttribute("reports", reports);
        model.addAttribute("project", project);
        model.addAttribute("filterType", type);
    model.addAttribute("filterStatus", normalizedStatus);
        model.addAttribute("filterFrom", from);
        model.addAttribute("filterTo", to);
        model.addAttribute("trend", reportingService.weeklyTrend(project, 8));
        model.addAttribute("generatedReports", reportExportService.recent(user, project));

        ReportAnalytics analytics = reportingService.buildAnalytics(project, type, normalizedStatus, fromDate, toDate);
        model.addAttribute("analytics", analytics);
        model.addAttribute("analyticsJson", jsonParserUtil.toJson(analytics));

        Set<String> statusOptions = new TreeSet<>();
        reports.stream()
            .map(com.example.automatedtestingframework.model.Report::getStatus)
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .forEach(statusOptions::add);
        if (normalizedStatus != null) {
            statusOptions.add(normalizedStatus);
        }
        model.addAttribute("statusOptions", statusOptions);
        return "reports";
    }

    @PostMapping("/reports/export")
    public String exportReports(@AuthenticationPrincipal UserDetails principal,
                                @RequestParam(name = "projectId") Long projectId,
                                @RequestParam(name = "type", required = false) TestCaseType type,
                                @RequestParam(name = "status", required = false) String status,
                                @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                RedirectAttributes redirectAttributes) {
        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        Project project = projectRepository.findById(projectId).orElseThrow();
        String normalizedStatus = cleanse(status);
        OffsetDateTime fromDate = toStartOfDay(from);
        OffsetDateTime toDate = toEndOfDay(to);

        try {
            GeneratedReport generated = reportExportService.generate(user, project, type, normalizedStatus, fromDate, toDate);
            redirectAttributes.addFlashAttribute("message", "Report '" + generated.getFileName() + "' generated and emailed to " + user.getEmail());
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        } catch (Exception ex) {
            log.error("Failed to export filtered report", ex);
            redirectAttributes.addFlashAttribute("error", "Failed to generate report: " + ex.getMessage());
        }

        return "redirect:" + buildRedirect(projectId, type, normalizedStatus, from, to);
    }

    @GetMapping("/regression-monitoring")
    public String regressionMonitoring(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        Project project = projectRepository.findByOwner(user).stream().findFirst().orElse(null);
        if (project == null) {
            model.addAttribute("message", "Create a project to start monitoring regressions.");
            return "regression-monitoring";
        }
        model.addAttribute("trend", reportingService.weeklyTrend(project, 12));
        model.addAttribute("reports", reportingService.latestReports(project, 10));
        return "regression-monitoring";
    }

    private OffsetDateTime toStartOfDay(@Nullable LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
    }

    private OffsetDateTime toEndOfDay(@Nullable LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).minusNanos(1).toOffsetDateTime();
    }

    private String cleanse(@Nullable String value) {
        return value != null && !value.isBlank() ? value.trim() : null;
    }

    private String buildRedirect(Long projectId,
                                 @Nullable TestCaseType type,
                                 @Nullable String status,
                                 @Nullable LocalDate from,
                                 @Nullable LocalDate to) {
        StringBuilder builder = new StringBuilder("/reports?projectId=").append(projectId);
        if (type != null) {
            builder.append("&type=").append(type.name());
        }
        if (status != null && !status.isBlank()) {
            builder.append("&status=").append(status.trim());
        }
        if (from != null) {
            builder.append("&from=").append(from);
        }
        if (to != null) {
            builder.append("&to=").append(to);
        }
        return builder.toString();
    }
}
