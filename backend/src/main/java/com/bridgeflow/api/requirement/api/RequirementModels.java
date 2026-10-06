package com.bridgeflow.api.requirement.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.bridgeflow.api.requirement.domain.ChangeType;

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

    public record ConfirmRevisionRequest(@NotNull UUID reviewerId) {
    }

    public record RevisionResponse(
        UUID id,
        int revisionNumber,
        String japaneseText,
        String vietnameseText,
        String changeType,
        String reviewStatus,
        Instant createdAt,
        UUID confirmedBy,
        Instant confirmedAt
    ) {
    }

    public record RequirementResponse(
        UUID id,
        UUID projectId,
        String displayKey,
        String status,
        UUID currentRevisionId,
        RevisionResponse latestRevision,
        List<RevisionResponse> revisions,
        Instant createdAt,
        Instant updatedAt,
        Instant archivedAt
    ) {
    }

    public record RequirementPageResponse(
        List<RequirementResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
    ) {
    }
}
