package me.paulbaur.ict.notification.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration for notifications on a per-target basis.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationConfig {

    /**
     * Target ID this configuration applies to.
     */
    private String targetId;

    /**
     * Whether notifications are enabled for this target.
     */
    private boolean enabled;

    /**
     * Webhook URL to send notifications to.
     */
    private String webhookUrl;

    /**
     * Whether to notify on UP to DOWN transitions.
     */
    private boolean notifyOnUpToDown;

    /**
     * Whether to notify on DOWN to UP transitions.
     */
    private boolean notifyOnDownToUp;

    /**
     * Timeout in milliseconds for webhook HTTP requests.
     */
    private int timeoutMs;

    /**
     * Create a default configuration.
     */
    public static NotificationConfig createDefault(String targetId) {
        return new NotificationConfig(
                targetId,
                false,  // disabled by default
                null,
                true,   // notify on UP to DOWN
                true,   // notify on DOWN to UP
                5000    // 5 second timeout
        );
    }
}
