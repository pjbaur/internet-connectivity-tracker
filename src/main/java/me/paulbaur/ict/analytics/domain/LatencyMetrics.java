package me.paulbaur.ict.analytics.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Latency metrics for a target over a time period.
 */
@Schema(description = "Latency statistics for a target over a specific time range")
public record LatencyMetrics(
        @Schema(description = "Target ID", example = "00000000-0000-0000-0000-000000000000")
        String targetId,

        @Schema(description = "Start of the time range", example = "2025-12-01T00:00:00Z")
        Instant startTime,

        @Schema(description = "End of the time range", example = "2025-12-31T23:59:59Z")
        Instant endTime,

        @Schema(description = "Average latency in milliseconds", example = "42.5")
        Double averageLatencyMs,

        @Schema(description = "Minimum latency in milliseconds", example = "10.0")
        Double minLatencyMs,

        @Schema(description = "Maximum latency in milliseconds", example = "150.0")
        Double maxLatencyMs,

        @Schema(description = "Number of probes with latency data", example = "950")
        long probeCount
) {
}
