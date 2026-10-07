package com.bridgeflow.api.glossary.api;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class GlossaryModels {

    private GlossaryModels() {
    }

    public record SaveGlossaryTermRequest(
        @NotBlank @Size(max = 160) String japaneseTerm,
        @NotBlank @Size(max = 240) String vietnameseTerm,
        @Size(max = 2000) String notes
    ) {
    }

    public record GlossaryTermResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID projectId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String japaneseTerm,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String vietnameseTerm,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String notes,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID createdBy,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant updatedAt
    ) {
    }
}
