package me.paulbaur.ict.analytics.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import me.paulbaur.ict.common.model.ProbeStatus;

import java.time.Instant;

/**
 * Represents a state change event (UP to DOWN or DOWN to UP).
 */
@Schema(description = "State change event representing a transition between UP and DOWN status")
public record StateChange(
        @Schema(description = "Target ID", example = "00000000-0000-0000-0000-000000000000")
        String targetId,

        @Schema(description = "Timestamp of the state change", example = "2025-12-29T12:34:56Z")
        Instant timestamp,

        @Schema(description = "Previous status", example = "UP")
        ProbeStatus fromStatus,

        @Schema(description = "New status", example = "DOWN")
        ProbeStatus toStatus,

        @Schema(description = "Error message if transitioning to DOWN", example = "connection refused")
        String errorMessage
) {
}
