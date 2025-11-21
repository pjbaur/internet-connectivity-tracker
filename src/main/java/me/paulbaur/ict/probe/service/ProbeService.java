package me.paulbaur.ict.probe.service;

import me.paulbaur.ict.probe.domain.ProbeResult;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ProbeService {

    void probe(String target);

    Optional<ProbeResult> getLatestProbeResult();

    void runScheduledProbes();

    List<ProbeResult> getRecentResults(String targetId, int limit);

    List<ProbeResult> getHistory(String targetId, int limit, Instant start, Instant end);
}
