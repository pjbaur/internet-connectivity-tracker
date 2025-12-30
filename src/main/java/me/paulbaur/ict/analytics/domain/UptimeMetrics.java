package me.paulbaur.ict.analytics.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Uptime metrics for a target over a time period.
 */
@Schema(description = "Uptime statistics for a target over a specific time range")
public record UptimeMetrics(
        @Schema(description = "Target ID", example = "00000000-0000-0000-0000-000000000000")
        String targetId,

        @Schema(description = "Start of the time range", example = "2025-12-01T00:00:00Z")
        Instant startTime,

        @Schema(description = "End of the time range", example = "2025-12-31T23:59:59Z")
        Instant endTime,

        @Schema(description = "Total number of probes", example = "1000")
        long totalProbes,

        @Schema(description = "Number of successful (UP) probes", example = "950")
        long successfulProbes,

        @Schema(description = "Number of failed (DOWN) probes", example = "50")
        long failedProbes,

        @Schema(description = "Uptime percentage", example = "95.0")
        double uptimePercentage
) {
    /**
     * Calculate uptime metrics from probe counts.
     */
    public static UptimeMetrics calculate(String targetId, Instant startTime, Instant endTime,
                                          long totalProbes, long successfulProbes, long failedProbes) {
        double uptimePercentage = totalProbes > 0
                ? (successfulProbes * 100.0) / totalProbes
                : 0.0;

        return new UptimeMetrics(
                targetId,
                startTime,
                endTime,
                totalProbes,
                successfulProbes,
                failedProbes,
                uptimePercentage
        );
    }
}
