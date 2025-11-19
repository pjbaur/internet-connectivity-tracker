package me.paulbaur.ict.system.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import org.springframework.http.MediaType;

import me.paulbaur.ict.common.model.ErrorResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;

// Simple non-Actuator health/version endpoint
@RestController
@Tag(name = "System", description = "Health and version endpoints for the service")
public class HealthController {

    @GetMapping("/api/health")
    @Operation(summary = "Health check", description = "Simple health endpoint returning basic service status information.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Service is healthy",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(type = "object"), examples = {@ExampleObject(value = "{\"status\":\"OK\",\"service\":\"internet-connectivity-tracker\"}")})
            ),
            @ApiResponse(responseCode = "500", description = "Internal server error. TODO: Align controller to return structured ErrorResponse",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public Map<String, Object> health() {
        return Map.of(
                "status", "OK",
                "service", "internet-connectivity-tracker"
        );
    }
}
