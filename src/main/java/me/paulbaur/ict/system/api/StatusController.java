package me.paulbaur.ict.system.api;

import me.paulbaur.ict.system.domain.StatusSnapshot;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

// GET /api/status - high-level status view
@RestController
public class StatusController {

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
}
