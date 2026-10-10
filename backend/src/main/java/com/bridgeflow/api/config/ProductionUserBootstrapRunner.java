package com.bridgeflow.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.bridgeflow.api.auth.application.ProductionUserBootstrapService;

@Component
@Profile("prod")
@ConditionalOnProperty(name = "bridgeflow.bootstrap.enabled", havingValue = "true")
public class ProductionUserBootstrapRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductionUserBootstrapRunner.class);

    private final ProductionUserBootstrapService bootstrapService;
    private final String email;
    private final String displayName;
    private final String passwordFile;

    public ProductionUserBootstrapRunner(
        ProductionUserBootstrapService bootstrapService,
        @Value("${bridgeflow.bootstrap.email:}") String email,
        @Value("${bridgeflow.bootstrap.display-name:}") String displayName,
        @Value("${bridgeflow.bootstrap.password-file:}") String passwordFile
    ) {
        this.bootstrapService = bootstrapService;
        this.email = email;
        this.displayName = displayName;
        this.passwordFile = passwordFile;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        bootstrapService.createInitialUser(email, displayName, passwordFile);
        LOGGER.info("Initial production user created successfully.");
    }
}
