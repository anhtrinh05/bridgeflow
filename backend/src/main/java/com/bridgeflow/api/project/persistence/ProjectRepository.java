package com.bridgeflow.api.project.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bridgeflow.api.project.domain.Project;
import com.bridgeflow.api.project.domain.ProjectStatus;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    Optional<Project> findByCode(String code);

    List<Project> findAllByOrderByUpdatedAtDesc();

    List<Project> findAllByStatusOrderByUpdatedAtDesc(ProjectStatus status);
}
