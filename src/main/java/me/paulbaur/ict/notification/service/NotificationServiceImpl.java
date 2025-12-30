package me.paulbaur.ict.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.paulbaur.ict.common.model.ProbeStatus;
import me.paulbaur.ict.notification.domain.NotificationConfig;
import me.paulbaur.ict.notification.domain.NotificationPayload;
import me.paulbaur.ict.notification.provider.NotificationProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Implementation of NotificationService that delegates to a NotificationProvider.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationProvider notificationProvider;

    @Value("${ict.notifications.enabled:false}")
    private boolean notificationsEnabled;

    @Override
    public void sendNotification(NotificationConfig config, NotificationPayload payload) {
        if (!notificationsEnabled) {
            log.debug("Notifications globally disabled, skipping notification");
            return;
        }

        if (!config.isEnabled()) {
            log.debug("Notifications disabled for target {}", config.getTargetId());
            return;
        }

        if (!shouldNotify(config, payload)) {
            log.debug("Notification filtered out based on config",
                    kv("targetId", config.getTargetId()),
                    kv("previousStatus", payload.previousStatus()),
                    kv("currentStatus", payload.currentStatus()));
            return;
        }

        try {
            notificationProvider.send(config, payload);
            log.info("Notification sent successfully",
                    kv("targetId", payload.targetId()),
                    kv("transition", payload.previousStatus() + " -> " + payload.currentStatus()));
        } catch (Exception e) {
            log.error("Failed to send notification",
                    kv("targetId", payload.targetId()),
                    kv("error", e.getMessage()),
                    e);
        }
    }

    @Override
    public boolean shouldNotify(NotificationConfig config, NotificationPayload payload) {
        ProbeStatus previous = payload.previousStatus();
        ProbeStatus current = payload.currentStatus();

        // No notification if status didn't change
        if (previous == current) {
            return false;
        }

        // Check UP -> DOWN transition
        if (previous == ProbeStatus.UP && current == ProbeStatus.DOWN) {
            return config.isNotifyOnUpToDown();
        }

        // Check DOWN -> UP transition
        if (previous == ProbeStatus.DOWN && current == ProbeStatus.UP) {
            return config.isNotifyOnDownToUp();
        }

        return false;
    }
}
