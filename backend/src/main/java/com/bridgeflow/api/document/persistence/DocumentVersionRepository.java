package com.bridgeflow.api.document.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bridgeflow.api.document.domain.DocumentVersion;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, UUID> {
    List<DocumentVersion> findAllByDocumentIdOrderByVersionNumberDesc(UUID documentId);
    Optional<DocumentVersion> findByIdAndDocumentId(UUID id, UUID documentId);
    long countByDocumentId(UUID documentId);
}
