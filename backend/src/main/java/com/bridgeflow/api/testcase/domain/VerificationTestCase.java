package com.bridgeflow.api.testcase.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.bridgeflow.api.ai.domain.AiJob;
import com.bridgeflow.api.analysis.domain.AcceptanceCriterion;
import com.bridgeflow.api.analysis.domain.ArtifactReviewStatus;
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
@Table(name = "verification_test_cases")
public class VerificationTestCase {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requirement_revision_id", nullable = false)
    private RequirementRevision requirementRevision;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "acceptance_criterion_id", nullable = false)
    private AcceptanceCriterion acceptanceCriterion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ai_job_id", nullable = false)
    private AiJob aiJob;

    @Column(name = "title_japanese", nullable = false, columnDefinition = "text") private String titleJapanese;
    @Column(name = "title_vietnamese", nullable = false, columnDefinition = "text") private String titleVietnamese;
    @Column(name = "preconditions_japanese", nullable = false, columnDefinition = "text") private String preconditionsJapanese;
    @Column(name = "preconditions_vietnamese", nullable = false, columnDefinition = "text") private String preconditionsVietnamese;
    @Column(name = "steps_japanese", nullable = false, columnDefinition = "text") private String stepsJapanese;
    @Column(name = "steps_vietnamese", nullable = false, columnDefinition = "text") private String stepsVietnamese;
    @Column(name = "expected_result_japanese", nullable = false, columnDefinition = "text") private String expectedResultJapanese;
    @Column(name = "expected_result_vietnamese", nullable = false, columnDefinition = "text") private String expectedResultVietnamese;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private TestCasePriority priority;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private ArtifactReviewStatus status = ArtifactReviewStatus.DRAFT;

    @Column(name = "created_by", nullable = false, updatable = false) private UUID createdBy;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "reviewed_by") private UUID reviewedBy;
    @Column(name = "reviewed_at") private Instant reviewedAt;

    protected VerificationTestCase() { }

    public VerificationTestCase(
        RequirementRevision revision, AcceptanceCriterion criterion, AiJob job,
        String titleJapanese, String titleVietnamese,
        String preconditionsJapanese, String preconditionsVietnamese,
        String stepsJapanese, String stepsVietnamese,
        String expectedResultJapanese, String expectedResultVietnamese,
        TestCasePriority priority, UUID createdBy
    ) {
        this.requirementRevision = Objects.requireNonNull(revision, "revision is required");
        this.acceptanceCriterion = Objects.requireNonNull(criterion, "criterion is required");
        this.aiJob = Objects.requireNonNull(job, "job is required");
        this.titleJapanese = requireText(titleJapanese, "titleJapanese");
        this.titleVietnamese = requireText(titleVietnamese, "titleVietnamese");
        this.preconditionsJapanese = requireText(preconditionsJapanese, "preconditionsJapanese");
        this.preconditionsVietnamese = requireText(preconditionsVietnamese, "preconditionsVietnamese");
        this.stepsJapanese = requireText(stepsJapanese, "stepsJapanese");
        this.stepsVietnamese = requireText(stepsVietnamese, "stepsVietnamese");
        this.expectedResultJapanese = requireText(expectedResultJapanese, "expectedResultJapanese");
        this.expectedResultVietnamese = requireText(expectedResultVietnamese, "expectedResultVietnamese");
        this.priority = Objects.requireNonNull(priority, "priority is required");
        this.createdBy = Objects.requireNonNull(createdBy, "createdBy is required");
    }

    @PrePersist void onCreate() { createdAt = Instant.now(); }

    public void review(ArtifactReviewStatus decision, UUID reviewerId) {
        if (decision == ArtifactReviewStatus.DRAFT) throw new IllegalArgumentException("Review decision must be final");
        status = Objects.requireNonNull(decision, "decision is required");
        reviewedBy = Objects.requireNonNull(reviewerId, "reviewerId is required");
        reviewedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getRequirementRevisionId() { return requirementRevision.getId(); }
    public UUID getAcceptanceCriterionId() { return acceptanceCriterion.getId(); }
    public UUID getAiJobId() { return aiJob.getId(); }
    public String getTitleJapanese() { return titleJapanese; }
    public String getTitleVietnamese() { return titleVietnamese; }
    public String getPreconditionsJapanese() { return preconditionsJapanese; }
    public String getPreconditionsVietnamese() { return preconditionsVietnamese; }
    public String getStepsJapanese() { return stepsJapanese; }
    public String getStepsVietnamese() { return stepsVietnamese; }
    public String getExpectedResultJapanese() { return expectedResultJapanese; }
    public String getExpectedResultVietnamese() { return expectedResultVietnamese; }
    public TestCasePriority getPriority() { return priority; }
    public ArtifactReviewStatus getStatus() { return status; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public UUID getReviewedBy() { return reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; }

    private static String requireText(String value, String field) {
        var normalized = Objects.requireNonNull(value, field + " is required").trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
        return normalized;
    }
}
