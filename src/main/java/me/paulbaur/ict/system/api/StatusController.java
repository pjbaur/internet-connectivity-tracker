package me.paulbaur.ict.system.api;

import lombok.RequiredArgsConstructor;
import me.paulbaur.ict.probe.domain.ProbeResult;
import me.paulbaur.ict.probe.service.ProbeService;
import me.paulbaur.ict.system.domain.StatusSnapshot;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

// GET /api/status - high-level status view
@RestController
@RequiredArgsConstructor
public class StatusController {

    private final ProbeService probeService;

    @GetMapping("/api/status")
    public StatusSnapshot getStatus() {
        // TODO: compute from latest probe results
        return new StatusSnapshot(
                Instant.now(),
                false,
                0,
                0
        );
    }

    @GetMapping("/api/probe-results/latest")
    public ProbeResult getProbeResult() {
        return probeService.getLatestProbeResult();
    }
}
