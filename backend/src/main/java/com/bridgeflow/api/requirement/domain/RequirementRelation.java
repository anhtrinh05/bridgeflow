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
    name = "requirement_relations",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_requirement_relations_edge",
        columnNames = {"source_requirement_id", "target_requirement_id", "relation_type"}
    )
)
public class RequirementRelation {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_requirement_id", nullable = false)
    private Requirement sourceRequirement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_requirement_id", nullable = false)
    private Requirement targetRequirement;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", nullable = false, length = 30)
    private RequirementRelationType relationType;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RequirementRelation() { }

    public RequirementRelation(
        Requirement sourceRequirement, Requirement targetRequirement,
        RequirementRelationType relationType, UUID createdBy
    ) {
        this.sourceRequirement = Objects.requireNonNull(sourceRequirement, "sourceRequirement is required");
        this.targetRequirement = Objects.requireNonNull(targetRequirement, "targetRequirement is required");
        this.relationType = Objects.requireNonNull(relationType, "relationType is required");
        this.createdBy = Objects.requireNonNull(createdBy, "createdBy is required");
    }

    @PrePersist void onCreate() { createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public Requirement getSourceRequirement() { return sourceRequirement; }
    public Requirement getTargetRequirement() { return targetRequirement; }
    public RequirementRelationType getRelationType() { return relationType; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
