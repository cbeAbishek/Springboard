package com.example.automatedtestingframework.service;

import com.example.automatedtestingframework.model.GeneratedReport;
import com.example.automatedtestingframework.model.Project;
import com.example.automatedtestingframework.model.Report;
import com.example.automatedtestingframework.model.TestCase;
import com.example.automatedtestingframework.model.TestCaseType;
import com.example.automatedtestingframework.model.User;
import com.example.automatedtestingframework.repository.GeneratedReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportExportServiceTest {

    private ReportingService reportingService;
    private ReportStorageService storageService;
    private GeneratedReportRepository generatedReportRepository;
    private MailService mailService;
    private ReportExportService reportExportService;

    @BeforeEach
    void setup() {
        reportingService = Mockito.mock(ReportingService.class);
        storageService = Mockito.mock(ReportStorageService.class);
        generatedReportRepository = Mockito.mock(GeneratedReportRepository.class);
        mailService = Mockito.mock(MailService.class);
        reportExportService = new ReportExportService(reportingService, storageService, generatedReportRepository, mailService);
    }

    @Test
    void shouldThrowWhenNoReportsToExport() {
        Project project = new Project();
        User user = new User();
        when(reportingService.findForExport(eq(project), any(), any(), any(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> reportExportService.generate(user, project, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No reports");
    }

    @Test
    void shouldGenerateReportUploadAndNotify() {
        Project project = new Project();
        project.setId(42L);
        User user = new User();
        user.setEmail("qa@example.com");
        user.setFullName("QA Lead");

        TestCase testCase = new TestCase();
        testCase.setName("Checkout flow");
        testCase.setType(TestCaseType.UI);

        Report report = new Report();
        report.setTestCase(testCase);
        report.setStatus("PASSED");
        report.setStartedAt(OffsetDateTime.now().minusMinutes(5));
        report.setCompletedAt(OffsetDateTime.now());
        report.setSummary("All good");
        report.setResponseCode(200);
        report.setScreenshotUrl("https://example.org/screenshot.png");

        when(reportingService.findForExport(eq(project), eq(TestCaseType.UI), eq("PASSED"), any(), any()))
            .thenReturn(List.of(report));
        when(storageService.upload(any(), anyString(), anyString())).thenReturn("https://cdn.example.org/report.csv");
        when(generatedReportRepository.save(any())).thenAnswer(invocation -> {
            GeneratedReport saved = invocation.getArgument(0);
            saved.setId(11L);
            saved.setFileUrl("https://cdn.example.org/report.csv");
            return saved;
        });

        GeneratedReport generated = reportExportService.generate(user, project, TestCaseType.UI, "PASSED", null, null);

        assertThat(generated.getTotalRecords()).isEqualTo(1);
        assertThat(generated.getFileUrl()).isEqualTo("https://cdn.example.org/report.csv");

        ArgumentCaptor<GeneratedReport> reportCaptor = ArgumentCaptor.forClass(GeneratedReport.class);
        verify(generatedReportRepository).save(reportCaptor.capture());
        assertThat(reportCaptor.getValue().getMimeType()).isEqualTo("text/csv");

        verify(mailService).sendGeneratedReport(eq(user), eq(project), any(GeneratedReport.class), contains("Filters applied"));
    }
}
