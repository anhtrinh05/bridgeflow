package com.bridgeflow.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI bridgeFlowOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("BridgeFlow API")
                .version("v1")
                .description("Bilingual requirement management API for Japanese–Vietnamese delivery teams."))
            .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("opaque")))
            .addServersItem(new Server().url("/"));
    }
}
