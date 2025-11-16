package me.paulbaur.ict.probe.service.strategy;

import lombok.extern.slf4j.Slf4j;
import me.paulbaur.ict.common.model.ProbeMethod;
import me.paulbaur.ict.common.model.ProbeStatus;
import me.paulbaur.ict.probe.domain.ProbeRequest;
import me.paulbaur.ict.probe.domain.ProbeResult;
import me.paulbaur.ict.target.domain.Target;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
public class TcpProbeStrategy implements ProbeStrategy {

    // TODO: externalize in application.yml later
    private static final int DEFAULT_PORT = 80;
    private static final int DEFAULT_TIMEOUT_MS = 1000;

    @Override
    public ProbeResult probe(ProbeRequest request) {
        Instant start = Instant.now();

        String host = request.host();
        int port = request.port();

        try (Socket socket = new Socket()) {
            long beforeConnect = System.nanoTime();

            socket.connect(new InetSocketAddress(host, port), DEFAULT_TIMEOUT_MS);

            long afterConnect = System.nanoTime();
            long latencyMs = (afterConnect - beforeConnect) / 1_000_000;

            log.debug("TCP probe succeeded: {}:{} ({} ms)", host, port, latencyMs);

            return new ProbeResult(
                    start,
                    request.targetId(),
                    host,
                    latencyMs,
                    ProbeStatus.UP,
                    ProbeMethod.TCP,
                    null
            );

        } catch (Exception ex) {
            long latencyMs = Duration.between(start, Instant.now()).toMillis();
            log.warn("TCP probe failed for {}: {}", host, ex.getMessage());

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
