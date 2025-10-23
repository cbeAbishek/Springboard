package com.example.automatedtestingframework.repository;

import com.example.automatedtestingframework.model.EndpointAnalysisResult;
import com.example.automatedtestingframework.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EndpointAnalysisResultRepository extends JpaRepository<EndpointAnalysisResult, Long> {
    List<EndpointAnalysisResult> findTop10ByProjectOrderByExecutedAtDesc(Project project);
}
