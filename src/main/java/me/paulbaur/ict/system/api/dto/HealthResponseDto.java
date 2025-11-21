package me.paulbaur.ict.system.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Simple health response for readiness/liveness checks")
public record HealthResponseDto(
        @Schema(description = "Overall health status", example = "OK")
        String status,

        @Schema(description = "Service identifier", example = "internet-connectivity-tracker")
        String service
) {
}
