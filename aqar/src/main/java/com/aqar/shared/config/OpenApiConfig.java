package com.aqar.shared.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;

@OpenAPIDefinition(
        info = @Info(title = "Aqar API", version = "0.1.0", description = "Real estate listing and analytics platform for the MENA market"),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local development"),
                @Server(url = "https://curly-broccoli-v7w49j564j4f767-8080.app.github.dev", description = "Codespaces")
        }
)
@SecurityScheme(
        name = "BearerJwt",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}
