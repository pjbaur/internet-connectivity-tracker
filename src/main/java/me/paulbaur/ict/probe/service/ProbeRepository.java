package me.paulbaur.ict.probe.service;

import me.paulbaur.ict.probe.domain.ProbeResult;

import java.time.Instant;
import java.util.List;

public interface ProbeRepository {

    void save(ProbeResult result);

    List<ProbeResult> findRecent(String targetId, int limit);

    List<ProbeResult> findBetween(
            String targetId,
            Instant start,
            Instant end
    );
}
