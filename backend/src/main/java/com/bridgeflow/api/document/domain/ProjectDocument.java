package com.bridgeflow.api.document.domain;

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

@Entity
@Table(name = "documents")
public class ProjectDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentStatus status = DocumentStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    protected ProjectDocument() {
    }

    public ProjectDocument(Project project, String title) {
        this.project = Objects.requireNonNull(project, "project is required");
        this.title = requireTitle(title);
    }

    public void archive() {
        if (status == DocumentStatus.ARCHIVED) throw new IllegalStateException("Tài liệu đã archive.");
        status = DocumentStatus.ARCHIVED;
        archivedAt = Instant.now();
    }

    @PrePersist
    void onCreate() {
        var now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public Project getProject() { return project; }
    public String getTitle() { return title; }
    public DocumentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getArchivedAt() { return archivedAt; }

    private static String requireTitle(String value) {
        var normalized = Objects.requireNonNull(value, "title is required").trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException("title must not be blank");
        return normalized;
    }
}
