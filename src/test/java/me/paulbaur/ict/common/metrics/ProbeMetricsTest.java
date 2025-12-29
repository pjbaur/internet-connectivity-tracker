package me.paulbaur.ict.common.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import me.paulbaur.ict.common.model.ProbeMethod;
import me.paulbaur.ict.common.model.ProbeStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProbeMetricsTest {

    private MeterRegistry meterRegistry;
    private ProbeMetrics probeMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        probeMetrics = new ProbeMetrics(meterRegistry);
    }

    @Test
    void shouldRecordProbeExecution() {
        probeMetrics.recordProbeExecution("target-1", ProbeStatus.UP, ProbeMethod.TCP);

        double count = meterRegistry.counter("probe.executions.total",
                "targetId", "target-1",
                "status", "UP",
                "method", "TCP").count();

        assertThat(count).isEqualTo(1.0);
    }

    @Test
    void shouldRecordProbeLatency() {
        probeMetrics.recordProbeLatency("target-1", ProbeMethod.TCP, 50L);

        double count = meterRegistry.timer("probe.latency",
                "targetId", "target-1",
                "method", "TCP").count();

        assertThat(count).isEqualTo(1.0);
    }

    @Test
    void shouldRecordElasticsearchOperation() {
        probeMetrics.recordElasticsearchOperation("save", "success");

        double count = meterRegistry.counter("elasticsearch.operations.total",
                "operation", "save",
                "status", "success").count();

        assertThat(count).isEqualTo(1.0);
    }

    @Test
    void shouldRecordElasticsearchOperationDuration() {
        probeMetrics.recordElasticsearchOperationDuration("save", 100L);

        double count = meterRegistry.timer("elasticsearch.operation.duration",
                "operation", "save").count();

        assertThat(count).isEqualTo(1.0);
    }

    @Test
    void shouldDifferentiateByTags() {
        probeMetrics.recordProbeExecution("target-1", ProbeStatus.UP, ProbeMethod.TCP);
        probeMetrics.recordProbeExecution("target-1", ProbeStatus.DOWN, ProbeMethod.TCP);
        probeMetrics.recordProbeExecution("target-2", ProbeStatus.UP, ProbeMethod.TCP);

        double upCount = meterRegistry.counter("probe.executions.total",
                "targetId", "target-1",
                "status", "UP",
                "method", "TCP").count();

        double downCount = meterRegistry.counter("probe.executions.total",
                "targetId", "target-1",
                "status", "DOWN",
                "method", "TCP").count();

        assertThat(upCount).isEqualTo(1.0);
        assertThat(downCount).isEqualTo(1.0);
    }
}
