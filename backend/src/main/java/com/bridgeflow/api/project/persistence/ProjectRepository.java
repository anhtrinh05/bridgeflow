package com.bridgeflow.api.project.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bridgeflow.api.project.domain.Project;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    Optional<Project> findByCode(String code);

    List<Project> findAllByOrderByUpdatedAtDesc();
}
