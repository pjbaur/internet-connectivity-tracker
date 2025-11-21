package me.paulbaur.ict.system.api;

import lombok.RequiredArgsConstructor;
import me.paulbaur.ict.common.model.ProbeStatus;
import me.paulbaur.ict.probe.api.dto.ProbeResultDto;
import me.paulbaur.ict.probe.domain.ProbeResult;
import me.paulbaur.ict.probe.service.ProbeService;
import me.paulbaur.ict.system.api.dto.StatusSnapshotDto;
import me.paulbaur.ict.system.domain.StatusSnapshot;
import me.paulbaur.ict.target.domain.Target;
import me.paulbaur.ict.target.service.TargetService;
import me.paulbaur.ict.common.model.ErrorResponse;
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
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = StatusSnapshotDto.class), examples = {@ExampleObject(value = "{\"timestamp\":\"2025-11-19T12:00:00Z\",\"anyDown\":false,\"totalTargets\":3,\"targetsDown\":1,\"unknownTargets\":1}" )})
            ),
            @ApiResponse(responseCode = "500", description = "Internal server error. TODO: Align controller to return structured ErrorResponse",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<StatusSnapshotDto> getStatus() {
        StatusSnapshot snapshot = computeSnapshot();
        return ResponseEntity.ok(StatusSnapshotDto.fromDomain(snapshot));
    }

    @GetMapping("/api/probe-results/latest")
    @Operation(summary = "Get latest probe result", description = "Returns the most recent ProbeResult collected by the system.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Latest probe result",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProbeResultDto.class), examples = {@ExampleObject(value = "{\"timestamp\":\"2025-11-19T12:34:56Z\",\"targetId\":\"00000000-0000-0000-0000-000000000000\",\"targetHost\":\"example.org\",\"latencyMs\":23,\"status\":\"UP\",\"method\":\"TCP\",\"errorMessage\":null}")})
            ),
            @ApiResponse(responseCode = "404", description = "No probe result found. TODO: Align controller to return structured ErrorResponse",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "500", description = "Internal server error. TODO: Align controller to return structured ErrorResponse",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<?> getProbeResult() {
        ProbeResult latest = probeService.getLatestProbeResult();
        if (latest == null) {
            ErrorResponse error = new ErrorResponse("No probe result available", "NOT_FOUND", Instant.now());
            return ResponseEntity.status(404).body(error);
        }
        return ResponseEntity.ok(ProbeResultDto.fromDomain(latest));
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
