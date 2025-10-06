package org.automation.reports.repository;

import org.automation.reports.model.TestReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for TestReport entity with advanced query capabilities
 */
@Repository
public interface TestReportRepository extends JpaRepository<TestReport, Long> {

    Optional<TestReport> findByReportId(String reportId);

    List<TestReport> findBySuiteType(String suiteType);

    List<TestReport> findByStatus(String status);

    List<TestReport> findByCreatedBy(String createdBy);

    @Query("SELECT r FROM TestReport r WHERE r.executionDate BETWEEN :startDate AND :endDate ORDER BY r.executionDate DESC")
    List<TestReport> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT r FROM TestReport r WHERE r.status = :status AND r.executionDate >= :date")
    List<TestReport> findRecentByStatus(@Param("status") String status, @Param("date") LocalDateTime date);

    @Query("SELECT r FROM TestReport r ORDER BY r.executionDate DESC")
    List<TestReport> findAllOrderByDateDesc();

    @Query("SELECT r FROM TestReport r ORDER BY r.executionDate DESC")
    List<TestReport> findAllOrderByExecutionDateDesc();

    @Query("SELECT r FROM TestReport r WHERE r.suiteType = :suiteType AND r.status = :status")
    List<TestReport> findBySuiteTypeAndStatus(@Param("suiteType") String suiteType, @Param("status") String status);

    @Query("SELECT COUNT(r) FROM TestReport r WHERE r.status = 'COMPLETED'")
    long countCompleted();

    @Query("SELECT AVG(r.successRate) FROM TestReport r WHERE r.status = 'COMPLETED'")
    Double getAverageSuccessRate();

    @Query("SELECT r FROM TestReport r WHERE r.browser = :browser ORDER BY r.executionDate DESC")
    List<TestReport> findByBrowser(@Param("browser") String browser);

    /**
     * Find reports with multiple filter criteria
     */
    @Query("SELECT r FROM TestReport r WHERE " +
           "(:suiteType IS NULL OR r.suiteType = :suiteType) AND " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:browser IS NULL OR r.browser = :browser) AND " +
           "(:startDate IS NULL OR r.executionDate >= :startDate) AND " +
           "(:endDate IS NULL OR r.executionDate <= :endDate) " +
           "ORDER BY r.executionDate DESC")
    List<TestReport> findReportsWithFilters(
        @Param("suiteType") String suiteType,
        @Param("status") String status,
        @Param("browser") String browser,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}

