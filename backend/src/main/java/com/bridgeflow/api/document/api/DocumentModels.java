package com.bridgeflow.api.document.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public final class DocumentModels {

    private DocumentModels() {
    }

    public record DocumentVersionResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int versionNumber,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String originalFilename,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String contentType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long sizeBytes,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String sha256,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID uploadedBy,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt
    ) {
    }

    public record DocumentResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID projectId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) DocumentVersionResponse latestVersion,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<DocumentVersionResponse> versions,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant updatedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Instant archivedAt
    ) {
    }

    public record DocumentContent(
        String filename,
        String contentType,
        byte[] bytes
    ) {
    }
}
