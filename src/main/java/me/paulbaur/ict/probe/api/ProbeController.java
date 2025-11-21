package me.paulbaur.ict.probe.api;

import me.paulbaur.ict.common.model.ErrorResponse;
import me.paulbaur.ict.probe.api.dto.ProbeResultDto;
import me.paulbaur.ict.probe.service.ProbeService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
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
 * The ProbeController provides an API for probing internet connectivity.
 * Typically, probing is scheduled, but I want to have an on-demand probe as well.
 */
@RestController
@RequestMapping(path = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(
        name = "Probe Results",
        description = "Operations for retrieving current and historical connectivity probe results."
)
public class ProbeController {

    private static final int MAX_LIMIT = 5000;

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
    @Operation(summary = "Get recent probe results for a target", description = "Returns up to `limit` recent probe results for the given targetId.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "A list of recent probe results",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = ProbeResultDto.class)),
                            examples = {@ExampleObject(name = "recent", value = "[{\"timestamp\":\"2025-11-19T12:34:56Z\",\"targetId\":\"00000000-0000-0000-0000-000000000000\",\"targetHost\":\"example.org\",\"latencyMs\":23,\"status\":\"UP\",\"method\":\"TCP\",\"errorMessage\":null}]")}
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Bad request - invalid parameters",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "500", description = "Internal server error. TODO: Align controller to return structured ErrorResponse",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/probe/targets/{targetId}/recent")
    public ResponseEntity<?> recent(
            @PathVariable String targetId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        if (targetId == null || targetId.isBlank()) {
            return validationError("targetId is required");
        }
        if (!isValidLimit(limit)) {
            return validationError("limit must be between 1 and " + MAX_LIMIT);
        }

        List<ProbeResultDto> results = ProbeResultDto.fromDomainList(probeService.getRecentResults(targetId, limit));
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
                            examples = {@ExampleObject(name = "history", value = "[{\"timestamp\":\"2025-11-19T12:34:56Z\",\"targetId\":\"00000000-0000-0000-0000-000000000000\",\"targetHost\":\"example.org\",\"latencyMs\":45,\"status\":\"UP\",\"method\":\"TCP\",\"errorMessage\":null}]")}
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "500", description = "Internal server error. TODO: Align controller to return structured ErrorResponse",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/history")
    public ResponseEntity<?> history(
            @RequestParam(name = "targetId") String targetId,
            @RequestParam(name = "limit", defaultValue = "100") int limit,
            @RequestParam(name = "start", required = false) String start,
            @RequestParam(name = "end", required = false) String end
    ) {
        if (targetId == null || targetId.isBlank()) {
            return validationError("targetId is required");
        }
        if (!isValidLimit(limit)) {
            return validationError("limit must be between 1 and " + MAX_LIMIT);
        }

        Instant startInstant = parseInstantOrNull("start", start);
        if (startInstant == null && hasText(start)) {
            return validationError("start must be an ISO-8601 timestamp");
        }

        Instant endInstant = parseInstantOrNull("end", end);
        if (endInstant == null && hasText(end)) {
            return validationError("end must be an ISO-8601 timestamp");
        }

        if ((startInstant == null) != (endInstant == null)) {
            return validationError("start and end must both be provided together");
        }

        if (startInstant != null && !startInstant.isBefore(endInstant)) {
            return validationError("start must be before end");
        }

        List<ProbeResultDto> results = ProbeResultDto.fromDomainList(probeService.getHistory(targetId, limit, startInstant, endInstant));
        return ResponseEntity.ok(results);
    }

    private boolean isValidLimit(int limit) {
        return limit > 0 && limit <= MAX_LIMIT;
    }

    private Instant parseInstantOrNull(String paramName, String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            return Instant.parse(trimmed);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private ResponseEntity<ErrorResponse> validationError(String message) {
        ErrorResponse error = new ErrorResponse(message, "VALIDATION_ERROR", Instant.now());
        return ResponseEntity.badRequest().body(error);
    }
}
