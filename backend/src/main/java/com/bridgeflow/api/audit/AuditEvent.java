package com.bridgeflow.api.audit;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

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
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "actor_user_id", nullable = false)
    private UUID actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private AuditAction action;

    @Column(name = "entity_type", nullable = false, length = 40)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected AuditEvent() {
    }

    public AuditEvent(UUID projectId, UUID actorUserId, AuditAction action, String entityType, UUID entityId) {
        this.projectId = Objects.requireNonNull(projectId, "projectId is required");
        this.actorUserId = Objects.requireNonNull(actorUserId, "actorUserId is required");
        this.action = Objects.requireNonNull(action, "action is required");
        this.entityType = Objects.requireNonNull(entityType, "entityType is required");
        this.entityId = Objects.requireNonNull(entityId, "entityId is required");
    }

    @PrePersist
    void onCreate() { occurredAt = Instant.now(); }

    public AuditAction getAction() { return action; }
    public UUID getActorUserId() { return actorUserId; }
}
