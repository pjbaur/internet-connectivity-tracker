package me.paulbaur.ict.probe.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request object to initiate or describe a probe against a target")
public record ProbeRequest(
        @Schema(description = "UUID of the target to probe", example = "00000000-0000-0000-0000-000000000000")
        String targetId,

        @Schema(description = "Hostname or IP address of the target", example = "example.org")
        String host,

        @Schema(description = "Port number for the probe (e.g., 80 for HTTP)", example = "80")
        int port
) {
}
