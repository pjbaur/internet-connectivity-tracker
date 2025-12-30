package me.paulbaur.ict.notification.provider;

import lombok.extern.slf4j.Slf4j;
import me.paulbaur.ict.notification.domain.NotificationConfig;
import me.paulbaur.ict.notification.domain.NotificationPayload;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Notification provider that sends webhooks via HTTP POST.
 */
@Component
@Slf4j
public class WebhookNotificationProvider implements NotificationProvider {

    private final WebClient webClient;

    @Value("${ict.notifications.providers.webhook.timeout-ms:5000}")
    private int defaultTimeoutMs;

    public WebhookNotificationProvider(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Override
    public void send(NotificationConfig config, NotificationPayload payload) {
        if (config.getWebhookUrl() == null || config.getWebhookUrl().isBlank()) {
            log.warn("Webhook URL not configured for target {}", config.getTargetId());
            return;
        }

        int timeout = config.getTimeoutMs() > 0 ? config.getTimeoutMs() : defaultTimeoutMs;

        log.debug("Sending webhook notification",
                kv("targetId", payload.targetId()),
                kv("url", config.getWebhookUrl()),
                kv("timeout", timeout));

        try {
            webClient.post()
                    .uri(config.getWebhookUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .onStatus(
                            status -> status.isError(),
                            response -> {
                                log.warn("Webhook returned error status",
                                        kv("status", response.statusCode()),
                                        kv("url", config.getWebhookUrl()));
                                return Mono.error(new WebhookException("Webhook returned error: " + response.statusCode()));
                            }
                    )
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            log.info("Webhook notification sent successfully",
                    kv("targetId", payload.targetId()),
                    kv("url", config.getWebhookUrl()));

        } catch (Exception e) {
            log.error("Failed to send webhook notification",
                    kv("targetId", payload.targetId()),
                    kv("url", config.getWebhookUrl()),
                    kv("error", e.getMessage()),
                    e);
            throw new WebhookException("Failed to send webhook notification", e);
        }
    }

    /**
     * Exception thrown when webhook sending fails.
     */
    public static class WebhookException extends RuntimeException {
        public WebhookException(String message) {
            super(message);
        }

        public WebhookException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
