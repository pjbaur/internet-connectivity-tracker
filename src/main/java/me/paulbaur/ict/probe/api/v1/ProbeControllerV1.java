package me.paulbaur.ict.probe.api.v1;

import me.paulbaur.ict.common.exception.NotFoundException;
import me.paulbaur.ict.common.model.ErrorResponse;
import me.paulbaur.ict.probe.api.dto.ProbeResultDto;
import me.paulbaur.ict.probe.service.ProbeService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * ProbeController V1 - Versioned API for probe results.
 * This is the first versioned API endpoint, supporting event-driven architecture and caching.
 */
@RestController
@RequestMapping(path = "/api/v1/probes", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(
        name = "Probe Results V1",
        description = "Version 1 API for retrieving current and historical connectivity probe results with caching support."
)
public class ProbeControllerV1 {

    private static final int MAX_LIMIT = 5000;

    private final ProbeService probeService;

    public ProbeControllerV1(ProbeService probeService) {
        this.probeService = probeService;
    }

    @Operation(
            summary = "Get most recent probe result",
            description = "Returns the most recent probe result recorded across all targets."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Latest probe result",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProbeResultDto.class),
                            examples = {@ExampleObject(name = "latest", value = "{\"timestamp\":\"2025-11-19T12:34:56Z\",\"targetId\":\"00000000-0000-0000-0000-000000000000\",\"targetHost\":\"example.org\",\"latencyMs\":23,\"probeCycleId\":\"e8f0d94e-1c67-4a39-9d34-1c0fbf5b0e4c\",\"status\":\"UP\",\"method\":\"TCP\",\"errorMessage\":null}")})
            ),
            @ApiResponse(responseCode = "404", description = "No probe result available",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/latest")
    public ResponseEntity<ProbeResultDto> latest() {
        return probeService.getLatestResult()
                .map(result -> ResponseEntity.ok(ProbeResultDto.fromDomain(result)))
                .orElseThrow(() -> new NotFoundException("No probe result available"));
    }

    @Operation(
            summary = "Get recent probe results for a target",
            description = "Returns up to `limit` recent probe results for the given targetId. Results are cached."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "A list of recent probe results",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = ProbeResultDto.class)),
                            examples = {@ExampleObject(name = "recent", value = "[{\"timestamp\":\"2025-11-19T12:34:56Z\",\"targetId\":\"00000000-0000-0000-0000-000000000000\",\"targetHost\":\"example.org\",\"latencyMs\":23,\"probeCycleId\":\"e8f0d94e-1c67-4a39-9d34-1c0fbf5b0e4c\",\"status\":\"UP\",\"method\":\"TCP\",\"errorMessage\":null}]")}
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Bad request - invalid parameters",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/targets/{targetId}/recent")
    @Cacheable(value = "probe-results", key = "#targetId + '-' + #limit")
    public ResponseEntity<List<ProbeResultDto>> recent(
            @Parameter(description = "Target ID (UUID)") @PathVariable String targetId,
            @Parameter(description = "Maximum number of results to return", example = "20")
            @RequestParam(defaultValue = "20") int limit
    ) {
        if (!hasText(targetId)) {
            throw new IllegalArgumentException("targetId is required");
        }
        if (!isValidLimit(limit)) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT);
        }

        List<ProbeResultDto> results = ProbeResultDto.fromDomainList(probeService.getRecentResultsForTarget(targetId, limit));
        return ResponseEntity.ok(results);
    }

    @Operation(
            summary = "Get probe history within an optional time range",
            description = "Returns probe results for the target, optionally bounded by `start` and `end` ISO-8601 timestamps. Results are sorted newest first."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Historical probe results",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = ProbeResultDto.class)),
                            examples = {@ExampleObject(name = "history", value = "[{\"timestamp\":\"2025-11-19T12:34:56Z\",\"targetId\":\"00000000-0000-0000-0000-000000000000\",\"targetHost\":\"example.org\",\"latencyMs\":45,\"probeCycleId\":\"e8f0d94e-1c67-4a39-9d34-1c0fbf5b0e4c\",\"status\":\"UP\",\"method\":\"TCP\",\"errorMessage\":null}]")}
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/history")
    public ResponseEntity<List<ProbeResultDto>> history(
            @Parameter(description = "Target ID (UUID)", required = true)
            @RequestParam(name = "targetId") String targetId,
            @Parameter(description = "Maximum number of results to return", example = "100")
            @RequestParam(name = "limit", defaultValue = "100") int limit,
            @Parameter(description = "Inclusive start of the time range (ISO-8601)", example = "2025-11-19T10:00:00Z")
            @RequestParam(name = "start", required = false) String start,
            @Parameter(description = "Inclusive end of the time range (ISO-8601)", example = "2025-11-19T12:00:00Z")
            @RequestParam(name = "end", required = false) String end
    ) {
        if (!hasText(targetId)) {
            throw new IllegalArgumentException("targetId is required");
        }
        if (!isValidLimit(limit)) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT);
        }

        Instant startInstant = parseIsoInstant("start", start);
        Instant endInstant = parseIsoInstant("end", end);

        if ((startInstant == null) != (endInstant == null)) {
            throw new IllegalArgumentException("start and end must both be provided together");
        }

        if (startInstant != null && !startInstant.isBefore(endInstant)) {
            throw new IllegalArgumentException("start must be before end");
        }

        List<ProbeResultDto> results = ProbeResultDto.fromDomainList(probeService.getHistoryForTarget(targetId, limit, startInstant, endInstant));
        return ResponseEntity.ok(results);
    }

    private boolean isValidLimit(int limit) {
        return limit > 0 && limit <= MAX_LIMIT;
    }

    private Instant parseIsoInstant(String paramName, String value) {
        if (!hasText(value)) {
            return null;
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
