package me.paulbaur.ict.probe.api;

import me.paulbaur.ict.probe.domain.ProbeResult;
import me.paulbaur.ict.probe.service.ProbeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The ProbeController provides an API for probing internet connectivity.
 * Typically, probing is scheduled, but I want to have an on-demand probe as well.
 */
@RestController
@RequestMapping("/api/probe")
@Tag(
        name = "Probe Results",
        description = "Operations for retrieving current and historical connectivity probe results."
)
public class ProbeController {

    private final ProbeService probeService;

    public ProbeController(ProbeService probeService) {
        this.probeService = probeService;
    }

    /**
     * Returns the most recent probe results for a given target.
     * @param targetId the ID of the target to get results for
     * @param limit the maximum number of results to return
     * @return a list of probe results
     */
    @GetMapping("/targets/{targetId}/recent")
    public List<ProbeResult> recent(
            @PathVariable String targetId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return probeService.getRecentResults(targetId, limit);
    }
}
