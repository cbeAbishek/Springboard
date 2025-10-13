package com.example.automatedtestingframework.service;

import com.example.automatedtestingframework.model.Project;
import com.example.automatedtestingframework.model.Report;
import com.example.automatedtestingframework.model.TestCaseType;
import com.example.automatedtestingframework.repository.ReportRepository;
import com.example.automatedtestingframework.service.dto.ReportAnalytics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class ReportingService {

    private final ReportRepository reportRepository;

    public ReportingService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public Page<Report> fetchReports(Project project,
                                     TestCaseType type,
                                     String status,
                                     OffsetDateTime from,
                                     OffsetDateTime to,
                                     int page,
                                     int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startedAt"));
        return reportRepository.searchReports(project, type, status, from, to, pageable);
    }

    public Map<String, Object> buildSummary(Project project) {
        Map<String, Object> summary = new HashMap<>();
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime startOfWeek = now.with(DayOfWeek.MONDAY); // implicit timezone
        summary.put("total", reportRepository.countByProject(project));
        summary.put("passedWeek", reportRepository.countByProjectAndStatusBetween(project, "PASSED", startOfWeek, now));
        summary.put("failedWeek", reportRepository.countByProjectAndStatusBetween(project, "FAILED", startOfWeek, now));
        summary.put("trend", weeklyTrend(project, 6));
        return summary;
    }

    public Map<String, Map<String, Long>> weeklyTrend(Project project, int weeksBack) {
        Map<String, Map<String, Long>> trend = new HashMap<>();
        OffsetDateTime now = OffsetDateTime.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).withHour(23).withMinute(59);
        for (int i = 0; i <= weeksBack; i++) {
            OffsetDateTime end = now.minusWeeks(i);
            OffsetDateTime start = end.minusWeeks(1).plusSeconds(1);
            long passed = reportRepository.countByProjectAndStatusBetween(project, "PASSED", start, end);
            long failed = reportRepository.countByProjectAndStatusBetween(project, "FAILED", start, end);
            trend.put("week-" + i, Map.of("passed", passed, "failed", failed));
        }
        return trend;
    }

    public List<Report> latestReports(Project project, int limit) {
        return reportRepository.searchReports(project, null, null, null, null,
            PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "startedAt"))).getContent();
    }

    public List<Report> findForExport(Project project,
                                      TestCaseType type,
                                      String status,
                                      OffsetDateTime from,
                                      OffsetDateTime to) {
        return reportRepository.searchReports(project, type, status, from, to, Pageable.unpaged()).getContent();
    }

    public ReportAnalytics buildAnalytics(Project project,
                                          TestCaseType type,
                                          String status,
                                          OffsetDateTime from,
                                          OffsetDateTime to) {
        List<Report> reports = reportRepository.searchReports(project, type, status, from, to, Pageable.unpaged()).getContent();
        if (reports.isEmpty()) {
            return ReportAnalytics.empty();
        }

        long totalRuns = reports.size();
        long passedRuns = reports.stream()
            .filter(report -> "PASSED".equalsIgnoreCase(report.getStatus()))
            .count();
        long failedRuns = reports.stream()
            .filter(report -> "FAILED".equalsIgnoreCase(report.getStatus()))
            .count();

        double passRate = totalRuns == 0 ? 0 : (passedRuns * 100.0) / totalRuns;

        List<Long> durationsMillis = new ArrayList<>();
        Map<String, Long> statusBreakdown = new LinkedHashMap<>();
        Map<String, Long> typeBreakdown = new LinkedHashMap<>();
        Map<LocalDate, long[]> dailyCounters = new TreeMap<>();
        Map<String, FailureAccumulator> failures = new HashMap<>();

        for (Report report : reports) {
            String statusValue = normaliseStatus(report.getStatus());
            statusBreakdown.merge(statusValue, 1L, Long::sum);

            if (report.getStartedAt() != null && report.getCompletedAt() != null) {
                long millis = Duration.between(report.getStartedAt(), report.getCompletedAt()).toMillis();
                if (millis >= 0) {
                    durationsMillis.add(millis);
                }
            }

            String typeKey = resolveTypeKey(report);
            typeBreakdown.merge(typeKey, 1L, Long::sum);

            if (report.getStartedAt() != null) {
                LocalDate day = report.getStartedAt().toLocalDate();
                long[] counts = dailyCounters.computeIfAbsent(day, ReportingService::createCounter);
                if ("PASSED".equalsIgnoreCase(report.getStatus())) {
                    counts[0]++;
                } else if ("FAILED".equalsIgnoreCase(report.getStatus())) {
                    counts[1]++;
                }
            }

            if ("FAILED".equalsIgnoreCase(report.getStatus())) {
                String name = report.getTestCase() != null ? report.getTestCase().getName() : "Ad-hoc run";
                FailureAccumulator accumulator = failures.get(name);
                OffsetDateTime failureMoment = report.getCompletedAt() != null ? report.getCompletedAt() : report.getStartedAt();
                if (accumulator == null) {
                    failures.put(name, new FailureAccumulator(1L, report.getErrorMessage(), failureMoment));
                } else {
                    accumulator.increment(failureMoment, report.getErrorMessage());
                }
            }
        }

        double averageDurationSeconds = durationsMillis.isEmpty()
            ? 0
            : durationsMillis.stream().mapToLong(Long::longValue).average().orElse(0) / 1000d;

        double percentile95Seconds = 0;
        if (!durationsMillis.isEmpty()) {
            List<Long> sorted = durationsMillis.stream()
                .sorted()
                .collect(Collectors.toList());
            int index = (int) Math.ceil(0.95 * sorted.size()) - 1;
            index = Math.max(index, 0);
            percentile95Seconds = sorted.get(index) / 1000d;
        }

        List<ReportAnalytics.DailyStat> dailyStats = dailyCounters.entrySet().stream()
            .map(entry -> new ReportAnalytics.DailyStat(entry.getKey(), entry.getValue()[0], entry.getValue()[1]))
            .collect(Collectors.toList());

        List<ReportAnalytics.TopFailure> topFailures = failures.entrySet().stream()
            .map(entry -> new ReportAnalytics.TopFailure(entry.getKey(), entry.getValue().count, entry.getValue().lastError, entry.getValue().lastFailureAt))
            .sorted((left, right) -> Long.compare(right.failures(), left.failures()))
            .limit(5)
            .collect(Collectors.toList());

        return new ReportAnalytics(totalRuns,
            passedRuns,
            failedRuns,
            passRate,
            averageDurationSeconds,
            percentile95Seconds,
            statusBreakdown.isEmpty() ? Collections.emptyMap() : statusBreakdown,
            typeBreakdown.isEmpty() ? Collections.emptyMap() : typeBreakdown,
            dailyStats,
            topFailures);
    }

    private String normaliseStatus(String status) {
        if (status == null || status.isBlank()) {
            return "UNKNOWN";
        }
        return status.trim().toUpperCase();
    }

    private String resolveTypeKey(Report report) {
        if (report.getTestCase() == null || report.getTestCase().getType() == null) {
            return "Ad-hoc";
        }
        return report.getTestCase().getType().name();
    }

    private static long[] createCounter(LocalDate date) {
        return new long[2];
    }

    private static final class FailureAccumulator {
        private long count;
        private String lastError;
        private OffsetDateTime lastFailureAt;

        private FailureAccumulator(long count, String lastError, OffsetDateTime lastFailureAt) {
            this.count = count;
            this.lastError = lastError;
            this.lastFailureAt = lastFailureAt;
        }

        private void increment(OffsetDateTime failureAt, String error) {
            this.count++;
            if (failureAt != null && (lastFailureAt == null || failureAt.isAfter(lastFailureAt))) {
                this.lastFailureAt = failureAt;
                this.lastError = error;
            }
        }
    }
}
