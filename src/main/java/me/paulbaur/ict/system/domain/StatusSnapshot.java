package me.paulbaur.ict.system.domain;

import java.time.Instant;

public record StatusSnapshot(
        Instant timestamp,
        boolean anyDown,
        int totalTargets,
        int targetsDown
) {
}
