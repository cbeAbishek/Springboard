package com.example.automatedtestingframework.service;

import com.example.automatedtestingframework.model.GeneratedReport;
import com.example.automatedtestingframework.model.Project;
import com.example.automatedtestingframework.model.Report;
import com.example.automatedtestingframework.model.TestCaseType;
import com.example.automatedtestingframework.model.User;
import com.example.automatedtestingframework.repository.GeneratedReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.StringJoiner;

@Service
public class ReportExportService {

    private static final Logger log = LoggerFactory.getLogger(ReportExportService.class);
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final ReportingService reportingService;
    private final ReportStorageService storageService;
    private final GeneratedReportRepository generatedReportRepository;
    private final MailService mailService;

    public ReportExportService(ReportingService reportingService,
                               ReportStorageService storageService,
                               GeneratedReportRepository generatedReportRepository,
                               MailService mailService) {
        this.reportingService = reportingService;
        this.storageService = storageService;
        this.generatedReportRepository = generatedReportRepository;
        this.mailService = mailService;
    }

    public GeneratedReport generate(User user,
                                    Project project,
                                    TestCaseType type,
                                    String status,
                                    OffsetDateTime from,
                                    OffsetDateTime to) {
        List<Report> reports = reportingService.findForExport(project, type, status, from, to);
        if (CollectionUtils.isEmpty(reports)) {
            throw new IllegalArgumentException("No reports found for the selected filters");
        }

        String csv = createCsv(reports);
        String filename = buildFilename(project, type, status);
        byte[] payload = csv.getBytes(StandardCharsets.UTF_8);
        String fileUrl = storageService.upload(payload, filename, "text/csv");

        GeneratedReport record = new GeneratedReport();
        record.setProject(project);
        record.setUser(user);
        record.setFilterType(type);
        record.setFilterStatus(status);
        record.setFilterFrom(from);
        record.setFilterTo(to);
        record.setTotalRecords(reports.size());
        record.setFileUrl(fileUrl);
        record.setFileName(filename);
        record.setMimeType("text/csv");
        GeneratedReport saved = generatedReportRepository.save(record);

        String filterSummary = buildFilterSummary(type, status, from, to);
        mailService.sendGeneratedReport(user, project, saved, filterSummary);

        log.info("Generated report {} with {} records for user {}", saved.getId(), reports.size(), user.getEmail());
        return saved;
    }

    public List<GeneratedReport> recent(User user, Project project) {
        return generatedReportRepository.findTop10ByUserAndProjectOrderByCreatedAtDesc(user, project);
    }

    private String buildFilename(Project project, TestCaseType type, String status) {
        StringJoiner joiner = new StringJoiner("-");
        joiner.add("report");
        joiner.add(project.getId().toString());
        joiner.add(FILE_TIMESTAMP.format(OffsetDateTime.now()));
        if (type != null) {
            joiner.add(type.name().toLowerCase());
        }
        if (status != null && !status.isBlank()) {
            joiner.add(status.toLowerCase());
        }
        return joiner.toString() + ".csv";
    }

    private String createCsv(List<Report> reports) {
        StringBuilder builder = new StringBuilder();
        builder.append("TestCase,Type,Status,StartedAt,CompletedAt,Summary,ResponseCode,ScreenshotURL\n");
        for (Report report : reports) {
            builder.append(escape(report.getTestCase() != null ? report.getTestCase().getName() : "Ad-hoc"))
                .append(',')
                .append(escape(report.getTestCase() != null ? report.getTestCase().getType().name() : ""))
                .append(',')
                .append(escape(report.getStatus()))
                .append(',')
                .append(escape(formatDate(report.getStartedAt())))
                .append(',')
                .append(escape(formatDate(report.getCompletedAt())))
                .append(',')
                .append(escape(report.getSummary()))
                .append(',')
                .append(report.getResponseCode() != null ? report.getResponseCode() : "")
                .append(',')
                .append(escape(report.getScreenshotUrl()))
                .append('\n');
        }
        return builder.toString();
    }

    private String formatDate(OffsetDateTime dateTime) {
        return dateTime != null ? dateTime.toString() : "";
    }

    private String escape(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();
        if (text.contains(",") || text.contains("\"")) {
            return '"' + text.replace("\"", "\"\"") + '"';
        }
        return text;
    }

    private String buildFilterSummary(TestCaseType type, String status, OffsetDateTime from, OffsetDateTime to) {
        StringBuilder summary = new StringBuilder("Filters applied: ");
        summary.append("type=").append(type != null ? type.name() : "ANY");
        summary.append(", status=").append(status != null && !status.isBlank() ? status : "ANY");
        summary.append(", from=").append(from != null ? from : "-");
        summary.append(", to=").append(to != null ? to : "-");
        return summary.toString();
    }
}
