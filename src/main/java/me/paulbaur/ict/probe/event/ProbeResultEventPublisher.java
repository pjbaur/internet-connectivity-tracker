package me.paulbaur.ict.probe.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.paulbaur.ict.common.model.ProbeStatus;
import me.paulbaur.ict.probe.domain.ProbeResult;
import me.paulbaur.ict.probe.service.ProbeRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Publisher for probe result events.
 * Determines if a probe result represents a state change by comparing with the previous result.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProbeResultEventPublisher {

    private final ApplicationEventPublisher eventPublisher;
    private final ProbeRepository probeRepository;

    /**
     * Publish a probe result event.
     * Determines if this is a state change by comparing with the last known status.
     *
     * @param result the probe result to publish
     */
    public void publishProbeResult(ProbeResult result) {
        boolean isStateChange = detectStateChange(result);

        ProbeResultEvent event = new ProbeResultEvent(this, result, isStateChange);

        if (isStateChange) {
            log.info(
                    "Publishing probe result event with state change",
                    kv("targetId", result.targetId()),
                    kv("status", result.status()),
                    kv("isStateChange", true)
            );
        } else {
            log.debug(
                    "Publishing probe result event",
                    kv("targetId", result.targetId()),
                    kv("status", result.status()),
                    kv("isStateChange", false)
            );
        }

        eventPublisher.publishEvent(event);
    }

    /**
     * Detect if this result represents a state change.
     * Compares the current status with the most recent stored result.
     *
     * @param currentResult the current probe result
     * @return true if the status changed from the previous result
     */
    private boolean detectStateChange(ProbeResult currentResult) {
        try {
            // Get the most recent result for this target (before this one is saved)
            Optional<ProbeResult> previousResultOpt = probeRepository
                    .findRecent(currentResult.targetId(), 1)
                    .stream()
                    .findFirst();

            if (previousResultOpt.isEmpty()) {
                // First probe for this target - not considered a state change
                log.debug(
                        "No previous result found for target - not a state change",
                        kv("targetId", currentResult.targetId())
                );
                return false;
            }

            ProbeResult previousResult = previousResultOpt.get();
            ProbeStatus previousStatus = previousResult.status();
            ProbeStatus currentStatus = currentResult.status();

            boolean isChange = previousStatus != currentStatus;

            if (isChange) {
                log.debug(
                        "State change detected",
                        kv("targetId", currentResult.targetId()),
                        kv("previousStatus", previousStatus),
                        kv("currentStatus", currentStatus)
                );
            }

            return isChange;
        } catch (Exception e) {
            log.warn(
                    "Failed to detect state change, assuming no change",
                    kv("targetId", currentResult.targetId()),
                    e
            );
            return false;
        }
    }
}
