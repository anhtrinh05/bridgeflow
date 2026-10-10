package com.bridgeflow.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Profiles;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BridgeFlowApiApplication {

    public static void main(String[] args) {
        var context = SpringApplication.run(BridgeFlowApiApplication.class, args);
        if (context.getEnvironment().getProperty("bridgeflow.bootstrap.enabled", Boolean.class, false)) {
            if (!context.getEnvironment().acceptsProfiles(Profiles.of("prod"))) {
                context.close();
                throw new IllegalStateException("Production user bootstrap requires the prod profile.");
            }
            context.close();
        }
    }
}
