package me.paulbaur.ict.probe.service.strategy;

import me.paulbaur.ict.common.model.ProbeMethod;
import me.paulbaur.ict.common.model.ProbeStatus;
import me.paulbaur.ict.probe.domain.ProbeRequest;
import me.paulbaur.ict.probe.domain.ProbeResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TcpProbeStrategyTest {

    @Test
    @Timeout(2)
    void probe_whenConnectionSucceeds_returnsUp() {
        TcpProbeStrategy strategy = new TcpProbeStrategy(SuccessSocket::new);
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
        TcpProbeStrategy strategy = new TcpProbeStrategy(TimeoutSocket::new);
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
        TcpProbeStrategy strategy = new TcpProbeStrategy(ConnectionRefusedSocket::new);
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
        TcpProbeStrategy strategy = new TcpProbeStrategy(UnknownHostSocket::new);
        String nonExistentHost = "test-" + UUID.randomUUID().toString() + ".invalid";
        ProbeRequest request = new ProbeRequest(UUID.randomUUID().toString(), nonExistentHost, 80);

        ProbeResult result = strategy.probe(request);

        assertThat(result.status()).isEqualTo(ProbeStatus.DOWN);
        assertThat(result.latencyMs()).isNull();
        assertThat(result.errorMessage()).isEqualTo("unknown host");
        assertThat(result.method()).isEqualTo(ProbeMethod.TCP);
    }

    private static class SuccessSocket extends Socket {
        @Override
        public void connect(SocketAddress endpoint, int timeout) {
            // success
        }

        @Override
        public void close() {
            // no-op for test
        }
    }

    private static class TimeoutSocket extends Socket {
        @Override
        public void connect(SocketAddress endpoint, int timeout) throws IOException {
            throw new SocketTimeoutException("connection timed out");
        }

        @Override
        public void close() {
            // no-op for test
        }
    }

    private static class ConnectionRefusedSocket extends Socket {
        @Override
        public void connect(SocketAddress endpoint, int timeout) throws IOException {
            throw new ConnectException("connection refused");
        }

        @Override
        public void close() {
            // no-op for test
        }
    }

    private static class UnknownHostSocket extends Socket {
        @Override
        public void connect(SocketAddress endpoint, int timeout) throws IOException {
            throw new UnknownHostException("unknown host");
        }

        @Override
        public void close() {
            // no-op for test
        }
    }
}
