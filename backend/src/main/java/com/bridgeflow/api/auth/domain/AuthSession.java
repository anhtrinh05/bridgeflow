package com.bridgeflow.api.auth.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "auth_sessions")
public class AuthSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuthSession() {
    }

    public AuthSession(AppUser user, String tokenHash, Instant expiresAt) {
        this.user = Objects.requireNonNull(user, "user is required");
        this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash is required");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt is required");
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public AppUser getUser() { return user; }
    public Instant getExpiresAt() { return expiresAt; }
}
