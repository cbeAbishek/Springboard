package org.example.repository;

import org.example.model.TestSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TestScheduleRepository extends JpaRepository<TestSchedule, Long> {

    List<TestSchedule> findByIsActiveTrue();

    List<TestSchedule> findByTestType(String testType);

    List<TestSchedule> findByEnvironment(String environment);

    List<TestSchedule> findByCreatedBy(String createdBy);

    @Query("SELECT s FROM TestSchedule s WHERE s.nextExecution <= :currentTime AND s.isActive = true")
    List<TestSchedule> findSchedulesDueForExecution(LocalDateTime currentTime);

    @Query("SELECT s FROM TestSchedule s WHERE s.nextExecution <= :currentTime AND s.isActive = true")
    List<TestSchedule> findDueSchedules(LocalDateTime currentTime);

    @Query("SELECT s FROM TestSchedule s WHERE s.lastExecution IS NULL AND s.isActive = true")
    List<TestSchedule> findNeverExecutedSchedules();

    @Query("SELECT COUNT(s) FROM TestSchedule s WHERE s.isActive = true")
    long countActiveSchedules();

    void deleteByCreatedAtBefore(LocalDateTime cutoffDate);
}
