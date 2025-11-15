package me.paulbaur.ict.probe.service;

import me.paulbaur.ict.probe.domain.ProbeResult;

import java.util.List;

public interface ProbeService {

    void probe(String target);

    ProbeResult getLatestStatus();

    void runScheduledProbes();

    List<ProbeResult> getRecentResults(String targetId, int limit);
}
