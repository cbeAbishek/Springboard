package com.example.automatedtestingframework.repository;

import com.example.automatedtestingframework.model.Project;
import com.example.automatedtestingframework.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByOwner(User owner);
    Optional<Project> findByOwnerAndNameIgnoreCase(User owner, String name);
    Optional<Project> findByIdAndOwner(Long id, User owner);
}
