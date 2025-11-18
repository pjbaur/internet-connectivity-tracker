package me.paulbaur.ict.probe.service;

import me.paulbaur.ict.probe.domain.ProbeResult;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProbeRepository {

    void save(ProbeResult result);

    List<ProbeResult> findRecent(String targetId, int limit);

    List<ProbeResult> findBetween(
            String targetId,
            Instant start,
            Instant end
    );

    Optional<ProbeResult> findLatest();
}
