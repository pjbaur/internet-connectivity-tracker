package me.paulbaur.ict.probe.service;

import me.paulbaur.ict.probe.domain.ProbeRequest;
import me.paulbaur.ict.probe.domain.ProbeResult;
import me.paulbaur.ict.probe.service.strategy.ProbeStrategy;
import me.paulbaur.ict.target.domain.Target;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.*;

public class ProbeServiceImplTest {

    @Test
    void runScheduledProbesInvokesStrategyAndSavesResult() {
        RoundRobinTargetSelector selector = mock(RoundRobinTargetSelector.class);
        ProbeStrategy strategy = mock(ProbeStrategy.class);
        ProbeRepository repo = mock(ProbeRepository.class);

        Target t = new Target(UUID.randomUUID(), "A", "localhost", 80);
        ProbeResult r = new ProbeResult(
                Instant.now(), t.getId().toString(), "localhost",
                10L, null, null, null
        );

        when(selector.nextTarget()).thenReturn(t);
        when(strategy.probe(any(ProbeRequest.class))).thenReturn(r);

        ProbeServiceImpl svc = new ProbeServiceImpl(selector, strategy, repo);

        svc.runScheduledProbes();

        verify(strategy).probe(any());
        verify(repo).save(r);
    }
}
