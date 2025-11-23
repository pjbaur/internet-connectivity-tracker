package me.paulbaur.ict.target.seed;

import java.util.Objects;

/**
 * Declarative description of a target sourced from a seed file.
 */
public record TargetDefinition(
        String label,
        String host,
        Integer port,
        String method,
        Integer intervalSeconds
) {
    public TargetDefinition {
        host = Objects.requireNonNull(host, "host is required");
        port = Objects.requireNonNull(port, "port is required");
    }
}
