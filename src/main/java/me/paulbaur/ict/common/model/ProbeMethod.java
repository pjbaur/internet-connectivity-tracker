package me.paulbaur.ict.common.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Method used to perform a probe; for example TCP or ICMP")
public enum ProbeMethod {
    @Schema(description = "TCP connect based probe")
    TCP,

    @Schema(description = "ICMP ping based probe")
    ICMP
}
