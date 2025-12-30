package me.paulbaur.ict.notification.provider;

import me.paulbaur.ict.notification.domain.NotificationConfig;
import me.paulbaur.ict.notification.domain.NotificationPayload;

/**
 * Provider interface for sending notifications through various channels.
 */
public interface NotificationProvider {

    /**
     * Send a notification.
     *
     * @param config notification configuration
     * @param payload notification payload
     */
    void send(NotificationConfig config, NotificationPayload payload);
}
