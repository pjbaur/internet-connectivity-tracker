package me.paulbaur.ict.probe.api;

import me.paulbaur.ict.probe.domain.ProbeResult;
import me.paulbaur.ict.probe.service.ProbeService;
import me.paulbaur.ict.common.model.ErrorResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;

import java.util.List;

/**
 * The ProbeController provides an API for probing internet connectivity.
 * Typically, probing is scheduled, but I want to have an on-demand probe as well.
 */
@RestController
@RequestMapping(path = "/api/probe", produces = MediaType.APPLICATION_JSON_VALUE)
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
    @Operation(summary = "Get recent probe results for a target", description = "Returns up to `limit` recent ProbeResult objects for the given targetId.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "A list of recent probe results",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = ProbeResult.class)),
                            examples = {@ExampleObject(name = "recent", value = "[{\"targetId\":\"target-1\",\"success\":true,\"rttMs\":23,\"timestamp\":\"2025-11-19T12:34:56Z\"}]")}
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Bad request - invalid parameters. TODO: Align controller to return structured ErrorResponse",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "500", description = "Internal server error. TODO: Align controller to return structured ErrorResponse",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/targets/{targetId}/recent")
    public List<ProbeResult> recent(
            @PathVariable String targetId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return probeService.getRecentResults(targetId, limit);
    }
}
