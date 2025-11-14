package me.paulbaur.ict.probe.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ProbeScheduler {

    private static final Logger log = LoggerFactory.getLogger(ProbeScheduler.class);

    private final ProbeService probeService;

    public ProbeScheduler(ProbeService probeService) {
        this.probeService = probeService;
    }

    @Scheduled(fixedDelayString = "${ict.probe.interval-ms:1000}")
    public void executeProbes() {
        log.debug("Running scheduled probes");
        probeService.runScheduledProbes();
    }
}
