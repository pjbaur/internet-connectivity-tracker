package me.paulbaur.ict.probe.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.paulbaur.ict.common.metrics.ProbeMetrics;
import me.paulbaur.ict.common.model.ProbeMethod;
import me.paulbaur.ict.common.model.ProbeStatus;
import me.paulbaur.ict.common.logging.LoggingContext;
import me.paulbaur.ict.probe.domain.ProbeRequest;
import me.paulbaur.ict.probe.domain.ProbeResult;
import me.paulbaur.ict.probe.service.strategy.ProbeStrategy;
import me.paulbaur.ict.probe.service.strategy.ProbeStrategyFactory;
import me.paulbaur.ict.target.domain.Target;
import me.paulbaur.ict.target.store.TargetRepository;

import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProbeServiceImpl implements ProbeService {

    private final RoundRobinTargetSelector targetSelector;
    private final ProbeStrategyFactory probeStrategyFactory;
    private final ProbeRepository probeRepository;
    private final TargetRepository targetRepository;
    private final ProbeMetrics probeMetrics;

    public ProbeResult probe(Target target) {
        String probeCycleId = resolveProbeCycleId();
        return probe(target, probeCycleId);
    }

    @Override
    public void runScheduledProbes() {
        String probeCycleId = resolveProbeCycleId();
        try (LoggingContext ignored = LoggingContext.withValue("probeCycleId", probeCycleId)) {
            log.debug("Running scheduled probes", kv("probeCycleId", probeCycleId));
            Target target = targetSelector.nextTarget();

            if (target != null) {
                probe(target, probeCycleId);
            } else {
                log.warn("No targets configured - skipping probe tick", kv("probeCycleId", probeCycleId));
            }
        } catch (Exception ex) {
            // Critical: ensure the scheduler thread never dies
            log.error("Unexpected error in scheduled probe execution loop", kv("probeCycleId", probeCycleId), ex);
        }
    }

    private ProbeResult probe(Target target, String probeCycleId) {
        try (LoggingContext ignored = LoggingContext.withValues(Map.of(
                "probeCycleId", probeCycleId,
                "targetId", target.getId().toString()
        ))) {
            log.debug(
                    "Initiating probe for target",
                    kv("targetId", target.getId()),
                    kv("host", target.getHost()),
                    kv("port", target.getPort()),
                    kv("probeCycleId", probeCycleId)
            );
            ProbeRequest request = new ProbeRequest(
                    target.getId().toString(),
                    target.getHost(),
                    target.getPort(),
                    probeCycleId
            );

            try {
                // Select the appropriate strategy based on target configuration
                ProbeStrategy strategy = probeStrategyFactory.getStrategy(target);
                ProbeResult result = strategy.probe(request);
                ProbeResult alignedResult = alignProbeCycle(result, probeCycleId);
                probeRepository.save(alignedResult);

                // Record metrics
                probeMetrics.recordProbeExecution(
                        alignedResult.targetId(),
                        alignedResult.status(),
                        alignedResult.method()
                );
                if (alignedResult.latencyMs() != null && alignedResult.status() == ProbeStatus.UP) {
                    probeMetrics.recordProbeLatency(
                            alignedResult.targetId(),
                            alignedResult.method(),
                            alignedResult.latencyMs()
                    );
                }

                log.info(
                    "Probe completed for target",
                    kv("targetId", target.getId()),
                    kv("host", target.getHost()),
                    kv("port", target.getPort()),
                    kv("status", alignedResult.status()),
                    kv("latencyMs", alignedResult.latencyMs()),
                    kv("method", alignedResult.method()),
                    kv("probeCycleId", probeCycleId)
                );

                return alignedResult;
            } catch (Exception ex) {
                // Catch unexpected exceptions from the strategy or repository
                log.error(
                        "Unexpected error during probe",
                        kv("targetId", target.getId()),
                        kv("host", target.getHost()),
                        kv("port", target.getPort()),
                        kv("status", ProbeStatus.DOWN),
                        kv("method", ProbeMethod.TCP),
                        kv("probeCycleId", probeCycleId),
                        kv("error", ex.getMessage()),
                        ex
                );

                // Create a failure result to ensure the system remains stable
                ProbeResult failureResult = new ProbeResult(
                        Instant.now(),
                        target.getId().toString(),
                        target.getHost(),
                        null,
                        probeCycleId,
                        ProbeStatus.DOWN,
                        ProbeMethod.TCP, // Assuming TCP, as it's the only strategy for now
                        "unexpected error: " + ex.getMessage()
                );
                probeRepository.save(failureResult);

                // Record failure metrics
                probeMetrics.recordProbeExecution(
                        failureResult.targetId(),
                        failureResult.status(),
                        failureResult.method()
                );

                return failureResult;
            }
        }
    }

    @Override
    public List<ProbeResult> getRecentResultsForTarget(String targetId, int limit) {
        try {
            return probeRepository.findRecent(targetId, limit);
        } catch (Exception ex) {
            log.error(
                    "Failed to retrieve recent results",
                    kv("targetId", targetId),
                    kv("limit", limit),
                    ex
            );
            return Collections.emptyList();
        }
    }

    @Override
    public List<ProbeResult> getHistoryForTarget(String targetId, int limit, Instant start, Instant end) {
        try {
            if (start != null && end != null) {
                List<ProbeResult> results = probeRepository.findBetween(targetId, start, end);
                return results.stream()
                        .limit(limit)
                        .toList();
            }

            return probeRepository.findRecent(targetId, limit);
        } catch (Exception ex) {
            log.error(
                    "Failed to retrieve history for target",
                    kv("targetId", targetId),
                    kv("rangeStart", start),
                    kv("rangeEnd", end),
                    kv("limit", limit),
                    ex
            );
            return Collections.emptyList();
        }
    }

    @Override
    public void probe(String targetId) {
        String probeCycleId = resolveProbeCycleId();
        UUID targetUUID = UUID.fromString(targetId);
        targetRepository.findById(targetUUID)
                .ifPresentOrElse(
                        target -> probe(target, probeCycleId),
                        () -> log.error("Cannot probe target: no target found", kv("targetId", targetId))
                );
    }

    @Override
    public Optional<ProbeResult> getLatestResult() {
        return probeRepository.findLatest();
    }

    private ProbeResult alignProbeCycle(ProbeResult result, String probeCycleId) {
        if (result == null) {
            return null;
        }
        if (probeCycleId.equals(result.probeCycleId())) {
            return result;
        }
        return new ProbeResult(
                result.timestamp(),
                result.targetId(),
                result.targetHost(),
                result.latencyMs(),
                probeCycleId,
                result.status(),
                result.method(),
                result.errorMessage()
        );
    }

    private String resolveProbeCycleId() {
        String existingCycle = MDC.get("probeCycleId");
        return existingCycle != null ? existingCycle : UUID.randomUUID().toString();
    }
}
