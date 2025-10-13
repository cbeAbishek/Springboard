package com.example.automatedtestingframework.repository;

import com.example.automatedtestingframework.model.Project;
import com.example.automatedtestingframework.model.Report;
import com.example.automatedtestingframework.model.TestCaseType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;

public interface ReportRepository extends JpaRepository<Report, Long> {

        long countByProject(Project project);

        @Query("""
                SELECT COUNT(r) FROM Report r
                WHERE r.project = :project
                  AND r.status = :status
                  AND r.startedAt BETWEEN :start AND :end
                """)
        long countByProjectAndStatusBetween(@Param("project") Project project,
                                                                                @Param("status") String status,
                                                                                @Param("start") OffsetDateTime start,
                                                                                @Param("end") OffsetDateTime end);

        @Query("""
                SELECT r FROM Report r
                LEFT JOIN r.testCase tc
                WHERE r.project = :project
                  AND (:type IS NULL OR (tc IS NOT NULL AND tc.type = :type))
                  AND (:status IS NULL OR :status = '' OR r.status = :status)
                  AND (:from IS NULL OR r.startedAt >= :from)
                  AND (:to IS NULL OR r.startedAt <= :to)
                """)
        Page<Report> searchReports(@Param("project") Project project,
                                                           @Param("type") TestCaseType type,
                                                           @Param("status") String status,
                                                           @Param("from") OffsetDateTime from,
                                                           @Param("to") OffsetDateTime to,
                                                           Pageable pageable);
}
