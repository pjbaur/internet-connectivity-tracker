package me.paulbaur.ict.probe.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.paulbaur.ict.probe.domain.ProbeRequest;
import me.paulbaur.ict.probe.domain.ProbeResult;
import me.paulbaur.ict.probe.service.strategy.ProbeStrategy;
import me.paulbaur.ict.target.domain.Target;
import me.paulbaur.ict.target.service.TargetRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProbeServiceImpl implements ProbeService {

    private final RoundRobinTargetSelector targetSelector;
    private final ProbeStrategy probeStrategy;
    private final ProbeRepository probeRepository;
    private final TargetRepository targetRepository;


    /**
     * Probes a single target, saves the result, and returns it.
     *
     * @param target The target to probe.
     * @return The result of the probe.
     */
    private ProbeResult probeTarget(Target target) {
        ProbeRequest request = new ProbeRequest(
                target.getId().toString(),
                target.getHost(),
                target.getPort()
        );

        ProbeResult result = probeStrategy.probe(request);
        probeRepository.save(result);
        return result;
    }

    /**
     * Runs a probe on the next available target in a round-robin fashion.
     * This method is intended to be called by a scheduler. It ensures that no
     * exceptions escape, which could otherwise stop the scheduled execution.
     * If no targets are configured, a warning is logged and the method returns.
     */
    @Override
    public void runScheduledProbes() {
        try {
            Target target = targetSelector.nextTarget();

            if (target != null) {
                probeTarget(target);
            } else {
                log.warn("No targets configured - skipping probe tick");
            }
        } catch (Exception ex) {
            // Critical: never let this escape to @Scheduled
            log.error("Unexpected error in scheduled probe execution", ex);
        }
    }

    public List<ProbeResult> getRecentResults(String targetId, int limit) {
        try {
            return probeRepository.findRecent(targetId, limit);
        } catch (Exception ex) {
            log.error("Failed to retrieve recent results for {}", targetId, ex);
            return Collections.emptyList();
        }
    }

    public void probe(String targetId) {
        // Fetch target from repo
        UUID targetUUID = UUID.fromString(targetId);
        Target target = targetRepository.findById(targetUUID).orElseThrow();

        // get result from strategy, persist to ES, etc.
        probeTarget(target);

    }

    @Override
    public ProbeResult getLatestProbeResult() {
        return probeRepository.findLatest().orElse(null);
    }
}
