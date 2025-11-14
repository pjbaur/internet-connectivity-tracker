package me.paulbaur.ict.probe.domain;

public record ProbeRequest(
        String targetId,
        String host,
        int port
) {
}
