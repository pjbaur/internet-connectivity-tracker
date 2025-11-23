# Logging Guidelines

These conventions keep logs useful for humans and machines while avoiding noise.

## Log levels
- `debug`: chatty details and loop-level progress (scheduler ticks, per-attempt probe diagnostics). Safe to drop in production.
- `info`: successful operations and state changes (seed completed, probe finished with status/latency). Prefer one log per logical action.
- `warn`: user/input issues or transient/system conditions where work continues (validation failures, connection refused, retries). Avoid stack traces; provide the next action for the caller.
- `error`: loss of functionality or unexpected exceptions that need attention. Log the exception object once at the boundary; upstream layers should not re-log the same stack trace.

## Context to include
- Always include available keys: `reqId`, `targetId`, `probeCycleId`, `host`, `port`, `method`, and API `path`.
- Avoid logging request/response bodies; prefer identifiers and counts.

## Noise control
- Repeated TCP connection refusals are rate-limited (once every 30s per host:port); suppressed events drop to `debug`.
- Prefer sampling or de-duplication for bursts instead of downgrading severities.

## Validation logging
- Validation errors are logged at `warn` with actionable messages that match the client response (e.g., which parameter to fix).

## Exceptions
- Include the exception object in only one log per failure. Downstream layers should wrap and propagate without emitting another stack trace unless they handle the error fully.
