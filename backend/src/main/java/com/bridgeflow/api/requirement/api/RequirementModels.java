package com.bridgeflow.api.requirement.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.bridgeflow.api.requirement.domain.ChangeType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class RequirementModels {

    private RequirementModels() {
    }

    public record CreateRequirementRequest(
        @NotBlank @Size(max = 40) String displayKey,
        @NotBlank String japaneseText,
        @NotBlank String vietnameseText
    ) {
    }

    public record CreateRevisionRequest(
        @NotBlank String japaneseText,
        @NotBlank String vietnameseText,
        @NotNull ChangeType changeType
    ) {
    }

    public record RevisionResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int revisionNumber,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String japaneseText,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String vietnameseText,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String changeType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String reviewStatus,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) UUID documentVersionId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String sourceAnchor,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) UUID confirmedBy,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Instant confirmedAt
    ) {
    }

    public record RequirementResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID projectId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String displayKey,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) UUID currentRevisionId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) RevisionResponse latestRevision,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<RevisionResponse> revisions,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant updatedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Instant archivedAt
    ) {
    }

    public record RequirementPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<RequirementResponse> items,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int page,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int size,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalElements,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int totalPages
    ) {
    }
}
