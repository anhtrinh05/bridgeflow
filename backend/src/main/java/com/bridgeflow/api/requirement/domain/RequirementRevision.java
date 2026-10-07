package com.bridgeflow.api.requirement.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "requirement_revisions",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_requirement_revisions_number",
        columnNames = {"requirement_id", "revision_number"}
    )
)
public class RequirementRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requirement_id", nullable = false)
    private Requirement requirement;

    @Column(name = "revision_number", nullable = false)
    private int revisionNumber;

    @Column(name = "document_version_id")
    private UUID documentVersionId;

    @Column(name = "source_anchor", length = 500)
    private String sourceAnchor;

    @Column(name = "japanese_text", nullable = false, columnDefinition = "text")
    private String japaneseText;

    @Column(name = "vietnamese_text", nullable = false, columnDefinition = "text")
    private String vietnameseText;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 20)
    private ChangeType changeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 20)
    private ReviewStatus reviewStatus = ReviewStatus.DRAFT;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "confirmed_by")
    private UUID confirmedBy;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    protected RequirementRevision() {
    }

    public RequirementRevision(
        Requirement requirement,
        int revisionNumber,
        String japaneseText,
        String vietnameseText,
        ChangeType changeType
    ) {
        this(requirement, revisionNumber, japaneseText, vietnameseText, changeType, null);
    }

    public RequirementRevision(
        Requirement requirement,
        int revisionNumber,
        String japaneseText,
        String vietnameseText,
        ChangeType changeType,
        UUID createdBy
    ) {
        this(requirement, revisionNumber, japaneseText, vietnameseText, changeType, createdBy, null, null);
    }

    public RequirementRevision(
        Requirement requirement,
        int revisionNumber,
        String japaneseText,
        String vietnameseText,
        ChangeType changeType,
        UUID createdBy,
        UUID documentVersionId,
        String sourceAnchor
    ) {
        if (revisionNumber < 1) {
            throw new IllegalArgumentException("revisionNumber must be at least 1");
        }
        this.requirement = Objects.requireNonNull(requirement, "requirement is required");
        this.revisionNumber = revisionNumber;
        this.japaneseText = requireText(japaneseText, "japaneseText");
        this.vietnameseText = requireText(vietnameseText, "vietnameseText");
        this.changeType = Objects.requireNonNull(changeType, "changeType is required");
        this.createdBy = createdBy;
        this.documentVersionId = documentVersionId;
        this.sourceAnchor = normalizeOptionalText(sourceAnchor);
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public void confirm(UUID reviewerId) {
        Objects.requireNonNull(reviewerId, "reviewerId is required");
        if (reviewStatus == ReviewStatus.CONFIRMED) {
            throw new IllegalStateException("revision is already confirmed");
        }
        reviewStatus = ReviewStatus.CONFIRMED;
        confirmedBy = reviewerId;
        confirmedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Requirement getRequirement() {
        return requirement;
    }

    public int getRevisionNumber() {
        return revisionNumber;
    }

    public UUID getDocumentVersionId() {
        return documentVersionId;
    }

    public String getSourceAnchor() {
        return sourceAnchor;
    }

    public String getJapaneseText() {
        return japaneseText;
    }

    public String getVietnameseText() {
        return vietnameseText;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public ReviewStatus getReviewStatus() {
        return reviewStatus;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public UUID getConfirmedBy() {
        return confirmedBy;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    private static String requireText(String value, String fieldName) {
        var normalized = Objects.requireNonNull(value, fieldName + " is required").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) return null;
        var normalized = value.trim();
        return normalized.length() <= 500 ? normalized : normalized.substring(0, 500);
    }
}
