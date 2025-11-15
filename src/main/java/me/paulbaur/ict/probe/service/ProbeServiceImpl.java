package me.paulbaur.ict.probe.service;

import me.paulbaur.ict.probe.domain.ProbeRequest;
import me.paulbaur.ict.probe.domain.ProbeResult;
import me.paulbaur.ict.probe.service.strategy.ProbeStrategy;
import me.paulbaur.ict.target.domain.Target;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class ProbeServiceImpl implements ProbeService {

    private static final Logger log = LoggerFactory.getLogger(ProbeServiceImpl.class);

    private final RoundRobinTargetSelector targetSelector;
    private final ProbeStrategy probeStrategy;
    private final ProbeRepository probeRepository;

    public ProbeServiceImpl(
            RoundRobinTargetSelector targetSelector,
            ProbeStrategy probeStrategy,
            ProbeRepository probeRepository) {
        this.targetSelector = targetSelector;
        this.probeStrategy = probeStrategy;
        this.probeRepository = probeRepository;
    }

    @Override
    public void runScheduledProbes() {
        try {
            Target target = targetSelector.nextTarget();

            if (target == null) {
                log.warn("No targets configured - skipping probe tick");
                return;
            }

            ProbeRequest request = new ProbeRequest(
                    target.getId().toString(),
                    target.getHost(),
                    target.getPort()
            );

            ProbeResult result = probeStrategy.probe(request);
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
}
