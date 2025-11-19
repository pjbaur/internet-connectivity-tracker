package me.paulbaur.ict.probe.domain;

import me.paulbaur.ict.common.model.ProbeMethod;
import me.paulbaur.ict.common.model.ProbeStatus;

import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of a connectivity probe against a target")
public record ProbeResult (
    @Schema(description = "When the probe was executed (ISO-8601 timestamp)", example = "2025-11-19T12:34:56Z", implementation = String.class)
    Instant timestamp,

    @Schema(description = "UUID of the probed target", example = "00000000-0000-0000-0000-000000000000")
    String targetId,

    @Schema(description = "Host (name or IP) of the probed target", example = "example.org")
    String targetHost,

    @Schema(description = "Measured round-trip latency in milliseconds; null if probe failed", example = "23")
    Long latencyMs,

    @Schema(description = "Result status of the probe (UP or DOWN)", implementation = ProbeStatus.class)
    ProbeStatus status,

    @Schema(description = "Method used to perform the probe (e.g., TCP, ICMP)", implementation = ProbeMethod.class)
    ProbeMethod method,

    @Schema(description = "Optional error message when the probe failed", example = "connection timed out")
    String errorMessage
) {

}
