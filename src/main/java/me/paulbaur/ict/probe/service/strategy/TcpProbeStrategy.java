package me.paulbaur.ict.probe.service.strategy;

import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import me.paulbaur.ict.common.model.ProbeMethod;
import me.paulbaur.ict.common.model.ProbeStatus;
import me.paulbaur.ict.common.logging.LogRateLimiter;
import me.paulbaur.ict.probe.domain.ProbeRequest;
import me.paulbaur.ict.probe.domain.ProbeResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;

@Slf4j
@Component
public class TcpProbeStrategy implements ProbeStrategy {

    // TODO: externalize in application.yml later
    private static final int DEFAULT_TIMEOUT_MS = 1000;
    private static final Duration CONNECTION_REFUSED_LOG_INTERVAL = Duration.ofSeconds(30);
    private static final LogRateLimiter connectionRefusedLimiter =
            new LogRateLimiter(CONNECTION_REFUSED_LOG_INTERVAL);

    private final Supplier<Socket> socketSupplier;
    private final Retry tcpProbeRetry;

    public TcpProbeStrategy() {
        this(Socket::new, null);
    }

    @Autowired
    public TcpProbeStrategy(Retry tcpProbeRetry) {
        this(Socket::new, tcpProbeRetry);
    }

    TcpProbeStrategy(Supplier<Socket> socketSupplier, Retry tcpProbeRetry) {
        this.socketSupplier = Objects.requireNonNull(socketSupplier, "socketSupplier");
        this.tcpProbeRetry = tcpProbeRetry;
    }

    @Override
    public ProbeResult probe(ProbeRequest request) {
        Instant start = Instant.now();
        String host = request.host();
        int port = request.port();
        String probeCycleId = request.probeCycleId();

        try {
            // Wrap the connection attempt with retry if tcpProbeRetry is available
            long latencyMs = executeWithRetry(() -> {
                try (Socket socket = socketSupplier.get()) {
                    long beforeConnect = System.nanoTime();
                    socket.connect(new InetSocketAddress(host, port), DEFAULT_TIMEOUT_MS);
                    long afterConnect = System.nanoTime();

                    return (afterConnect - beforeConnect) / 1_000_000;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            log.debug(
                    "TCP probe succeeded",
                    kv("targetId", request.targetId()),
                    kv("host", host),
                    kv("port", port),
                    kv("latencyMs", latencyMs),
                    kv("status", ProbeStatus.UP),
                    kv("method", ProbeMethod.TCP),
                    kv("probeCycleId", probeCycleId)
            );

            return new ProbeResult(
                    start,
                    request.targetId(),
                    host,
                    latencyMs,
                    probeCycleId,
                    ProbeStatus.UP,
                    ProbeMethod.TCP,
                    null
            );
        } catch (RuntimeException e) {
            Throwable cause = e.getCause();

            if (cause instanceof SocketTimeoutException) {
                log.warn(
                        "TCP probe timed out",
                        kv("targetId", request.targetId()),
                        kv("host", host),
                        kv("port", port),
                        kv("status", ProbeStatus.DOWN),
                        kv("method", ProbeMethod.TCP),
                        kv("probeCycleId", probeCycleId),
                        kv("error", cause.getMessage())
                );
                return createFailureResult(start, request, "connection timed out");
            } else if (cause instanceof ConnectException) {
                ConnectException connectEx = (ConnectException) cause;
                String limiterKey = host + ":" + port;
                boolean shouldLog = connectionRefusedLimiter.shouldLog(limiterKey);

                if (shouldLog) {
                    log.warn(
                            "TCP probe connection refused",
                            kv("targetId", request.targetId()),
                            kv("host", host),
                            kv("port", port),
                            kv("status", ProbeStatus.DOWN),
                            kv("method", ProbeMethod.TCP),
                            kv("probeCycleId", probeCycleId),
                            kv("error", connectEx.getMessage()),
                            kv("rateLimited", false),
                            kv("rateLimitWindowSec", CONNECTION_REFUSED_LOG_INTERVAL.toSeconds())
                    );
                } else {
                    log.debug(
                            "TCP probe connection refused (suppressed by rate limit)",
                            kv("targetId", request.targetId()),
                            kv("host", host),
                            kv("port", port),
                            kv("status", ProbeStatus.DOWN),
                            kv("method", ProbeMethod.TCP),
                            kv("probeCycleId", probeCycleId),
                            kv("error", connectEx.getMessage()),
                            kv("rateLimited", true)
                    );
                }

                return createFailureResult(start, request, "connection refused");
            } else if (cause instanceof UnknownHostException) {
                log.warn(
                        "TCP probe failed: unknown host",
                        kv("targetId", request.targetId()),
                        kv("host", host),
                        kv("port", port),
                        kv("status", ProbeStatus.DOWN),
                        kv("method", ProbeMethod.TCP),
                        kv("probeCycleId", probeCycleId),
                        kv("error", "unknown host")
                );
                return createFailureResult(start, request, "unknown host");
            } else if (cause instanceof IOException) {
                log.warn(
                        "TCP probe I/O error",
                        kv("targetId", request.targetId()),
                        kv("host", host),
                        kv("port", port),
                        kv("status", ProbeStatus.DOWN),
                        kv("method", ProbeMethod.TCP),
                        kv("probeCycleId", probeCycleId),
                        kv("error", cause.getMessage())
                );
                return createFailureResult(start, request, "I/O error: " + cause.getMessage());
            } else {
                // Unexpected exception
                log.error(
                        "TCP probe unexpected error",
                        kv("targetId", request.targetId()),
                        kv("host", host),
                        kv("port", port),
                        kv("status", ProbeStatus.DOWN),
                        kv("method", ProbeMethod.TCP),
                        kv("probeCycleId", probeCycleId),
                        e
                );
                return createFailureResult(start, request, "unexpected error: " + e.getMessage());
            }
        }
    }

    /**
     * Execute the operation with retry if tcpProbeRetry is configured.
     */
    private <T> T executeWithRetry(Supplier<T> operation) {
        if (tcpProbeRetry != null) {
            return Retry.decorateSupplier(tcpProbeRetry, operation).get();
        } else {
            return operation.get();
        }
    }

    private ProbeResult createFailureResult(Instant timestamp, ProbeRequest request, String errorMessage) {
        return new ProbeResult(
                timestamp,
                request.targetId(),
                request.host(),
                null, // Latency is null for failures
                request.probeCycleId(),
                ProbeStatus.DOWN,
                ProbeMethod.TCP,
                errorMessage
        );
    }
}
