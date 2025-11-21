package me.paulbaur.ict.target.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Payload for creating a new probe target")
public record TargetRequestDto(
        @Schema(description = "Human-readable label for the target", example = "Google DNS")
        String label,

        @Schema(description = "Hostname or IP address of the target", example = "8.8.8.8")
        String host,

        @Schema(description = "TCP port to probe on the target", example = "53")
        Integer port
) {
}
