package com.bridgeflow.api.ai.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public final class AiModels {

    private AiModels() {
    }

    public record AiJobResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID projectId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) UUID documentVersionId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) UUID requirementRevisionId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String purpose,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String provider,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String model,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID correlationId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID requestedBy,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int candidateCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<UUID> requirementIds,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<UUID> clarificationQuestionIds,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<UUID> acceptanceCriterionIds,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String errorCode,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String errorMessage,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Instant startedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Instant completedAt
    ) {
    }
}
