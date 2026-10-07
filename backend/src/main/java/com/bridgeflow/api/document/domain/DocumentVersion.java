package com.bridgeflow.api.document.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.bridgeflow.api.auth.domain.AppUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "document_versions",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_document_versions_number", columnNames = {"document_id", "version_number"}
    )
)
public class DocumentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private ProjectDocument document;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 120)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(nullable = false, length = 64)
    private String sha256;

    @Column(name = "storage_key", nullable = false, unique = true, length = 500)
    private String storageKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by", nullable = false, updatable = false)
    private AppUser uploadedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected DocumentVersion() {
    }

    public DocumentVersion(
        ProjectDocument document,
        int versionNumber,
        String originalFilename,
        String contentType,
        long sizeBytes,
        String sha256,
        String storageKey,
        AppUser uploadedBy
    ) {
        this.document = Objects.requireNonNull(document, "document is required");
        if (versionNumber < 1) throw new IllegalArgumentException("versionNumber must be positive");
        this.versionNumber = versionNumber;
        this.originalFilename = Objects.requireNonNull(originalFilename, "originalFilename is required");
        this.contentType = Objects.requireNonNull(contentType, "contentType is required");
        if (sizeBytes < 1) throw new IllegalArgumentException("sizeBytes must be positive");
        this.sizeBytes = sizeBytes;
        this.sha256 = Objects.requireNonNull(sha256, "sha256 is required");
        this.storageKey = Objects.requireNonNull(storageKey, "storageKey is required");
        this.uploadedBy = Objects.requireNonNull(uploadedBy, "uploadedBy is required");
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public ProjectDocument getDocument() { return document; }
    public int getVersionNumber() { return versionNumber; }
    public String getOriginalFilename() { return originalFilename; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public String getSha256() { return sha256; }
    public String getStorageKey() { return storageKey; }
    public UUID getUploadedById() { return uploadedBy.getId(); }
    public Instant getCreatedAt() { return createdAt; }
}
