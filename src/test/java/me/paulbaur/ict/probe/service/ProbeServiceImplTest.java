package me.paulbaur.ict.probe.service;

import me.paulbaur.ict.probe.domain.ProbeRequest;
import me.paulbaur.ict.probe.domain.ProbeResult;
import me.paulbaur.ict.probe.service.strategy.ProbeStrategy;
import me.paulbaur.ict.target.domain.Target;
import me.paulbaur.ict.target.service.TargetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.*;

public class ProbeServiceImplTest {

    private RoundRobinTargetSelector selector;
    private ProbeStrategy strategy;
    private ProbeRepository probeRepository;
    private ProbeServiceImpl service;
    private TargetRepository targetRepository;

    @BeforeEach
    void setUp() {
        selector = mock(RoundRobinTargetSelector.class);
        strategy = mock(ProbeStrategy.class);
        probeRepository = mock(ProbeRepository.class);
        service = new ProbeServiceImpl(selector, strategy, probeRepository, targetRepository);
    }

    @Test
    void runScheduledProbesInvokesStrategyAndSavesResult() {
        // Given a configured target and a successful probe result
        Target target = new Target(UUID.randomUUID(), "A", "localhost", 80);
        ProbeResult result = new ProbeResult(
                Instant.now(),
                target.getId().toString(),
                target.getHost(),
                10L,
                null,
                null,
                null
        );

        when(selector.nextTarget()).thenReturn(target);
        when(strategy.probe(any(ProbeRequest.class))).thenReturn(result);

        // When
        service.runScheduledProbes();

        // Then
        verify(selector).nextTarget();
        verify(strategy).probe(any(ProbeRequest.class));
        verify(probeRepository).save(result);
    }

    @Test
    void runScheduledProbesDoesNothingWhenNoTargets() {
        // Given no targets configured
        when(selector.nextTarget()).thenReturn(null);

        // When
        service.runScheduledProbes();

        // Then - srategy and repository must not be invoked
        verify(selector).nextTarget();
        verifyNoInteractions(strategy);
        verifyNoInteractions(probeRepository);
    }
}
