package me.paulbaur.ict.common.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Indicates whether a probe observed the target as reachable (UP) or unreachable (DOWN)")
public enum ProbeStatus {
    @Schema(description = "Target was reachable at probe time")
    UP,

    @Schema(description = "Target was unreachable at probe time")
    DOWN
}
