package com.bridgeflow.api.requirement.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bridgeflow.api.requirement.domain.RequirementRevision;

public interface RequirementRevisionRepository extends JpaRepository<RequirementRevision, UUID> {

    List<RequirementRevision> findByRequirementIdOrderByRevisionNumberAsc(UUID requirementId);
}
