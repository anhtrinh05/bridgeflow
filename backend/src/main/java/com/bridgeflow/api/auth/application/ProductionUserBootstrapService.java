package com.bridgeflow.api.auth.application;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bridgeflow.api.auth.domain.AppUser;
import com.bridgeflow.api.auth.persistence.AppUserRepository;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Service
public class ProductionUserBootstrapService {

    private static final int MINIMUM_PASSWORD_LENGTH = 16;
    private static final int MAXIMUM_PASSWORD_LENGTH = 128;

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Validator validator;

    public ProductionUserBootstrapService(
        AppUserRepository userRepository,
        PasswordEncoder passwordEncoder,
        Validator validator
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.validator = validator;
    }

    @Transactional
    public AppUser createInitialUser(String email, String displayName, String passwordFile) {
        if (userRepository.count() != 0) {
            throw new IllegalStateException(
                "Production user bootstrap is only allowed when the app_users table is empty."
            );
        }

        var input = new BootstrapInput(normalize(email), normalize(displayName), normalize(passwordFile));
        var violations = validator.validate(input);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException("Invalid production bootstrap configuration.", violations);
        }

        var password = readPassword(Path.of(input.passwordFile()));
        return userRepository.saveAndFlush(new AppUser(
            input.email(), input.displayName(), passwordEncoder.encode(password)
        ));
    }

    private String readPassword(Path path) {
        if (!path.isAbsolute() || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Bootstrap password file must be an existing absolute file.");
        }

        try {
            var raw = Files.readString(path, StandardCharsets.UTF_8);
            var password = raw.strip();
            if (password.contains("\n") || password.contains("\r")) {
                throw new IllegalArgumentException("Bootstrap password file must contain exactly one line.");
            }
            if (password.length() < MINIMUM_PASSWORD_LENGTH || password.length() > MAXIMUM_PASSWORD_LENGTH) {
                throw new IllegalArgumentException(
                    "Bootstrap password must contain between " + MINIMUM_PASSWORD_LENGTH
                        + " and " + MAXIMUM_PASSWORD_LENGTH + " characters."
                );
            }
            return password;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read the bootstrap password file.", exception);
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private record BootstrapInput(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 120) String displayName,
        @NotBlank String passwordFile
    ) {
    }
}
