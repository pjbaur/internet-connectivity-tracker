package me.paulbaur.ict.system.domain;

import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Snapshot of the overall system status, used for health and status endpoints")
public record StatusSnapshot(
        @Schema(description = "When the snapshot was taken (ISO-8601)", example = "2025-11-19T12:34:56Z", implementation = String.class)
        Instant timestamp,

        @Schema(description = "True if any monitored target is currently down", example = "false")
        boolean anyDown,

        @Schema(description = "Total number of targets monitored", example = "12")
        int totalTargets,

        @Schema(description = "Number of targets currently down", example = "0")
        int targetsDown
) {
}
