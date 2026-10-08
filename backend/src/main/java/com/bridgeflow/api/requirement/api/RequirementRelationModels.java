package com.bridgeflow.api.requirement.api;

import java.time.Instant;
import java.util.UUID;

import com.bridgeflow.api.requirement.domain.RequirementRelationType;

import jakarta.validation.constraints.NotNull;

public final class RequirementRelationModels {
    private RequirementRelationModels() { }

    public record CreateRequirementRelationRequest(
        @NotNull UUID targetRequirementId,
        @NotNull RequirementRelationType relationType
    ) { }

    public record RequirementRelationResponse(
        UUID id,
        UUID sourceRequirementId,
        String sourceDisplayKey,
        UUID targetRequirementId,
        String targetDisplayKey,
        RequirementRelationType relationType,
        UUID createdBy,
        Instant createdAt
    ) { }
}
