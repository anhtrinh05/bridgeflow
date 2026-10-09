package com.bridgeflow.api.project.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bridgeflow.api.project.domain.Project;
import com.bridgeflow.api.project.domain.ProjectStatus;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    Optional<Project> findByCode(String code);

    List<Project> findAllByOrderByUpdatedAtDesc();

    List<Project> findAllByStatusOrderByUpdatedAtDesc(ProjectStatus status);

    List<Project> findAllByStatusAndUpdatedAtBeforeOrderByUpdatedAtAsc(ProjectStatus status, Instant cutoff);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM projects WHERE id = :projectId", nativeQuery = true)
    int deleteProjectAggregateById(@Param("projectId") UUID projectId);
}
