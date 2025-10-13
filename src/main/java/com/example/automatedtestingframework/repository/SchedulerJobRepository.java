package com.example.automatedtestingframework.repository;

import com.example.automatedtestingframework.model.Project;
import com.example.automatedtestingframework.model.SchedulerFrequency;
import com.example.automatedtestingframework.model.SchedulerJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SchedulerJobRepository extends JpaRepository<SchedulerJob, Long> {
    List<SchedulerJob> findByProject(Project project);
    List<SchedulerJob> findByProjectAndActiveTrue(Project project);
    List<SchedulerJob> findByFrequency(SchedulerFrequency frequency);
}
