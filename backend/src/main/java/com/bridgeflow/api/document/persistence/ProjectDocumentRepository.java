package com.bridgeflow.api.document.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bridgeflow.api.document.domain.DocumentStatus;
import com.bridgeflow.api.document.domain.ProjectDocument;

public interface ProjectDocumentRepository extends JpaRepository<ProjectDocument, UUID> {
    List<ProjectDocument> findAllByProjectIdAndStatusOrderByUpdatedAtDesc(UUID projectId, DocumentStatus status);
    List<ProjectDocument> findAllByProjectIdOrderByUpdatedAtDesc(UUID projectId);
}
