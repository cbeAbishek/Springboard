package com.example.automatedtestingframework.repository;

import com.example.automatedtestingframework.model.GeneratedReport;
import com.example.automatedtestingframework.model.Project;
import com.example.automatedtestingframework.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GeneratedReportRepository extends JpaRepository<GeneratedReport, Long> {

    List<GeneratedReport> findTop10ByUserAndProjectOrderByCreatedAtDesc(User user, Project project);
}
