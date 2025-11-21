package me.paulbaur.ict.system.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import me.paulbaur.ict.common.model.ErrorResponse;
import me.paulbaur.ict.system.api.dto.HealthResponseDto;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// Simple non-Actuator health/version endpoint
@RestController
@Tag(name = "System", description = "Health and version endpoints for the service")
public class HealthController {

    @GetMapping(path = "/api/health", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Health check", description = "Simple health endpoint returning basic service status information.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Service is healthy",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = HealthResponseDto.class),
                            examples = @ExampleObject(
                                    name = "healthy",
                                    value = """
                                            {
                                              "status": "OK",
                                              "service": "internet-connectivity-tracker"
                                            }
                                            """
                            ))
            ),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "internalError",
                                    value = """
                                            {
                                              "message": "Unexpected failure",
                                              "code": "INTERNAL_ERROR",
                                              "timestamp": "2025-11-19T12:34:56Z"
                                            }
                                            """
                            ))
            )
    })
    public ResponseEntity<HealthResponseDto> health() {
        HealthResponseDto response = new HealthResponseDto("OK", "internet-connectivity-tracker");
        return ResponseEntity.ok(response);
    }
}
