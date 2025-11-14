package me.paulbaur.ict.probe.domain;

import me.paulbaur.ict.common.model.ProbeMethod;
import me.paulbaur.ict.common.model.ProbeStatus;

import java.time.Instant;

public record ProbeResult (
    Instant timestamp,
    String targetId,
    String targetHost,
    Long latencyMs,
    ProbeStatus status,
    ProbeMethod method,
    String errorMessage
) {

}
