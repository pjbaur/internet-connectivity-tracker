package me.paulbaur.ict.probe.service.strategy;

import me.paulbaur.ict.common.model.ProbeMethod;
import me.paulbaur.ict.common.model.ProbeStatus;
import me.paulbaur.ict.probe.domain.ProbeRequest;
import me.paulbaur.ict.probe.domain.ProbeResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TcpProbeStrategyTest {

    private final TcpProbeStrategy strategy = new TcpProbeStrategy();

    @Test
    @Timeout(2)
    void probe_whenConnectionSucceeds_returnsUp() {
        // Precondition: Assumes a public, reliable service is listening on this port.
        // This is not a pure unit test, but an integration test for the strategy.
        ProbeRequest request = new ProbeRequest(UUID.randomUUID().toString(), "1.1.1.1", 53);

        ProbeResult result = strategy.probe(request);

        assertThat(result.status()).isEqualTo(ProbeStatus.UP);
        assertThat(result.latencyMs()).isNotNull().isGreaterThanOrEqualTo(0);
        assertThat(result.errorMessage()).isNull();
        assertThat(result.method()).isEqualTo(ProbeMethod.TCP);
        assertThat(result.targetId()).isEqualTo(request.targetId());
        assertThat(result.targetHost()).isEqualTo(request.host());
        assertThat(result.timestamp()).isNotNull();
    }

    @Test
    @Timeout(2)
    void probe_whenConnectionTimesOut_returnsDown() {
        // This IP address is reserved for documentation and is unlikely to respond.
        ProbeRequest request = new ProbeRequest(UUID.randomUUID().toString(), "192.168.2.1", 80);

        ProbeResult result = strategy.probe(request);

        assertThat(result.status()).isEqualTo(ProbeStatus.DOWN);
        assertThat(result.latencyMs()).isNull();
        assertThat(result.errorMessage()).isEqualTo("connection timed out");
        assertThat(result.method()).isEqualTo(ProbeMethod.TCP);
    }

    @Test
    @Timeout(2)
    void probe_whenConnectionIsRefused_returnsDown() {
        // Probing a port on localhost that is very unlikely to be open.
        ProbeRequest request = new ProbeRequest(UUID.randomUUID().toString(), "localhost", 1);

        ProbeResult result = strategy.probe(request);

        assertThat(result.status()).isEqualTo(ProbeStatus.DOWN);
        assertThat(result.latencyMs()).isNull();
        assertThat(result.errorMessage()).isEqualTo("connection refused");
        assertThat(result.method()).isEqualTo(ProbeMethod.TCP);
    }

    @Test
    @Timeout(2)
    void probe_whenHostIsUnknown_returnsDown() {
        // A syntactically valid but non-existent domain name.
        String nonExistentHost = "test-" + UUID.randomUUID().toString() + ".invalid";
        ProbeRequest request = new ProbeRequest(UUID.randomUUID().toString(), nonExistentHost, 80);

        ProbeResult result = strategy.probe(request);

        assertThat(result.status()).isEqualTo(ProbeStatus.DOWN);
        assertThat(result.latencyMs()).isNull();
        assertThat(result.errorMessage()).isEqualTo("unknown host");
        assertThat(result.method()).isEqualTo(ProbeMethod.TCP);
    }
}
