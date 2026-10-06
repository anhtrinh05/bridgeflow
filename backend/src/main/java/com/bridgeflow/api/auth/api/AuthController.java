package com.bridgeflow.api.auth.api;

import static com.bridgeflow.api.auth.api.AuthModels.LoginRequest;
import static com.bridgeflow.api.auth.api.AuthModels.LoginResponse;
import static com.bridgeflow.api.auth.api.AuthModels.UserResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bridgeflow.api.auth.application.AuthService;
import com.bridgeflow.api.auth.application.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Login and current user session")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(operationId = "login", summary = "Login with email and password")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    @Operation(operationId = "getCurrentUser", summary = "Get the authenticated user")
    @SecurityRequirement(name = "bearerAuth")
    public UserResponse me(@AuthenticationPrincipal CurrentUser user) {
        return authService.toResponse(user);
    }

    @PostMapping("/logout")
    @Operation(operationId = "logout", summary = "Revoke the current access token")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        authService.logout(bearerToken(authorization));
        return ResponseEntity.noContent().build();
    }

    private String bearerToken(String authorization) {
        return authorization != null && authorization.startsWith("Bearer ")
            ? authorization.substring(7)
            : "";
    }
}
