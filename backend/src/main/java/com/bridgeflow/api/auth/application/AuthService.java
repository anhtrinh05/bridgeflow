package com.bridgeflow.api.auth.application;

import static com.bridgeflow.api.auth.api.AuthModels.LoginResponse;
import static com.bridgeflow.api.auth.api.AuthModels.UserResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bridgeflow.api.auth.api.AuthModels.LoginRequest;
import com.bridgeflow.api.auth.domain.AuthSession;
import com.bridgeflow.api.auth.persistence.AppUserRepository;
import com.bridgeflow.api.auth.persistence.AuthSessionRepository;
import com.bridgeflow.api.common.InvalidCredentialsException;

@Service
public class AuthService {

    private static final Duration SESSION_DURATION = Duration.ofHours(8);
    private final SecureRandom secureRandom = new SecureRandom();
    private final AppUserRepository userRepository;
    private final AuthSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
        AppUserRepository userRepository,
        AuthSessionRepository sessionRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        var user = userRepository.findByEmailIgnoreCase(request.email().trim().toLowerCase(Locale.ROOT))
            .filter(candidate -> candidate.isActive() && passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
            .orElseThrow(InvalidCredentialsException::new);
        var rawToken = newToken();
        var expiresAt = Instant.now().plus(SESSION_DURATION);
        sessionRepository.save(new AuthSession(user, hashToken(rawToken), expiresAt));
        return new LoginResponse(rawToken, expiresAt, toResponse(user.getId(), user.getEmail(), user.getDisplayName()));
    }

    @Transactional(readOnly = true)
    public Optional<CurrentUser> authenticate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return Optional.empty();
        return sessionRepository.findByTokenHashAndExpiresAtAfter(hashToken(rawToken), Instant.now())
            .map(AuthSession::getUser)
            .filter(user -> user.isActive())
            .map(user -> new CurrentUser(user.getId(), user.getEmail(), user.getDisplayName()));
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken != null && !rawToken.isBlank()) sessionRepository.deleteByTokenHash(hashToken(rawToken));
    }

    public UserResponse toResponse(CurrentUser user) {
        return toResponse(user.id(), user.email(), user.displayName());
    }

    private UserResponse toResponse(java.util.UUID id, String email, String displayName) {
        return new UserResponse(id, email, displayName);
    }

    private String newToken() {
        var bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hashToken(String rawToken) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
