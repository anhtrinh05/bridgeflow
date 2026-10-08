package com.bridgeflow.api.testcase.api;

import static com.bridgeflow.api.ai.api.AiModels.AiJobResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.bridgeflow.api.analysis.domain.ArtifactReviewStatus;
import com.bridgeflow.api.testcase.domain.TestCasePriority;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public final class TestCaseModels {
    private TestCaseModels() { }

    public record TestCaseWorkspaceResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) AiJobResponse job,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<TestCaseResponse> testCases
    ) { }

    public record TestCaseResponse(
        UUID id, UUID requirementRevisionId, UUID acceptanceCriterionId, UUID aiJobId,
        String titleJapanese, String titleVietnamese,
        String preconditionsJapanese, String preconditionsVietnamese,
        String stepsJapanese, String stepsVietnamese,
        String expectedResultJapanese, String expectedResultVietnamese,
        TestCasePriority priority, ArtifactReviewStatus status,
        UUID createdBy, Instant createdAt,
        @Schema(nullable = true) UUID reviewedBy,
        @Schema(nullable = true) Instant reviewedAt
    ) { }

    public record ReviewTestCaseRequest(@NotNull ArtifactReviewStatus decision) { }
}
