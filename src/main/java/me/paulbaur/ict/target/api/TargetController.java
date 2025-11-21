package me.paulbaur.ict.target.api;

import me.paulbaur.ict.common.exception.NotFoundException;
import me.paulbaur.ict.common.model.ErrorResponse;
import me.paulbaur.ict.target.api.dto.TargetRequestDto;
import me.paulbaur.ict.target.api.dto.TargetResponseDto;
import me.paulbaur.ict.target.domain.Target;
import me.paulbaur.ict.target.service.TargetService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65535;

    private final TargetService targetService;

    public TargetController(TargetService targetService) {
        this.targetService = targetService;
    }

    @GetMapping
    @Operation(summary = "List all targets", description = "Returns all configured probe targets.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of targets",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @io.swagger.v3.oas.annotations.media.ArraySchema(schema = @Schema(implementation = TargetResponseDto.class)), examples = {@ExampleObject(value = "[{\"id\":\"00000000-0000-0000-0000-000000000000\",\"label\":\"example.org\",\"host\":\"93.184.216.34\",\"port\":80,\"enabled\":true}]")})
            ),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public List<TargetResponseDto> listTargets() {
        return targetService.findAll().stream()
                .map(TargetResponseDto::fromDomain)
                .toList();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new target", description = "Create and return a new probe target.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Target created",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TargetResponseDto.class), examples = {@ExampleObject(value = "{\"id\":\"00000000-0000-0000-0000-000000000000\",\"label\":\"example.org\",\"host\":\"93.184.216.34\",\"port\":80,\"enabled\":true}")})
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<TargetResponseDto> createTarget(@RequestBody(required = false) TargetRequestDto request) {
        validateCreateRequest(request);

        Target newTarget = new Target(
                UUID.randomUUID(),
                request.label().trim(),
                request.host().trim(),
                request.port()
        );
        Target created = targetService.create(newTarget);
        return ResponseEntity.status(HttpStatus.CREATED).body(TargetResponseDto.fromDomain(created));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a target", description = "Deletes the target with the specified UUID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Target deleted"),
            @ApiResponse(responseCode = "400", description = "Invalid target ID",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Target not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<Void> deleteTarget(@PathVariable String id) {
        if (!hasText(id)) {
            throw new IllegalArgumentException("id is required");
        }

        UUID targetId = parseUuid(id);

        boolean deleted = targetService.delete(targetId);
        if (!deleted) {
            throw new NotFoundException("Target not found for id " + id);
        }

        return ResponseEntity.noContent().build();
    }

    private void validateCreateRequest(TargetRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (!hasText(request.label())) {
            throw new IllegalArgumentException("label is required");
        }
        if (!hasText(request.host())) {
            throw new IllegalArgumentException("host is required");
        }
        if (request.port() == null) {
            throw new IllegalArgumentException("port is required");
        }
        if (request.port() < MIN_PORT || request.port() > MAX_PORT) {
            throw new IllegalArgumentException("port must be between " + MIN_PORT + " and " + MAX_PORT);
        }
    }

    private UUID parseUuid(String id) {
        try {
            return UUID.fromString(id.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("id must be a valid UUID", ex);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
