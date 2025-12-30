package me.paulbaur.ict.probe.event.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.paulbaur.ict.probe.domain.ProbeResult;
import me.paulbaur.ict.probe.event.ProbeResultEvent;
import me.paulbaur.ict.probe.service.ProbeRepository;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Event listener that asynchronously saves probe results to Elasticsearch.
 * This decouples probe execution from storage, allowing probes to complete quickly
 * while storage happens in the background.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchEventListener {

    private final ProbeRepository probeRepository;

    /**
     * Handle probe result events by saving them to Elasticsearch.
     * This method executes asynchronously to avoid blocking the probe execution.
     *
     * @param event the probe result event
     */
    @EventListener
    @Async("probeTaskExecutor")
    public void handleProbeResultEvent(ProbeResultEvent event) {
        ProbeResult result = event.getResult();

        try {
            log.debug(
                    "Saving probe result to Elasticsearch",
                    kv("targetId", result.targetId()),
                    kv("status", result.status()),
                    kv("isStateChange", event.isStateChange())
            );

            probeRepository.save(result);

            log.debug(
                    "Successfully saved probe result to Elasticsearch",
                    kv("targetId", result.targetId()),
                    kv("status", result.status())
            );
        } catch (Exception e) {
            log.error(
                    "Failed to save probe result to Elasticsearch",
                    kv("targetId", result.targetId()),
                    kv("status", result.status()),
                    kv("error", e.getMessage()),
                    e
            );
            // Don't rethrow - we don't want to break the event processing chain
        }
    }
}
