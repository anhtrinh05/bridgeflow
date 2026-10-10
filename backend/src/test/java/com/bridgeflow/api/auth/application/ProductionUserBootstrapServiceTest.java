package com.bridgeflow.api.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.bridgeflow.api.auth.domain.AppUser;
import com.bridgeflow.api.auth.persistence.AppUserRepository;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class ProductionUserBootstrapServiceTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @TempDir
    private Path temporaryDirectory;

    @Test
    void createsTheOnlyInitialUserFromAPasswordFile() throws Exception {
        var passwordFile = temporaryDirectory.resolve("bootstrap-password.txt");
        Files.writeString(passwordFile, "correct-horse-battery-staple");
        var savedUser = new AtomicReference<AppUser>();
        var service = service(0, savedUser);

        service.createInitialUser(" Operator@Example.com ", " Initial Operator ", passwordFile.toString());

        assertThat(savedUser.get().getEmail()).isEqualTo("operator@example.com");
        assertThat(savedUser.get().getDisplayName()).isEqualTo("Initial Operator");
        assertThat(savedUser.get().getPasswordHash()).isEqualTo("encoded:correct-horse-battery-staple");
    }

    @Test
    void refusesToOverwriteOrAddUsersAfterBootstrap() {
        var savedUser = new AtomicReference<AppUser>();
        var service = service(1, savedUser);

        assertThatThrownBy(() -> service.createInitialUser(
            "operator@example.com", "Initial Operator", temporaryDirectory.resolve("missing").toString()
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("only allowed when the app_users table is empty");

        assertThat(savedUser).hasNullValue();
    }

    @Test
    void rejectsWeakPasswordsWithoutPersistingAUser() throws Exception {
        var passwordFile = temporaryDirectory.resolve("weak-password.txt");
        Files.writeString(passwordFile, "too-short");
        var savedUser = new AtomicReference<AppUser>();
        var service = service(0, savedUser);

        assertThatThrownBy(() -> service.createInitialUser(
            "operator@example.com", "Initial Operator", passwordFile.toString()
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("between 16 and 128 characters");

        assertThat(savedUser).hasNullValue();
    }

    private ProductionUserBootstrapService service(long userCount, AtomicReference<AppUser> savedUser) {
        var repository = (AppUserRepository) Proxy.newProxyInstance(
            AppUserRepository.class.getClassLoader(),
            new Class<?>[] { AppUserRepository.class },
            (proxy, method, arguments) -> switch (method.getName()) {
                case "count" -> userCount;
                case "saveAndFlush" -> {
                    var user = (AppUser) arguments[0];
                    savedUser.set(user);
                    yield user;
                }
                case "toString" -> "ProductionUserBootstrapServiceTestRepository";
                default -> throw new UnsupportedOperationException(method.getName());
            }
        );
        var passwordEncoder = new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return "encoded:" + rawPassword;
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return encodedPassword.equals(encode(rawPassword));
            }
        };
        return new ProductionUserBootstrapService(repository, passwordEncoder, VALIDATOR);
    }
}
