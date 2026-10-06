package com.bridgeflow.api.audit;

import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditEventRepository repository;

    public AuditService(AuditEventRepository repository) {
        this.repository = repository;
    }

    public void record(UUID projectId, UUID actorUserId, AuditAction action, String entityType, UUID entityId) {
        repository.save(new AuditEvent(projectId, actorUserId, action, entityType, entityId));
    }
}
