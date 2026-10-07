package com.bridgeflow.api.glossary.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.bridgeflow.api.auth.domain.AppUser;
import com.bridgeflow.api.project.domain.Project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    name = "glossary_terms",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_glossary_terms_project_japanese", columnNames = {"project_id", "japanese_term"}
    )
)
public class GlossaryTerm {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "japanese_term", nullable = false, length = 160)
    private String japaneseTerm;

    @Column(name = "vietnamese_term", nullable = false, length = 240)
    private String vietnameseTerm;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private AppUser createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected GlossaryTerm() {
    }

    public GlossaryTerm(
        Project project,
        String japaneseTerm,
        String vietnameseTerm,
        String notes,
        AppUser createdBy
    ) {
        this.project = Objects.requireNonNull(project, "project is required");
        this.createdBy = Objects.requireNonNull(createdBy, "createdBy is required");
        update(japaneseTerm, vietnameseTerm, notes);
    }

    public void update(String japaneseTerm, String vietnameseTerm, String notes) {
        this.japaneseTerm = requireText(japaneseTerm, "japaneseTerm");
        this.vietnameseTerm = requireText(vietnameseTerm, "vietnameseTerm");
        this.notes = normalizeOptional(notes);
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

    public UUID getId() { return id; }
    public Project getProject() { return project; }
    public String getJapaneseTerm() { return japaneseTerm; }
    public String getVietnameseTerm() { return vietnameseTerm; }
    public String getNotes() { return notes; }
    public UUID getCreatedById() { return createdBy.getId(); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    private static String requireText(String value, String fieldName) {
        var normalized = Objects.requireNonNull(value, fieldName + " is required").trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(fieldName + " must not be blank");
        return normalized;
    }

    private static String normalizeOptional(String value) {
        if (value == null) return null;
        var normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
