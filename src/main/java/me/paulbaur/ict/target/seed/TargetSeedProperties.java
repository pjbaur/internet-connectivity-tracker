package me.paulbaur.ict.target.seed;

import java.util.List;
import java.util.Objects;

/**
 * Root configuration object representing the seed file contents.
 */
public record TargetSeedProperties(
        int schemaVersion,
        List<TargetDefinition> targets
) {
    public TargetSeedProperties {
        Objects.requireNonNull(targets, "targets must not be null");
        targets = List.copyOf(targets);
    }
}
