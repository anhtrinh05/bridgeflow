package com.bridgeflow.api.project.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bridgeflow.api.project.domain.ProjectDeletionReceipt;

public interface ProjectDeletionReceiptRepository extends JpaRepository<ProjectDeletionReceipt, UUID> {
    List<ProjectDeletionReceipt> findAllByProjectIdOrderByDeletedAtDesc(UUID projectId);
}
