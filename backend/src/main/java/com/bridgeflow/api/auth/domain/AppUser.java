package com.bridgeflow.api.auth.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AppUser() {
    }

    public AppUser(String email, String displayName, String passwordHash) {
        this.email = normalizeEmail(email);
        this.displayName = requireText(displayName, "displayName");
        this.passwordHash = requireText(passwordHash, "passwordHash");
    }

    @PrePersist
    void onCreate() {
        var now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public String getPasswordHash() { return passwordHash; }
    public UserStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    private static String normalizeEmail(String value) {
        return requireText(value, "email").toLowerCase(Locale.ROOT);
    }

    private static String requireText(String value, String fieldName) {
        var normalized = Objects.requireNonNull(value, fieldName + " is required").trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(fieldName + " must not be blank");
        return normalized;
    }
}
