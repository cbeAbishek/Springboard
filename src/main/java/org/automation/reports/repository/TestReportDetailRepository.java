package org.automation.reports.repository;

import org.automation.reports.model.TestReportDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for TestReportDetail entity
 */
@Repository
public interface TestReportDetailRepository extends JpaRepository<TestReportDetail, Long> {

    List<TestReportDetail> findByReportId(Long reportId);

    List<TestReportDetail> findByStatus(String status);

    List<TestReportDetail> findByTestType(String testType);

    @Query("SELECT d FROM TestReportDetail d WHERE d.report.reportId = :reportId")
    List<TestReportDetail> findByReportReportId(@Param("reportId") String reportId);

    @Query("SELECT d FROM TestReportDetail d WHERE d.report.reportId = :reportId")
    List<TestReportDetail> findByReport_ReportId(@Param("reportId") String reportId);

    @Query("SELECT d FROM TestReportDetail d WHERE d.report.reportId = :reportId AND d.status = :status")
    List<TestReportDetail> findByReportIdAndStatus(@Param("reportId") String reportId, @Param("status") String status);

    @Query("SELECT d FROM TestReportDetail d WHERE d.testName LIKE %:testName%")
    List<TestReportDetail> findByTestNameContaining(@Param("testName") String testName);

    @Query("SELECT d FROM TestReportDetail d WHERE d.screenshotPath IS NOT NULL")
    List<TestReportDetail> findAllWithScreenshots();

    @Query("SELECT d FROM TestReportDetail d WHERE d.report.reportId = :reportId AND d.status IN ('FAIL', 'FAILED') AND d.screenshotPath IS NOT NULL")
    List<TestReportDetail> findFailedTestsWithScreenshots(@Param("reportId") String reportId);

    @Query("SELECT COUNT(d) FROM TestReportDetail d WHERE d.status = :status")
    long countByStatus(@Param("status") String status);
}
