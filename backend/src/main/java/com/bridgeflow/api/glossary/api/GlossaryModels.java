package com.bridgeflow.api.glossary.api;

import java.time.Instant;
import java.util.UUID;

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
        UUID id,
        UUID projectId,
        String japaneseTerm,
        String vietnameseTerm,
        String notes,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt
    ) {
    }
}
