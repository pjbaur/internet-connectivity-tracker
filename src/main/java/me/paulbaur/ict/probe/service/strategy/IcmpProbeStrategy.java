package me.paulbaur.ict.probe.service.strategy;

import lombok.extern.slf4j.Slf4j;
import me.paulbaur.ict.common.model.ProbeMethod;
import me.paulbaur.ict.common.model.ProbeStatus;
import me.paulbaur.ict.probe.domain.ProbeRequest;
import me.paulbaur.ict.probe.domain.ProbeResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * ICMP-based probe strategy using native ping command.
 * Executes system ping and parses output to extract latency.
 * Supports Linux, macOS, and Windows platforms.
 */
@Slf4j
@Component
public class IcmpProbeStrategy implements ProbeStrategy {

    private static final int DEFAULT_TIMEOUT_MS = 2000;
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    private static final boolean IS_WINDOWS = OS_NAME.contains("win");

    // Regex patterns to extract latency from ping output
    // Linux/macOS: time=1.23 ms
    // Windows: time=1ms or time<1ms
    private static final Pattern LATENCY_PATTERN_UNIX = Pattern.compile("time[=<](\\d+\\.?\\d*)\\s*ms", Pattern.CASE_INSENSITIVE);
    private static final Pattern LATENCY_PATTERN_WINDOWS = Pattern.compile("time[=<](\\d+)ms", Pattern.CASE_INSENSITIVE);

    @Value("${ict.probe.icmp.timeout-ms:2000}")
    private int defaultTimeoutMs;

    @Value("${ict.probe.icmp.packet-size:32}")
    private int packetSize;

    @Override
    public ProbeResult probe(ProbeRequest request) {
        Instant start = Instant.now();
        String host = request.host();
        String probeCycleId = request.probeCycleId();

        try {
            // Build ping command based on OS
            ProcessBuilder processBuilder = buildPingCommand(host);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            // Read output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // Wait for process to complete with timeout
            boolean completed = process.waitFor(defaultTimeoutMs, TimeUnit.MILLISECONDS);

            if (!completed) {
                process.destroyForcibly();
                log.warn(
                        "ICMP probe timed out",
                        kv("targetId", request.targetId()),
                        kv("host", host),
                        kv("status", ProbeStatus.DOWN),
                        kv("method", ProbeMethod.ICMP),
                        kv("probeCycleId", probeCycleId),
                        kv("timeoutMs", defaultTimeoutMs)
                );
                return createFailureResult(start, request, "ping timed out");
            }

            int exitCode = process.exitValue();

            if (exitCode == 0) {
                // Parse latency from output
                Long latencyMs = parseLatency(output.toString());

                if (latencyMs != null) {
                    log.debug(
                            "ICMP probe succeeded",
                            kv("targetId", request.targetId()),
                            kv("host", host),
                            kv("latencyMs", latencyMs),
                            kv("status", ProbeStatus.UP),
                            kv("method", ProbeMethod.ICMP),
                            kv("probeCycleId", probeCycleId)
                    );

                    return new ProbeResult(
                            start,
                            request.targetId(),
                            host,
                            latencyMs,
                            probeCycleId,
                            ProbeStatus.UP,
                            ProbeMethod.ICMP,
                            null
                    );
                } else {
                    // Ping succeeded but couldn't parse latency
                    log.warn(
                            "ICMP probe succeeded but failed to parse latency",
                            kv("targetId", request.targetId()),
                            kv("host", host),
                            kv("status", ProbeStatus.UP),
                            kv("method", ProbeMethod.ICMP),
                            kv("probeCycleId", probeCycleId),
                            kv("output", output.toString())
                    );
                    // Return success with null latency
                    return new ProbeResult(
                            start,
                            request.targetId(),
                            host,
                            null,
                            probeCycleId,
                            ProbeStatus.UP,
                            ProbeMethod.ICMP,
                            null
                    );
                }
            } else {
                // Ping failed (host unreachable, etc.)
                log.warn(
                        "ICMP probe failed",
                        kv("targetId", request.targetId()),
                        kv("host", host),
                        kv("status", ProbeStatus.DOWN),
                        kv("method", ProbeMethod.ICMP),
                        kv("probeCycleId", probeCycleId),
                        kv("exitCode", exitCode),
                        kv("output", output.toString())
                );
                return createFailureResult(start, request, "ping failed (exit code: " + exitCode + ")");
            }

        } catch (IOException e) {
            log.error(
                    "ICMP probe I/O error",
                    kv("targetId", request.targetId()),
                    kv("host", host),
                    kv("status", ProbeStatus.DOWN),
                    kv("method", ProbeMethod.ICMP),
                    kv("probeCycleId", probeCycleId),
                    kv("error", e.getMessage()),
                    e
            );
            return createFailureResult(start, request, "I/O error: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error(
                    "ICMP probe interrupted",
                    kv("targetId", request.targetId()),
                    kv("host", host),
                    kv("status", ProbeStatus.DOWN),
                    kv("method", ProbeMethod.ICMP),
                    kv("probeCycleId", probeCycleId),
                    kv("error", e.getMessage()),
                    e
            );
            return createFailureResult(start, request, "interrupted: " + e.getMessage());
        } catch (Exception e) {
            log.error(
                    "ICMP probe unexpected error",
                    kv("targetId", request.targetId()),
                    kv("host", host),
                    kv("status", ProbeStatus.DOWN),
                    kv("method", ProbeMethod.ICMP),
                    kv("probeCycleId", probeCycleId),
                    kv("error", e.getMessage()),
                    e
            );
            return createFailureResult(start, request, "unexpected error: " + e.getMessage());
        }
    }

    /**
     * Build the ping command based on the operating system.
     *
     * @param host the host to ping
     * @return ProcessBuilder configured with the appropriate ping command
     */
    private ProcessBuilder buildPingCommand(String host) {
        if (IS_WINDOWS) {
            // Windows: ping -n 1 -w timeout_ms -l packet_size host
            return new ProcessBuilder(
                    "ping",
                    "-n", "1",  // Send 1 packet
                    "-w", String.valueOf(defaultTimeoutMs),  // Timeout in milliseconds
                    "-l", String.valueOf(packetSize),  // Packet size
                    host
            );
        } else {
            // Linux/macOS: ping -c 1 -W timeout_sec -s packet_size host
            int timeoutSec = Math.max(1, defaultTimeoutMs / 1000);  // Convert to seconds, minimum 1
            return new ProcessBuilder(
                    "ping",
                    "-c", "1",  // Send 1 packet
                    "-W", String.valueOf(timeoutSec),  // Timeout in seconds
                    "-s", String.valueOf(packetSize),  // Packet size
                    host
            );
        }
    }

    /**
     * Parse latency from ping output.
     * Handles both Unix (time=1.23 ms) and Windows (time=1ms or time<1ms) formats.
     *
     * @param output the ping command output
     * @return latency in milliseconds, or null if not found
     */
    private Long parseLatency(String output) {
        Pattern pattern = IS_WINDOWS ? LATENCY_PATTERN_WINDOWS : LATENCY_PATTERN_UNIX;
        Matcher matcher = pattern.matcher(output);

        if (matcher.find()) {
            try {
                String latencyStr = matcher.group(1);
                double latency = Double.parseDouble(latencyStr);
                return Math.round(latency);
            } catch (NumberFormatException e) {
                log.debug("Failed to parse latency value", kv("latencyStr", matcher.group(1)), e);
                return null;
            }
        }

        return null;
    }

    private ProbeResult createFailureResult(Instant timestamp, ProbeRequest request, String errorMessage) {
        return new ProbeResult(
                timestamp,
                request.targetId(),
                request.host(),
                null,
                request.probeCycleId(),
                ProbeStatus.DOWN,
                ProbeMethod.ICMP,
                errorMessage
        );
    }
}
