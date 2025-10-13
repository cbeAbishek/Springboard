package com.example.automatedtestingframework.service.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public record ReportAnalytics(
        long totalRuns,
        long passedRuns,
        long failedRuns,
        double passRate,
        double averageDurationSeconds,
        double percentile95DurationSeconds,
        Map<String, Long> statusBreakdown,
        Map<String, Long> typeBreakdown,
        List<DailyStat> dailyStats,
        List<TopFailure> topFailures
) {

    public static ReportAnalytics empty() {
        return new ReportAnalytics(0, 0, 0, 0, 0, 0,
            Collections.emptyMap(), Collections.emptyMap(),
            Collections.emptyList(), Collections.emptyList());
    }

    public record DailyStat(LocalDate date, long passed, long failed) { }

    public record TopFailure(String testName, long failures, String lastError, OffsetDateTime lastFailureAt) { }
}
