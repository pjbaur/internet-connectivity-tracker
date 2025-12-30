package me.paulbaur.ict.notification.service;

import me.paulbaur.ict.notification.domain.NotificationConfig;
import me.paulbaur.ict.notification.domain.NotificationPayload;

/**
 * Service for sending notifications on state changes.
 */
public interface NotificationService {

    /**
     * Send a state change notification.
     *
     * @param config notification configuration
     * @param payload notification payload
     */
    void sendNotification(NotificationConfig config, NotificationPayload payload);

    /**
     * Check if notification should be sent based on config and state change.
     *
     * @param config notification configuration
     * @param payload notification payload
     * @return true if notification should be sent
     */
    boolean shouldNotify(NotificationConfig config, NotificationPayload payload);
}
