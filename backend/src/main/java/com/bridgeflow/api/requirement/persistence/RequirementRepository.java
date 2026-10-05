package com.bridgeflow.api.requirement.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bridgeflow.api.requirement.domain.Requirement;

public interface RequirementRepository extends JpaRepository<Requirement, UUID> {

    Optional<Requirement> findByProjectIdAndDisplayKey(UUID projectId, String displayKey);

    List<Requirement> findByProjectIdOrderByDisplayKeyAsc(UUID projectId);

    long countByProjectId(UUID projectId);
}
