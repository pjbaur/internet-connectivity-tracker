package me.paulbaur.ict.probe.service.strategy;

import me.paulbaur.ict.common.model.ProbeStatus;
import me.paulbaur.ict.probe.domain.ProbeRequest;
import me.paulbaur.ict.probe.domain.ProbeResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TcpProbeStrategyTest {

    private final TcpProbeStrategy strategy = new TcpProbeStrategy();

    @Test
    void probeReturnsWhenConnectionSucceeds() {
        ProbeRequest request = new ProbeRequest("t1", "google.com", 80);

        ProbeResult result = strategy.probe(request);

        assertThat(result.status()).isIn(ProbeStatus.UP, ProbeStatus.DOWN);
    }

    void probeReturnsDownForInvalidPort() {
        ProbeRequest request = new ProbeRequest("t1", "localhost", 65000);

        ProbeResult result = strategy.probe(request);

        assertThat(result.status()).isEqualTo(ProbeStatus.DOWN);
        assertThat(result.errorMessage()).isNotNull();
    }
}
