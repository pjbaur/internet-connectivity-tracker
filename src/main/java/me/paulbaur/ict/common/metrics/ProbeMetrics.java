package me.paulbaur.ict.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import me.paulbaur.ict.common.model.ProbeMethod;
import me.paulbaur.ict.common.model.ProbeStatus;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ProbeMetrics {

    private final MeterRegistry meterRegistry;

    public ProbeMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordProbeExecution(String targetId, ProbeStatus status, ProbeMethod method) {
        Counter.builder("probe.executions.total")
                .tag("targetId", targetId)
                .tag("status", status.name())
                .tag("method", method.name())
                .description("Total number of probe executions")
                .register(meterRegistry)
                .increment();
    }

    public void recordProbeLatency(String targetId, ProbeMethod method, long latencyMs) {
        Timer.builder("probe.latency")
                .tag("targetId", targetId)
                .tag("method", method.name())
                .description("Probe latency distribution")
                .register(meterRegistry)
                .record(latencyMs, TimeUnit.MILLISECONDS);
    }

    public void recordElasticsearchOperation(String operation, String status) {
        Counter.builder("elasticsearch.operations.total")
                .tag("operation", operation)
                .tag("status", status)
                .description("Total number of Elasticsearch operations")
                .register(meterRegistry)
                .increment();
    }

    public void recordElasticsearchOperationDuration(String operation, long durationMs) {
        Timer.builder("elasticsearch.operation.duration")
                .tag("operation", operation)
                .description("Elasticsearch operation duration")
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }
}
