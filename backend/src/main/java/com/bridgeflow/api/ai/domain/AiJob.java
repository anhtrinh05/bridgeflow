package com.bridgeflow.api.ai.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.bridgeflow.api.auth.domain.AppUser;
import com.bridgeflow.api.document.domain.DocumentVersion;
import com.bridgeflow.api.project.domain.Project;
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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "ai_jobs",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_ai_jobs_version_purpose", columnNames = {"document_version_id", "purpose"}),
        @UniqueConstraint(name = "uk_ai_jobs_correlation_id", columnNames = "correlation_id")
    }
)
public class AiJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_version_id")
    private DocumentVersion documentVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requirement_revision_id")
    private RequirementRevision requirementRevision;

    @Column(nullable = false, length = 40)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiJobStatus status = AiJobStatus.PENDING;

    @Column(nullable = false, length = 80)
    private String provider;

    @Column(nullable = false, length = 120)
    private String model;

    @Column(name = "correlation_id", nullable = false, updatable = false)
    private UUID correlationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by", nullable = false, updatable = false)
    private AppUser requestedBy;

    @Column(name = "candidate_count", nullable = false)
    private int candidateCount;

    @Column(name = "error_code", length = 80)
    private String errorCode;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected AiJob() {
    }

    public AiJob(
        Project project,
        DocumentVersion documentVersion,
        String provider,
        String model,
        AppUser requestedBy
    ) {
        this.project = Objects.requireNonNull(project, "project is required");
        this.documentVersion = Objects.requireNonNull(documentVersion, "documentVersion is required");
        this.purpose = "REQUIREMENT_EXTRACTION";
        this.provider = requireText(provider, "provider");
        this.model = requireText(model, "model");
        this.requestedBy = Objects.requireNonNull(requestedBy, "requestedBy is required");
        this.correlationId = UUID.randomUUID();
    }

    public AiJob(
        Project project,
        RequirementRevision requirementRevision,
        String provider,
        String model,
        AppUser requestedBy
    ) {
        this.project = Objects.requireNonNull(project, "project is required");
        this.requirementRevision = Objects.requireNonNull(requirementRevision, "requirementRevision is required");
        this.purpose = "REQUIREMENT_ANALYSIS";
        this.provider = requireText(provider, "provider");
        this.model = requireText(model, "model");
        this.requestedBy = Objects.requireNonNull(requestedBy, "requestedBy is required");
        this.correlationId = UUID.randomUUID();
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public void start() {
        if (status != AiJobStatus.PENDING && status != AiJobStatus.FAILED) {
            throw new IllegalStateException("AI job không thể bắt đầu từ trạng thái " + status + ".");
        }
        status = AiJobStatus.RUNNING;
        startedAt = Instant.now();
        completedAt = null;
        candidateCount = 0;
        errorCode = null;
        errorMessage = null;
    }

    public void complete(int count) {
        if (count < 0) throw new IllegalArgumentException("candidateCount must not be negative");
        status = AiJobStatus.COMPLETED;
        candidateCount = count;
        completedAt = Instant.now();
    }

    public void fail(String code, String message) {
        status = AiJobStatus.FAILED;
        errorCode = requireText(code, "errorCode");
        errorMessage = truncate(message == null ? "AI provider không trả về chi tiết lỗi." : message, 500);
        completedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getProjectId() { return project.getId(); }
    public UUID getDocumentVersionId() { return documentVersion == null ? null : documentVersion.getId(); }
    public UUID getRequirementRevisionId() { return requirementRevision == null ? null : requirementRevision.getId(); }
    public String getPurpose() { return purpose; }
    public AiJobStatus getStatus() { return status; }
    public String getProvider() { return provider; }
    public String getModel() { return model; }
    public UUID getCorrelationId() { return correlationId; }
    public UUID getRequestedById() { return requestedBy.getId(); }
    public int getCandidateCount() { return candidateCount; }
    public String getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }

    private static String requireText(String value, String fieldName) {
        var normalized = Objects.requireNonNull(value, fieldName + " is required").trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(fieldName + " must not be blank");
        return normalized;
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
