package com.bridgeflow.api.auth.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.bridgeflow.api.auth.domain.AuthSession;

public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {

    @EntityGraph(attributePaths = "user")
    Optional<AuthSession> findByTokenHashAndExpiresAtAfter(String tokenHash, Instant now);

    void deleteByTokenHash(String tokenHash);
}
