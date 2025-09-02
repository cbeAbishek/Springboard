package org.example.repository;

import org.example.model.TestSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestScheduleRepository extends JpaRepository<TestSchedule, Long> {

    List<TestSchedule> findByIsActiveTrue();

    List<TestSchedule> findByEnvironment(String environment);

    List<TestSchedule> findByTestSuite(String testSuite);
}
