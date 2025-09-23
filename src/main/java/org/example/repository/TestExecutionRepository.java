package org.example.repository;

import org.example.model.TestExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TestExecutionRepository extends JpaRepository<TestExecution, Long> {

    List<TestExecution> findByTestCaseIdOrderByStartTimeDesc(Long testCaseId);

    List<TestExecution> findTop10ByOrderByStartTimeDesc();

    List<TestExecution> findByStartTimeBefore(LocalDateTime cutoffDate);

    long countByStatus(TestExecution.ExecutionStatus status);

    List<TestExecution> findByStatus(TestExecution.ExecutionStatus status);

    List<TestExecution> findByEnvironment(String environment);

    @Query("SELECT e FROM TestExecution e WHERE e.startTime >= :startTime AND e.startTime <= :endTime ORDER BY e.startTime DESC")
    List<TestExecution> findByDateRange(LocalDateTime startTime, LocalDateTime endTime);

    @Query("SELECT e FROM TestExecution e WHERE e.testBatch.id = :batchId ORDER BY e.startTime DESC")
    List<TestExecution> findByBatchIdOrderByStartTimeDesc(Long batchId);

    @Query("SELECT e FROM TestExecution e WHERE e.environment = :environment ORDER BY e.startTime DESC")
    List<TestExecution> findByEnvironmentOrderByStartTimeDesc(String environment);

    @Query("SELECT e FROM TestExecution e WHERE e.status = :status AND e.startTime >= :startTime")
    List<TestExecution> findByStatusAndStartTimeAfter(TestExecution.ExecutionStatus status, LocalDateTime startTime);

    @Query("SELECT COUNT(e) FROM TestExecution e WHERE e.startTime >= :startTime AND e.status = 'PASSED'")
    long countPassedExecutionsSince(LocalDateTime startTime);

    @Query("SELECT COUNT(e) FROM TestExecution e WHERE e.startTime >= :startTime AND e.status = 'FAILED'")
    long countFailedExecutionsSince(LocalDateTime startTime);

    void deleteByStartTimeBefore(LocalDateTime cutoffDate);
}
