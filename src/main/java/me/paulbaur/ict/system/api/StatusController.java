package me.paulbaur.ict.system.api;

import lombok.RequiredArgsConstructor;
import me.paulbaur.ict.common.model.ProbeStatus;
import me.paulbaur.ict.probe.domain.ProbeResult;
import me.paulbaur.ict.probe.service.ProbeService;
import me.paulbaur.ict.system.domain.StatusSnapshot;
import me.paulbaur.ict.target.domain.Target;
import me.paulbaur.ict.target.service.TargetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;

import java.time.Instant;
import java.util.List;

// GET /api/status - high-level status view
@RestController
@RequiredArgsConstructor
@Tag(name = "Status", description = "High-level service status endpoints")
public class StatusController {

    private final ProbeService probeService;
    private final TargetService targetService;

    @GetMapping("/api/status")
    @Operation(summary = "Get service status snapshot", description = "Returns a high-level StatusSnapshot computed from recent probe results.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status snapshot",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = StatusSnapshot.class), examples = {@ExampleObject(value = "{\"timestamp\":\"2025-11-19T12:00:00Z\",\"anyDown\":false,\"totalTargets\":3,\"targetsDown\":1,\"unknownTargets\":1}" )})
            )
    })
    public ResponseEntity<StatusSnapshot> getStatus() {
        return ResponseEntity.ok(computeSnapshot());
    }

    private StatusSnapshot computeSnapshot() {
        List<Target> monitoredTargets = targetService.findAll().stream()
                .filter(Target::isEnabled)
                .toList();

        int targetsDown = 0;
        int unknownTargets = 0;
        for (Target target : monitoredTargets) {
            ProbeResult latestResult = latestResultForTarget(target);
            if (latestResult == null) {
                unknownTargets++;
            } else if (latestResult.status() == ProbeStatus.DOWN) {
                targetsDown++;
            }
        }

        boolean anyDown = targetsDown > 0;
        return new StatusSnapshot(
                Instant.now(),
                anyDown,
                monitoredTargets.size(),
                targetsDown,
                unknownTargets
        );
    }

    private ProbeResult latestResultForTarget(Target target) {
        List<ProbeResult> results = probeService.getRecentResults(target.getId().toString(), 1);
        if (results == null || results.isEmpty()) {
            return null;
        }
        return results.get(0);
    }
}
