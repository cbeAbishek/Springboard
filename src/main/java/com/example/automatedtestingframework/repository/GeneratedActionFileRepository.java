package com.example.automatedtestingframework.repository;

import com.example.automatedtestingframework.model.GeneratedActionFile;
import com.example.automatedtestingframework.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GeneratedActionFileRepository extends JpaRepository<GeneratedActionFile, Long> {
    List<GeneratedActionFile> findByProject(Project project);
}
