package com.bridgeflow.api.project.domain;

import java.time.Instant;
import java.util.UUID;

import com.bridgeflow.api.project.application.ProjectDeletionReason;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "project_deletion_receipts")
public class ProjectDeletionReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "project_id", nullable = false, updatable = false)
    private UUID projectId;

    @Column(name = "project_code", nullable = false, updatable = false, length = 40)
    private String projectCode;

    @Column(name = "deleted_by", updatable = false)
    private UUID deletedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private ProjectDeletionReason reason;

    @Column(name = "deleted_at", nullable = false, updatable = false)
    private Instant deletedAt;

    protected ProjectDeletionReceipt() {
    }

    public ProjectDeletionReceipt(UUID projectId, String projectCode, UUID deletedBy, ProjectDeletionReason reason) {
        this.projectId = projectId;
        this.projectCode = projectCode;
        this.deletedBy = deletedBy;
        this.reason = reason;
    }

    @PrePersist
    void onCreate() {
        deletedAt = Instant.now();
    }

    public UUID getProjectId() {
        return projectId;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public UUID getDeletedBy() {
        return deletedBy;
    }

    public ProjectDeletionReason getReason() {
        return reason;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
