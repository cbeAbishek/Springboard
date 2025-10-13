package com.example.automatedtestingframework.service;

import com.example.automatedtestingframework.model.Project;
import com.example.automatedtestingframework.model.TestCaseType;
import com.example.automatedtestingframework.repository.ReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;

class ReportingServiceTest {

    static {
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    private ReportRepository reportRepository;
    private ReportingService reportingService;

    @BeforeEach
    void setup() {
        reportRepository = Mockito.mock(ReportRepository.class);
        reportingService = new ReportingService(reportRepository);
    }

    @Test
    void shouldReturnWeeklyTrendData() {
        Project project = new Project();
        project.setId(1L);
        Mockito.when(reportRepository.countByProjectAndStatusBetween(eq(project), eq("PASSED"), any(), any())).thenReturn(5L);
        Mockito.when(reportRepository.countByProjectAndStatusBetween(eq(project), eq("FAILED"), any(), any())).thenReturn(2L);

        var trend = reportingService.weeklyTrend(project, 2);
        assertThat(trend).isNotEmpty();
        assertThat(trend.get("week-0").get("passed")).isEqualTo(5L);
        assertThat(trend.get("week-0").get("failed")).isEqualTo(2L);
    }

    @Test
    void shouldFetchReportsWithTypeFilter() {
        Project project = new Project();
        project.setId(1L);
        Mockito.when(reportRepository.searchReports(eq(project), eq(TestCaseType.API), isNull(), isNull(), isNull(), any(Pageable.class)))
            .thenAnswer(invocation -> {
                Pageable pageable = invocation.getArgument(5, Pageable.class);
                assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "startedAt"));
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            });

        Page<?> page = reportingService.fetchReports(project, TestCaseType.API, null, null, null, 0, 10);
        assertThat(page.getContent()).isEmpty();
    }
}
