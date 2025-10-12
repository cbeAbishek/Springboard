package org.example.repository;

import org.example.model.TestBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TestBatchRepository extends JpaRepository<TestBatch, Long> {

    List<TestBatch> findByStatus(TestBatch.BatchStatus status);

    List<TestBatch> findByEnvironment(String environment);

    List<TestBatch> findTop10ByOrderByCreatedAtDesc();

    List<TestBatch> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    List<TestBatch> findByStatusOrderByCreatedAtDesc(TestBatch.BatchStatus status);

    @Query("SELECT b FROM TestBatch b ORDER BY b.createdAt DESC")
    List<TestBatch> findTopRecentBatches(int limit);

    @Query("SELECT b FROM TestBatch b WHERE b.status = 'RUNNING' ORDER BY b.createdAt ASC")
    List<TestBatch> findRunningBatches();

    @Query("SELECT COUNT(b) FROM TestBatch b WHERE b.status = :status")
    long countByStatus(TestBatch.BatchStatus status);

    @Query("SELECT b FROM TestBatch b WHERE b.createdBy = :createdBy ORDER BY b.createdAt DESC")
    List<TestBatch> findByCreatedByOrderByCreatedAtDesc(String createdBy);

    void deleteByCreatedAtBefore(LocalDateTime cutoffDate);
}
