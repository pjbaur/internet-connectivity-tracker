package me.paulbaur.ict.system.config;

import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;

@OpenAPIDefinition(
        info = @Info(
                title = "Internet Connectivity Tracker API",
                version = "0.1.0-SNAPSHOT",
                description = "Tracks TCP probe results and stores them in Elasticsearch. Provides endpoints for probes, targets, history, health and metrics."
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local development")
        }
)
@Configuration
public class OpenApiConfig {
    // Global OpenAPI definition only - no runtime beans required for basic springdoc usage.
}

