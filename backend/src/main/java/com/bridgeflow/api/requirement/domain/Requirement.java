package com.bridgeflow.api.requirement.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.bridgeflow.api.project.domain.Project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "requirements",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_requirements_project_display_key",
        columnNames = {"project_id", "display_key"}
    )
)
public class Requirement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "display_key", nullable = false, length = 40)
    private String displayKey;

    @Column(name = "current_revision_id")
    private UUID currentRevisionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequirementStatus status = RequirementStatus.DRAFT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    protected Requirement() {
    }

    public Requirement(Project project, String displayKey) {
        this.project = Objects.requireNonNull(project, "project is required");
        this.displayKey = requireText(displayKey, "displayKey");
    }

    @PrePersist
    void onCreate() {
        var now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void pointToRevision(RequirementRevision revision) {
        Objects.requireNonNull(revision, "revision is required");
        if (id == null || !Objects.equals(id, revision.getRequirement().getId())) {
            throw new IllegalArgumentException("revision must belong to this requirement");
        }
        if (revision.getReviewStatus() != ReviewStatus.CONFIRMED) {
            throw new IllegalArgumentException("current revision must be confirmed by a reviewer");
        }
        currentRevisionId = Objects.requireNonNull(revision.getId(), "revision must be persisted first");
        status = RequirementStatus.CONFIRMED;
    }

    public UUID getId() {
        return id;
    }

    public Project getProject() {
        return project;
    }

    public String getDisplayKey() {
        return displayKey;
    }

    public UUID getCurrentRevisionId() {
        return currentRevisionId;
    }

    public RequirementStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    private static String requireText(String value, String fieldName) {
        var normalized = Objects.requireNonNull(value, fieldName + " is required").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
