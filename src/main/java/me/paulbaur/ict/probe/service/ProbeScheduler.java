package me.paulbaur.ict.probe.service;

import me.paulbaur.ict.common.health.ProbeSchedulerHealthIndicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ProbeScheduler {

    private static final Logger log = LoggerFactory.getLogger(ProbeScheduler.class);

    private final ProbeService probeService;
    private final ProbeSchedulerHealthIndicator healthIndicator;

    public ProbeScheduler(ProbeService probeService, ProbeSchedulerHealthIndicator healthIndicator) {
        this.probeService = probeService;
        this.healthIndicator = healthIndicator;
    }

    @Scheduled(fixedDelayString = "${ict.probe.interval-ms:1000}")
    public void executeProbes() {
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
