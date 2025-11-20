package me.paulbaur.ict.target.api;

import co.elastic.clients.elasticsearch.nodes.Http;
import me.paulbaur.ict.target.domain.Target;
import me.paulbaur.ict.target.service.TargetService;
import me.paulbaur.ict.common.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/targets", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Targets", description = "Management of probe targets")
public class TargetController {

    private final TargetService targetService;

    public TargetController(TargetService targetService) {
        this.targetService = targetService;
    }

    @GetMapping
    @Operation(summary = "List all targets", description = "Returns all configured probe targets.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of targets",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @io.swagger.v3.oas.annotations.media.ArraySchema(schema = @Schema(implementation = Target.class)), examples = {@ExampleObject(value = "[{\"id\":\"00000000-0000-0000-0000-000000000000\",\"name\":\"example.org\",\"address\":\"93.184.216.34\"}]")})
            ),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public List<Target> listTargets() {
        return targetService.findAll();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new target", description = "Create and return a new probe target.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Target created",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Target.class), examples = {@ExampleObject(value = "{\"name\":\"example.org\",\"address\":\"93.184.216.34\"}")})
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request body. TODO: Align controller to return structured ErrorResponse",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @ResponseStatus(HttpStatus.CREATED)
    public Target createTarget(@RequestBody Target target) {
        return targetService.create(target);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a target", description = "Deletes the target with the specified UUID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Target deleted"),
            @ApiResponse(responseCode = "404", description = "Target not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTarget(@PathVariable UUID id) {
        targetService.delete(id);
    }
}
