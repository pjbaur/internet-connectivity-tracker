package me.paulbaur.ict.system.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import me.paulbaur.ict.system.domain.StatusSnapshot;

import java.time.Instant;
import java.util.Objects;

@Schema(description = "DTO returned by /api/status summarizing overall target health")
public record StatusSnapshotDto(
        @Schema(description = "When the snapshot was calculated (ISO-8601)", example = "2025-11-19T12:00:00Z", implementation = String.class)
        Instant timestamp,
        @Schema(description = "True if any monitored target is currently down", example = "false")
        boolean anyDown,
        @Schema(description = "Total number of enabled targets being monitored", example = "5")
        int totalTargets,
        @Schema(description = "Number of targets whose latest probe is DOWN", example = "1")
        int targetsDown,
        @Schema(description = "Number of targets that have never produced a probe result", example = "2")
        int unknownTargets
) {

    public static StatusSnapshotDto fromDomain(StatusSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        return new StatusSnapshotDto(
                snapshot.timestamp(),
                snapshot.anyDown(),
                snapshot.totalTargets(),
                snapshot.targetsDown(),
                snapshot.unknownTargets()
        );
    }
}
