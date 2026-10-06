package com.bridgeflow.api.project.api;

import java.time.Instant;
import java.util.UUID;

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
        UUID id,
        String code,
        String name,
        String customerName,
        String status,
        long requirementCount,
        Instant createdAt,
        Instant updatedAt
    ) {
    }
}
