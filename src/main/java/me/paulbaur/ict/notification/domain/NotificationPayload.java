package me.paulbaur.ict.notification.domain;

import me.paulbaur.ict.common.model.ProbeStatus;

import java.time.Instant;

/**
 * Payload sent in state change notifications.
 */
public record NotificationPayload(
        String targetId,
        String targetHost,
        Instant timestamp,
        ProbeStatus previousStatus,
        ProbeStatus currentStatus,
        String errorMessage
) {
}
