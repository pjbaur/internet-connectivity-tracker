package me.paulbaur.ict.target.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import me.paulbaur.ict.target.domain.Target;

import java.util.Objects;
import java.util.UUID;

@Schema(description = "Target returned by the API")
public record TargetResponseDto(
        @Schema(description = "Unique identifier of the target (UUID)", example = "00000000-0000-0000-0000-000000000000")
        UUID id,

        @Schema(description = "Human-readable label for the target", example = "example.org")
        String label,

        @Schema(description = "Hostname or IP address of the target", example = "93.184.216.34")
        String host,

        @Schema(description = "Port number for the target service", example = "80")
        int port,

        @Schema(description = "Whether the target is currently enabled", example = "true")
        boolean enabled
) {

    public static TargetResponseDto fromDomain(Target target) {
        Objects.requireNonNull(target, "target must not be null");
        return new TargetResponseDto(
                target.getId(),
                target.getLabel(),
                target.getHost(),
                target.getPort(),
                target.isEnabled()
        );
    }
}
