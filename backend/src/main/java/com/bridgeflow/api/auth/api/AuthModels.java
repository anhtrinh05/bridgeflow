package com.bridgeflow.api.auth.api;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public final class AuthModels {
    private AuthModels() {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record UserResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String email,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String displayName
    ) {
    }

    public record LoginResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String accessToken,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant expiresAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UserResponse user
    ) {
    }
}
