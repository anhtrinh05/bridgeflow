package com.bridgeflow.api.project.membership;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {

    @EntityGraph(attributePaths = "project")
    List<ProjectMember> findAllByUserIdOrderByProjectUpdatedAtDesc(UUID userId);

    @EntityGraph(attributePaths = {"project", "user"})
    Optional<ProjectMember> findByProjectIdAndUserId(UUID projectId, UUID userId);

    boolean existsByProjectIdAndUserId(UUID projectId, UUID userId);
}
