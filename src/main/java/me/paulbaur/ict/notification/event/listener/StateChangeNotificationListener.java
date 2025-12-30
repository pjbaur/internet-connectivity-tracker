package me.paulbaur.ict.notification.event.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.paulbaur.ict.common.model.ProbeStatus;
import me.paulbaur.ict.notification.domain.NotificationConfig;
import me.paulbaur.ict.notification.domain.NotificationPayload;
import me.paulbaur.ict.notification.service.NotificationService;
import me.paulbaur.ict.probe.domain.ProbeResult;
import me.paulbaur.ict.probe.event.ProbeResultEvent;
import me.paulbaur.ict.probe.service.ProbeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Listens for probe result events and sends notifications on state changes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StateChangeNotificationListener {

    private final NotificationService notificationService;
    private final ProbeRepository probeRepository;

    @Value("${ict.notifications.enabled:false}")
    private boolean notificationsEnabled;

    @Value("${ict.notifications.providers.webhook.default-url:}")
    private String defaultWebhookUrl;

    /**
     * Handle probe result events and send notifications on state changes.
     * Runs asynchronously to avoid blocking the event publisher.
     */
    @Async("probeTaskExecutor")
    @EventListener
    public void handleProbeResultEvent(ProbeResultEvent event) {
        if (!notificationsEnabled) {
            return;
        }

        if (!event.isStateChange()) {
            log.debug("Skipping notification - not a state change",
                    kv("targetId", event.getResult().targetId()));
            return;
        }

        ProbeResult currentResult = event.getResult();

        // Get previous status
        ProbeStatus previousStatus = getPreviousStatus(currentResult.targetId());
        ProbeStatus currentStatus = currentResult.status();

        log.info("Processing state change notification",
                kv("targetId", currentResult.targetId()),
                kv("previousStatus", previousStatus),
                kv("currentStatus", currentStatus));

        // Create notification payload
        NotificationPayload payload = new NotificationPayload(
                currentResult.targetId(),
                currentResult.targetHost(),
                currentResult.timestamp(),
                previousStatus,
                currentStatus,
                currentResult.errorMessage()
        );

        // Get or create notification config for this target
        // For now, use a default configuration
        // In a full implementation, this would be fetched from a repository
        NotificationConfig config = createDefaultConfig(currentResult.targetId());

        // Send notification
        notificationService.sendNotification(config, payload);
    }

    /**
     * Get the previous status for a target by fetching the second-most-recent result.
     * Returns UP if no previous result is found (assume target was previously up).
     */
    private ProbeStatus getPreviousStatus(String targetId) {
        try {
            // Fetch the 2 most recent results (current one should be saved by now)
            // Skip the first (current) and take the second (previous)
            Optional<ProbeResult> previousResult = probeRepository
                    .findRecent(targetId, 2)
                    .stream()
                    .skip(1)
                    .findFirst();

            return previousResult.map(ProbeResult::status).orElse(ProbeStatus.UP);
        } catch (Exception e) {
            log.warn("Failed to get previous status for target {}",
                    targetId,
                    e);
            return ProbeStatus.UP;
        }
    }

    /**
     * Create a default notification configuration.
     * In a real implementation, this would be fetched from a database or config file.
     */
    private NotificationConfig createDefaultConfig(String targetId) {
        NotificationConfig config = NotificationConfig.createDefault(targetId);

        // Enable notifications if a default webhook URL is configured
        if (defaultWebhookUrl != null && !defaultWebhookUrl.isBlank()) {
            config.setEnabled(true);
            config.setWebhookUrl(defaultWebhookUrl);
        }

        return config;
    }
}
