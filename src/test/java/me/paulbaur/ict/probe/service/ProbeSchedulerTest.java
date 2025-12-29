package me.paulbaur.ict.probe.service;

import me.paulbaur.ict.common.health.ProbeSchedulerHealthIndicator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProbeSchedulerTest {

    @Mock
    private ProbeService probeService;

    @Mock
    private ProbeSchedulerHealthIndicator healthIndicator;

    @InjectMocks
    private ProbeScheduler probeScheduler;

    @Test
    void executeProbes_shouldDelegateToProbeService() {
        // When
        probeScheduler.executeProbes();

        // Then
        verify(probeService).runScheduledProbes();
        verify(healthIndicator).recordExecution();
    }
}
