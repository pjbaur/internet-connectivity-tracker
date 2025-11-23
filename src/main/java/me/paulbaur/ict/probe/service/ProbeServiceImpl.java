package me.paulbaur.ict.probe.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.paulbaur.ict.common.model.ProbeMethod;
import me.paulbaur.ict.common.model.ProbeStatus;
import me.paulbaur.ict.probe.domain.ProbeRequest;
import me.paulbaur.ict.probe.domain.ProbeResult;
import me.paulbaur.ict.probe.service.strategy.ProbeStrategy;
import me.paulbaur.ict.target.domain.Target;
import me.paulbaur.ict.target.store.TargetRepository;

import org.springframework.stereotype.Service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProbeServiceImpl implements ProbeService {

    private final RoundRobinTargetSelector targetSelector;
    private final ProbeStrategy probeStrategy;
    private final ProbeRepository probeRepository;
    private final TargetRepository targetRepository;

    public ProbeResult probe(Target target) {
        log.debug(
                "Initiating probe for target",
                kv("targetId", target.getId()),
                kv("host", target.getHost()),
                kv("port", target.getPort())
        );
        ProbeRequest request = new ProbeRequest(
                target.getId().toString(),
                target.getHost(),
                target.getPort()
        );

        try {
            ProbeResult result = probeStrategy.probe(request);
            probeRepository.save(result);

            log.info(
                    "Probe completed for target",
                    kv("targetId", target.getId()),
                    kv("host", target.getHost()),
                    kv("port", target.getPort()),
                    kv("status", result.status()),
                    kv("latencyMs", result.latencyMs()),
                    kv("method", result.method())
            );

            return result;
        } catch (Exception ex) {
            // Catch unexpected exceptions from the strategy or repository
            log.error(
                    "Unexpected error during probe",
                    kv("targetId", target.getId()),
                    kv("host", target.getHost()),
                    kv("port", target.getPort()),
                    kv("status", ProbeStatus.DOWN),
                    kv("method", ProbeMethod.TCP),
                    kv("error", ex.getMessage()),
                    ex
            );

            // Create a failure result to ensure the system remains stable
            ProbeResult failureResult = new ProbeResult(
                    Instant.now(),
                    target.getId().toString(),
                    target.getHost(),
                    null,
                    ProbeStatus.DOWN,
                    ProbeMethod.TCP, // Assuming TCP, as it's the only strategy for now
                    "unexpected error: " + ex.getMessage()
            );
            probeRepository.save(failureResult);
            return failureResult;
        }
    }

    @Override
    public void runScheduledProbes() {
        try {
            Target target = targetSelector.nextTarget();

            if (target != null) {
                probe(target);
            } else {
                log.warn("No targets configured - skipping probe tick");
            }
        } catch (Exception ex) {
            // Critical: ensure the scheduler thread never dies
            log.error("Unexpected error in scheduled probe execution loop", ex);
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
        UUID targetUUID = UUID.fromString(targetId);
        targetRepository.findById(targetUUID)
                .ifPresentOrElse(
                        this::probe,
                        () -> log.error("Cannot probe target: no target found", kv("targetId", targetId))
                );
    }

    @Override
    public Optional<ProbeResult> getLatestResult() {
        return probeRepository.findLatest();
    }
}
