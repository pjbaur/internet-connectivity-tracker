package me.paulbaur.ict.analytics.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import me.paulbaur.ict.analytics.domain.LatencyMetrics;
import me.paulbaur.ict.analytics.domain.StateChange;
import me.paulbaur.ict.analytics.domain.TimeSeriesDataPoint;
import me.paulbaur.ict.analytics.domain.UptimeMetrics;
import me.paulbaur.ict.analytics.service.ProbeAnalyticsService;
import me.paulbaur.ict.common.model.ErrorResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * REST API for probe analytics and historical metrics.
 */
@RestController
@RequestMapping(path = "/api/analytics", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(
        name = "Analytics",
        description = "Operations for retrieving aggregated metrics, uptime statistics, and time series data."
)
public class AnalyticsController {

    private static final int MAX_STATE_CHANGE_LIMIT = 1000;
    private static final String DEFAULT_TIME_SERIES_INTERVAL = "1h";

    private final ProbeAnalyticsService analyticsService;

    @Operation(
            summary = "Get uptime metrics for a target",
            description = "Returns uptime statistics including total probes, successful probes, failed probes, and uptime percentage for the specified time range."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Uptime metrics",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UptimeMetrics.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/targets/{targetId}/uptime")
    public ResponseEntity<UptimeMetrics> getUptimeMetrics(
            @Parameter(description = "Target ID (UUID)", required = true)
            @PathVariable String targetId,
            @Parameter(description = "Start of the time range (ISO-8601)", example = "2025-12-01T00:00:00Z", required = true)
            @RequestParam(name = "start") String start,
            @Parameter(description = "End of the time range (ISO-8601)", example = "2025-12-31T23:59:59Z", required = true)
            @RequestParam(name = "end") String end
    ) {
        validateTargetId(targetId);
        Instant startInstant = parseIsoInstant("start", start);
        Instant endInstant = parseIsoInstant("end", end);
        validateTimeRange(startInstant, endInstant);

        UptimeMetrics metrics = analyticsService.getUptimeMetrics(targetId, startInstant, endInstant);
        return ResponseEntity.ok(metrics);
    }

    @Operation(
            summary = "Get latency metrics for a target",
            description = "Returns latency statistics including average, minimum, and maximum latency for the specified time range."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Latency metrics",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LatencyMetrics.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/targets/{targetId}/latency")
    public ResponseEntity<LatencyMetrics> getLatencyMetrics(
            @Parameter(description = "Target ID (UUID)", required = true)
            @PathVariable String targetId,
            @Parameter(description = "Start of the time range (ISO-8601)", example = "2025-12-01T00:00:00Z", required = true)
            @RequestParam(name = "start") String start,
            @Parameter(description = "End of the time range (ISO-8601)", example = "2025-12-31T23:59:59Z", required = true)
            @RequestParam(name = "end") String end
    ) {
        validateTargetId(targetId);
        Instant startInstant = parseIsoInstant("start", start);
        Instant endInstant = parseIsoInstant("end", end);
        validateTimeRange(startInstant, endInstant);

        LatencyMetrics metrics = analyticsService.getLatencyMetrics(targetId, startInstant, endInstant);
        return ResponseEntity.ok(metrics);
    }

    @Operation(
            summary = "Get state changes for a target",
            description = "Returns a list of state change events (UP to DOWN or DOWN to UP transitions) for the specified time range."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of state changes",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = StateChange.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/targets/{targetId}/state-changes")
    public ResponseEntity<List<StateChange>> getStateChanges(
            @Parameter(description = "Target ID (UUID)", required = true)
            @PathVariable String targetId,
            @Parameter(description = "Start of the time range (ISO-8601)", example = "2025-12-01T00:00:00Z", required = true)
            @RequestParam(name = "start") String start,
            @Parameter(description = "End of the time range (ISO-8601)", example = "2025-12-31T23:59:59Z", required = true)
            @RequestParam(name = "end") String end,
            @Parameter(description = "Maximum number of state changes to return", example = "100")
            @RequestParam(name = "limit", defaultValue = "100") int limit
    ) {
        validateTargetId(targetId);
        Instant startInstant = parseIsoInstant("start", start);
        Instant endInstant = parseIsoInstant("end", end);
        validateTimeRange(startInstant, endInstant);
        validateStateChangeLimit(limit);

        List<StateChange> stateChanges = analyticsService.getStateChanges(targetId, startInstant, endInstant, limit);
        return ResponseEntity.ok(stateChanges);
    }

    @Operation(
            summary = "Get time series data for a target",
            description = "Returns time-bucketed metrics including uptime percentage, average latency, and probe counts for the specified time range."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Time series data points",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TimeSeriesDataPoint.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/targets/{targetId}/time-series")
    public ResponseEntity<List<TimeSeriesDataPoint>> getTimeSeries(
            @Parameter(description = "Target ID (UUID)", required = true)
            @PathVariable String targetId,
            @Parameter(description = "Start of the time range (ISO-8601)", example = "2025-12-01T00:00:00Z", required = true)
            @RequestParam(name = "start") String start,
            @Parameter(description = "End of the time range (ISO-8601)", example = "2025-12-31T23:59:59Z", required = true)
            @RequestParam(name = "end") String end,
            @Parameter(description = "Time bucket interval (e.g., '1h', '1d')", example = "1h")
            @RequestParam(name = "interval", defaultValue = DEFAULT_TIME_SERIES_INTERVAL) String interval
    ) {
        validateTargetId(targetId);
        Instant startInstant = parseIsoInstant("start", start);
        Instant endInstant = parseIsoInstant("end", end);
        validateTimeRange(startInstant, endInstant);
        validateInterval(interval);

        List<TimeSeriesDataPoint> timeSeries = analyticsService.getTimeSeries(targetId, startInstant, endInstant, interval);
        return ResponseEntity.ok(timeSeries);
    }

    private void validateTargetId(String targetId) {
        if (!hasText(targetId)) {
            throw new IllegalArgumentException("targetId is required");
        }
    }

    private void validateTimeRange(Instant start, Instant end) {
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("start must be before end");
        }
    }

    private void validateStateChangeLimit(int limit) {
        if (limit <= 0 || limit > MAX_STATE_CHANGE_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_STATE_CHANGE_LIMIT);
        }
    }

    private void validateInterval(String interval) {
        if (!hasText(interval)) {
            throw new IllegalArgumentException("interval is required");
        }
        // Basic validation - could be enhanced to validate specific interval formats
        if (!interval.matches("\\d+[smhd]")) {
            throw new IllegalArgumentException("interval must be in format like '1h', '30m', '1d'");
        }
    }

    private Instant parseIsoInstant(String paramName, String value) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(paramName + " is required");
        }
        try {
            return Instant.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(paramName + " must be an ISO-8601 timestamp");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
