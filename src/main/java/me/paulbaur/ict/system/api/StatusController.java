package me.paulbaur.ict.system.api;

import lombok.RequiredArgsConstructor;
import me.paulbaur.ict.probe.domain.ProbeResult;
import me.paulbaur.ict.probe.service.ProbeService;
import me.paulbaur.ict.system.domain.StatusSnapshot;
import me.paulbaur.ict.common.model.ErrorResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;

import java.time.Instant;

// GET /api/status - high-level status view
@RestController
@RequiredArgsConstructor
@Tag(name = "Status", description = "High-level service status endpoints")
public class StatusController {

    private final ProbeService probeService;

    @GetMapping("/api/status")
    @Operation(summary = "Get service status snapshot", description = "Returns a high-level StatusSnapshot computed from recent probe results.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status snapshot",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = StatusSnapshot.class), examples = {@ExampleObject(value = "{\"timestamp\":\"2025-11-19T12:00:00Z\",\"anyDown\":false,\"totalProbes\":100,\"failedProbes\":0}" )})
            ),
            @ApiResponse(responseCode = "500", description = "Internal server error. TODO: Align controller to return structured ErrorResponse",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            )
    })
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
    @Operation(summary = "Get latest probe result", description = "Returns the most recent ProbeResult collected by the system.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Latest probe result",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProbeResult.class), examples = {@ExampleObject(value = "{\"targetId\":\"target-1\",\"success\":true,\"rttMs\":23,\"timestamp\":\"2025-11-19T12:34:56Z\"}")})
            ),
            @ApiResponse(responseCode = "404", description = "No probe result found. TODO: Align controller to return structured ErrorResponse",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "500", description = "Internal server error. TODO: Align controller to return structured ErrorResponse",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ProbeResult getProbeResult() {
        return probeService.getLatestProbeResult();
    }
}
