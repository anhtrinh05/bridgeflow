package com.bridgeflow.api.auth.application;

import java.util.UUID;

public record CurrentUser(UUID id, String email, String displayName) {
}
