package me.paulbaur.ict.probe.service.strategy;

import me.paulbaur.ict.common.model.ProbeMethod;
import me.paulbaur.ict.common.model.ProbeStatus;
import me.paulbaur.ict.probe.domain.ProbeRequest;
import me.paulbaur.ict.probe.domain.ProbeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;

@Component
public class TcpProbeStrategy implements ProbeStrategy {

    private static final Logger log = LoggerFactory.getLogger(TcpProbeStrategy.class);

    // TODO: make configurable
    private static final int DEFAULT_TIMEOUT_MS = 1000;

    public ProbeResult probe(ProbeRequest request) {
        Instant start = Instant.now();
        try (Socket socket = new Socket()) {
            socket.connect(
                    new InetSocketAddress(
                            request.host(),
                            request.port()),
                    DEFAULT_TIMEOUT_MS
            );
            long latencyMs = Duration.between(start, Instant.now()).toMillis();
            return new ProbeResult(
                    Instant.now(),
                    request.targetId(),
                    request.host(),
                    latencyMs,
                    ProbeStatus.UP,
                    ProbeMethod.TCP,
                    null
            );
        } catch (Exception ex) {
            long latencyMs = Duration.between(start, Instant.now()).toMillis();
            log.debug("TCP probe failed for {}: {}", request.host(), ex.getMessage() );
            return new ProbeResult(
                    Instant.now(),
                    request.targetId(),
                    request.host(),
                    latencyMs,
                    ProbeStatus.DOWN,
                    ProbeMethod.TCP,
                    ex.getMessage()
            );
        }
    }
}
