package com.example.automatedtestingframework.repository;

import com.example.automatedtestingframework.model.Project;
import com.example.automatedtestingframework.model.TestCase;
import com.example.automatedtestingframework.model.TestCaseType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TestCaseRepository extends JpaRepository<TestCase, Long> {
    List<TestCase> findByProject(Project project);
    List<TestCase> findByProjectAndType(Project project, TestCaseType type);
    Optional<TestCase> findByProjectAndNameIgnoreCase(Project project, String name);
}
