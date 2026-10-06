package com.bridgeflow.api.project.api;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ProjectModels {

    private ProjectModels() {
    }

    public record CreateProjectRequest(
        @NotBlank @Size(max = 40) String code,
        @NotBlank @Size(max = 160) String name,
        @Size(max = 160) String customerName
    ) {
    }

    public record UpdateProjectRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 160) String customerName
    ) {
    }

    public record ProjectResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String customerName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String role,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long requirementCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant updatedAt
    ) {
    }
}
