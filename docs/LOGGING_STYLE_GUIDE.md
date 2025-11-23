# Logging Style Guide

- Prefer structured logging using `StructuredArguments.kv(...)` or markers so JSON/ELK pipelines keep keys. Include actionable fields: `reqId`, `targetId`, `probeCycleId`, `probeId`, `seedRunId`, `host`, `port`, `status`, `method`, `latencyMs`, and `error` where relevant.
- Use MDC for lifecycle context (request IDs, probe cycle IDs, seed runs). Scope MDC with `LoggingContext` to avoid leaks between requests or scheduler ticks.
- Choose levels intentionally: `info` for successful state changes, `warn` for user/input/environment issues, `error` for unexpected failures that need operator action. Attach the exception object once at the leaf log.
- Avoid noisy logs: rate-limit repeated warnings with `LogRateLimiter`; keep messages single-line and omit large payloads or secrets.
- When changing logging, add/adjust log-capturing tests to assert required keys so regressions are caught early.
