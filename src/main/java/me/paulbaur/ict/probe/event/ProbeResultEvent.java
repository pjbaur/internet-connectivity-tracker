package me.paulbaur.ict.probe.event;

import me.paulbaur.ict.probe.domain.ProbeResult;
import org.springframework.context.ApplicationEvent;

import java.time.Instant;

/**
 * Domain event published when a probe result is available.
 * This event decouples probe execution from storage and other side effects.
 * Listeners can react to probe results without blocking the probe execution.
 */
public class ProbeResultEvent extends ApplicationEvent {

    private final ProbeResult result;
    private final boolean isStateChange;

    /**
     * Create a new ProbeResultEvent.
     *
     * @param source the object that published the event
     * @param result the probe result
     * @param isStateChange whether this result represents a state change (UP->DOWN or DOWN->UP)
     */
    public ProbeResultEvent(Object source, ProbeResult result, boolean isStateChange) {
        super(source);
        this.result = result;
        this.isStateChange = isStateChange;
    }

    public ProbeResult getResult() {
        return result;
    }

    public boolean isStateChange() {
        return isStateChange;
    }

    @Override
    public String toString() {
        return "ProbeResultEvent{" +
                "result=" + result +
                ", isStateChange=" + isStateChange +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}
