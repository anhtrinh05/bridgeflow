package com.bridgeflow.api.analysis.api;

import static com.bridgeflow.api.ai.api.AiModels.AiJobResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.bridgeflow.api.analysis.domain.ArtifactReviewStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class AnalysisModels {

    private AnalysisModels() {
    }

    public record RequirementAnalysisResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) AiJobResponse job,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<ClarificationQuestionResponse> questions,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<AcceptanceCriterionResponse> acceptanceCriteria
    ) {
    }

    public record ClarificationQuestionResponse(
        UUID id,
        UUID requirementRevisionId,
        UUID aiJobId,
        String japaneseText,
        String vietnameseText,
        @Schema(nullable = true) String rationale,
        @Schema(nullable = true) String answerJapanese,
        @Schema(nullable = true) String answerVietnamese,
        ArtifactReviewStatus status,
        UUID createdBy,
        Instant createdAt,
        @Schema(nullable = true) UUID reviewedBy,
        @Schema(nullable = true) Instant reviewedAt,
        @Schema(nullable = true) UUID answeredBy,
        @Schema(nullable = true) Instant answeredAt
    ) {
    }

    public record AcceptanceCriterionResponse(
        UUID id,
        UUID requirementRevisionId,
        UUID aiJobId,
        String japaneseText,
        String vietnameseText,
        ArtifactReviewStatus status,
        UUID createdBy,
        Instant createdAt,
        @Schema(nullable = true) UUID reviewedBy,
        @Schema(nullable = true) Instant reviewedAt
    ) {
    }

    public record AnswerQuestionRequest(
        @NotBlank String japaneseText,
        @NotBlank String vietnameseText
    ) {
    }

    public record ReviewArtifactRequest(@NotNull ArtifactReviewStatus decision) {
    }
}
