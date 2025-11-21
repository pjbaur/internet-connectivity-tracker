package me.paulbaur.ict.probe.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import me.paulbaur.ict.common.model.ProbeMethod;
import me.paulbaur.ict.common.model.ProbeStatus;
import me.paulbaur.ict.probe.domain.ProbeResult;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Schema(description = "Probe result returned by the API")
public record ProbeResultDto(
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

    public static ProbeResultDto fromDomain(ProbeResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return new ProbeResultDto(
                result.timestamp(),
                result.targetId(),
                result.targetHost(),
                result.latencyMs(),
                result.status(),
                result.method(),
                result.errorMessage()
        );
    }

    public static List<ProbeResultDto> fromDomainList(List<ProbeResult> results) {
        if (results == null || results.isEmpty()) {
            return List.of();
        }
        return results.stream()
                .map(ProbeResultDto::fromDomain)
                .toList();
    }
}
