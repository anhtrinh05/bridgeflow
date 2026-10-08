package com.bridgeflow.api.requirement.api;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public final class TraceabilityModels {
    private TraceabilityModels() { }

    public record RequirementTraceabilityResponse(
        UUID requirementId,
        String displayKey,
        @Schema(nullable = true) UUID currentRevisionId,
        List<RevisionTraceResponse> revisions
    ) { }

    public record RevisionTraceResponse(
        UUID revisionId,
        int revisionNumber,
        String changeType,
        String reviewStatus,
        @Schema(nullable = true) UUID documentVersionId,
        @Schema(nullable = true) String sourceAnchor,
        List<ArtifactTraceResponse> artifacts
    ) { }

    public record ArtifactTraceResponse(
        UUID id,
        String type,
        String status,
        @Schema(nullable = true) UUID sourceArtifactId
    ) { }

    public record ChangeImpactResponse(
        UUID requirementId,
        UUID targetRevisionId,
        @Schema(nullable = true) UUID baselineRevisionId,
        String impactLevel,
        boolean japaneseChanged,
        boolean vietnameseChanged,
        boolean requiresArtifactRegeneration,
        List<String> reasons,
        List<AffectedArtifactResponse> affectedArtifacts
    ) { }

    public record AffectedArtifactResponse(
        UUID id,
        String type,
        String status,
        UUID sourceRevisionId,
        String recommendedAction
    ) { }
}
