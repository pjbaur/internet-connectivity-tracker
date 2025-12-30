package me.paulbaur.ict.probe.service;

import me.paulbaur.ict.common.health.ProbeSchedulerHealthIndicator;
import me.paulbaur.ict.coordination.service.LeaderElectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ProbeScheduler {

    private static final Logger log = LoggerFactory.getLogger(ProbeScheduler.class);

    private final ProbeService probeService;
    private final ProbeSchedulerHealthIndicator healthIndicator;
    private final Optional<LeaderElectionService> leaderElectionService;

    public ProbeScheduler(
            ProbeService probeService,
            ProbeSchedulerHealthIndicator healthIndicator,
            @Autowired(required = false) LeaderElectionService leaderElectionService) {
        this.probeService = probeService;
        this.healthIndicator = healthIndicator;
        this.leaderElectionService = Optional.ofNullable(leaderElectionService);
    }

    @Scheduled(fixedDelayString = "${ict.probe.interval-ms:1000}")
    @Async("probeTaskExecutor")
    public void executeProbes() {
        // If leader election is enabled, only execute if this node is the leader
        if (leaderElectionService.isPresent() && !leaderElectionService.get().isLeader()) {
            log.trace("Skipping probe execution - not the leader node");
            return;
        }

        log.debug("Running scheduled probes");
        try {
            probeService.runScheduledProbes();
            healthIndicator.recordExecution();
        } catch (Exception ex) {
            log.error("Scheduled probe execution failed", ex);
            healthIndicator.recordFailure();
            throw ex;
        }
    }
}
