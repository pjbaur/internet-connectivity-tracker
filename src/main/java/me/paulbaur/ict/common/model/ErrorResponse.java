package me.paulbaur.ict.common.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Standard error response returned by the API when requests fail")
public record ErrorResponse(
        @Schema(description = "Human-readable error message", example = "Invalid request: missing targetId")
        String message,

        @Schema(description = "Application-specific error code", example = "VALIDATION_ERROR")
        String code,

        @Schema(description = "Time the error occurred (ISO-8601)", example = "2025-11-19T12:34:56Z", implementation = String.class)
        Instant timestamp
) {
}

