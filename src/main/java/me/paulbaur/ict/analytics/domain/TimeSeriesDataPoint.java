package me.paulbaur.ict.analytics.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * A data point in a time series of probe metrics.
 */
@Schema(description = "Time-bucketed metric data point")
public record TimeSeriesDataPoint(
        @Schema(description = "Timestamp bucket", example = "2025-12-29T12:00:00Z")
        Instant timestamp,

        @Schema(description = "Average latency in this bucket (ms)", example = "42.5")
        Double averageLatencyMs,

        @Schema(description = "Number of probes in this bucket", example = "60")
        long probeCount,

        @Schema(description = "Number of successful probes in this bucket", example = "58")
        long successfulProbes,

        @Schema(description = "Uptime percentage in this bucket", example = "96.67")
        double uptimePercentage
) {
}
