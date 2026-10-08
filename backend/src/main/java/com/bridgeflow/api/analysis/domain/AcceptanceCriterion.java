package com.bridgeflow.api.analysis.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.bridgeflow.api.ai.domain.AiJob;
import com.bridgeflow.api.requirement.domain.RequirementRevision;

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
import jakarta.persistence.Table;

@Entity
@Table(name = "acceptance_criteria")
public class AcceptanceCriterion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requirement_revision_id", nullable = false)
    private RequirementRevision requirementRevision;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ai_job_id", nullable = false)
    private AiJob aiJob;

    @Column(name = "japanese_text", nullable = false, columnDefinition = "text")
    private String japaneseText;

    @Column(name = "vietnamese_text", nullable = false, columnDefinition = "text")
    private String vietnameseText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ArtifactReviewStatus status = ArtifactReviewStatus.DRAFT;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    protected AcceptanceCriterion() {
    }

    public AcceptanceCriterion(
        RequirementRevision requirementRevision,
        AiJob aiJob,
        String japaneseText,
        String vietnameseText,
        UUID createdBy
    ) {
        this.requirementRevision = Objects.requireNonNull(requirementRevision, "requirementRevision is required");
        this.aiJob = Objects.requireNonNull(aiJob, "aiJob is required");
        this.japaneseText = requireText(japaneseText, "japaneseText");
        this.vietnameseText = requireText(vietnameseText, "vietnameseText");
        this.createdBy = Objects.requireNonNull(createdBy, "createdBy is required");
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public void review(ArtifactReviewStatus decision, UUID reviewerId) {
        if (decision == ArtifactReviewStatus.DRAFT) throw new IllegalArgumentException("Review decision must be final");
        status = Objects.requireNonNull(decision, "decision is required");
        reviewedBy = Objects.requireNonNull(reviewerId, "reviewerId is required");
        reviewedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getRequirementRevisionId() { return requirementRevision.getId(); }
    public UUID getAiJobId() { return aiJob.getId(); }
    public String getJapaneseText() { return japaneseText; }
    public String getVietnameseText() { return vietnameseText; }
    public ArtifactReviewStatus getStatus() { return status; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public UUID getReviewedBy() { return reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; }

    private static String requireText(String value, String fieldName) {
        var normalized = Objects.requireNonNull(value, fieldName + " is required").trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(fieldName + " must not be blank");
        return normalized;
    }
}
