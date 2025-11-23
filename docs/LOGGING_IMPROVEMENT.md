# Logging Improvement Plan

## Current State (observed)
- Uses default Spring Boot logging (no `logback-spring.xml`), so format/rotation are defaults.
- Mix of Lombok `@Slf4j` and manual `LoggerFactory`; messages are mostly plain strings with positional args (no structured key/value helpers).
- Context is sometimes included, but inconsistently (`Map.of(...)` in `TargetManager`, plain strings elsewhere). No MDC propagation for request IDs or probe cycle IDs.
- Error logs capture stack traces in some paths (`ProbeServiceImpl`, `ElasticProbeRepository`, `GlobalExceptionHandler`), but success paths rarely capture target identifiers in a structured way.
- No guidance for log levels; e.g., scheduler tick uses `debug`, probes use `info`, seeding uses `info`/`warn`, repository failures use `error`.

## Goals
- Make logs machine-friendly (structured key/value) while keeping human-readable console output.
- Establish consistent context (request ID, target ID, probe ID) across scheduler, strategies, repository, and API layers.
- Reduce noise and ensure actionable signals (levels, sampling, and rate limiting where needed).
- Align with future observability stack (Elastic/Kibana) so logs can be queried by keys.

## Plan (phased)

### Phase 1 — Baseline Configuration
- Add `logback-spring.xml` with:
  - Console appender using a concise pattern including level, timestamp, thread, logger, and MDC keys (`reqId`, `targetId`, `probeId`).
  - Optional JSON appender (logstash-logback-encoder) guarded by profile to ship logs to Elasticsearch/console.
  - Rolling policy for file output (if desired) with size/time-based rollover.
- Define default log levels per package: `me.paulbaur.ict` at `INFO`, `probe` subpackage optionally at `DEBUG` when `ict.probe.logging-debug=true`.

### Phase 2 — Structured Logging Adoption
- Introduce `StructuredArguments.kv(...)` (from logstash-logback-encoder) or `Markers.appendEntries(...)` to standardize key/value logging.
- Refactor hotspots to emit keys consistently:
  - `ProbeServiceImpl` and `TcpProbeStrategy` include `targetId`, `host`, `port`, `latencyMs`, `status`, `method`.
  - `ElasticProbeRepository` includes `index`, `targetId`, `range` for queries; avoid logging entire exceptions more than once.
  - `TargetManager` seeding uses structured keys instead of `Map.of(...)`.
  - `YamlTargetSeedLoader` logs `resourcePath` and `schemaVersion` as keys.

### Phase 3 — Context Propagation
- HTTP layer: add a servlet filter to generate/propagate `reqId` (UUID) into MDC for all API calls.
- Scheduler/probe pipeline: generate a `probeCycleId` per tick in `ProbeScheduler`/`ProbeServiceImpl` and propagate through strategy and repository calls (add to MDC or pass via domain model).
- Seed initialization: add `seedRunId` for startup seeding to correlate logs during initialization.

### Phase 4 — Noise Control & Quality
- Define log level guidelines (when to use `debug` vs `info` vs `warn`/`error`) and document them.
- Add rate limiting or sampling where noisy events could occur (e.g., repeated connection refusals in `TcpProbeStrategy`).
- Ensure exception logs include the exception object once; avoid duplicated stack traces in higher layers.
- Add validation logging for user-facing errors with clear, actionable messages in `GlobalExceptionHandler`.

### Phase 5 — Observability Integration
- Wire JSON appender to Elasticsearch (or stdout) in Docker Compose for local ELK triage; document how to toggle.
- Create Kibana saved searches/visualizations keyed on `targetId`, `status`, `method`, `reqId`, `probeCycleId`.
- Add log-based alerts for sustained `error`/`warn` spikes (future).

### Phase 6 — Testing & Verification
- Add log-capturing tests for key flows (probe success/failure, seeding edge cases, repository failures) to ensure log messages contain required keys.
- Add a small logging style guide to `/docs` and enforce via code review checklist.

## Thoughts from **Grok 4.1**

|Item|Recommendation|
|----|--------------|
|File rolling policy in Phase 1|"You wrote “Rolling policy for file output (if desired)”. In a container-first project, consider making file output opt-in only (e.g. active only under spring profile local or file-logging). Otherwise you risk people accidentally enabling file appenders in Kubernetes and filling up ephemeral storage."|
|JSON appender dependency version|Use net.logstash.logback:logstash-logback-encoder 7.4 or 8.0 (both Java-21 compatible). 7.4 is battle-tested; 8.0 has nicer LoggingEventCompositeJsonEncoder defaults.|
|MDC clearing|Add MDC.clear() (or try-with-resources wrapper) in the servlet filter and in the scheduler tick so old values can’t leak between requests/cycles.|
|Rate limiting implementation|The simplest thing that works is ch.qos.logback.ext.RateLimiter from logback-contribs or a small Guava RateLimiter + a Logback TurboFilter. Both are <10 lines.|
|Profile naming for JSON|Common convention is json-logging or elastic. Document how to activate it in docker-compose (SPRING_PROFILES_ACTIVE=json-logging).|
|probeCycleId generation|"A UUID per scheduler tick is perfect, but also consider adding an incrementing cycleNumber (long) — humans love sequential numbers in dashboards."|
|Avoid logging full request/response bodies|"If you ever add REST-client logging, make sure it’s behind DEBUG and truncated, otherwise ES storage explodes."|
