
---

# 🚩 MVP Completion Checklist (Blocking for v0.1.0)

This section tracks only the work required to ship the **MVP (v0.1.0)** as defined in the PRD, ROADMAP, and ARCHITECTURE:

- TCP-only probing
- Round-robin multi-target scheduling
- Elasticsearch persistence
- `/api/status` + `/api/history` + basic target management
- Docker + docker-compose stack
- Basic CI and Kibana dashboards
- Minimal but meaningful tests

---

## ✅ A. Probe Execution (TcpProbeStrategy + ProbeServiceImpl)

### ◻ A1. Implement core TCP probing in `TcpProbeStrategy`
- Implement `probe(ProbeRequest request)` to:
    - [ ] Open a TCP socket to the target host/port.
    - [ ] Enforce a configurable connect timeout.
    - [ ] Measure latency (e.g., `System.nanoTime()` before/after connect).
    - [ ] Return `ProbeResult` with:
        - `status = UP` when connect succeeds.
        - `status = DOWN` when connect fails or times out.
        - `latencyMs` populated only for successful probes.
        - `errorMessage` populated for failures (timeout, refused, DNS, etc.).
- Add unit tests:
    - [ ] Success case (mock socket/connect).
    - [ ] Timeout scenario.
    - [ ] Connection refused scenario.
    - [ ] Unknown host / DNS failure.
- Ensure all I/O exceptions are caught and mapped to `ProbeResult` rather than leaking upwards.

### ◻ A2. Implement full probing workflow in `ProbeServiceImpl.probe(...)`
- Implement the main probing method to:
    - [ ] Build a `ProbeRequest` from the chosen `Target`.
    - [ ] Delegate to `ProbeStrategy` (currently `TcpProbeStrategy`).
    - [ ] Persist the resulting `ProbeResult` via `ProbeRepository.save(...)`.
    - [ ] Perform structured logging (`log.info/debug/error`) for:
        - target, status, latency, errorMessage, method.
- Error handling:
    - [ ] Wrap unexpected exceptions in a domain-appropriate error and map to a DOWN result (do not crash the scheduler).
    - [ ] Ensure logging includes enough context for debugging (target ID, host, port).
- Add unit tests for `ProbeServiceImpl`:
    - [ ] Verify it calls `ProbeStrategy` with the expected request.
    - [ ] Verify it calls `ProbeRepository.save(...)` with the resulting `ProbeResult`.
    - [ ] Verify logging and behavior on success and failure paths.

### ◻ A3. Implement `getLatestStatus()` in `ProbeServiceImpl`
- [ ] Use `ProbeRepository.findLatest()` (returning `Optional<ProbeResult>`).
- [ ] Decide on behavior when no results exist for MVP (e.g., return `null` and let controller map to 204/200).
- [ ] Add unit test for `getLatestStatus()`:
    - [ ] Case where a latest result is present.
    - [ ] Case where repository returns `Optional.empty()`.

---

## ✅ B. Scheduling & Target Management

### ◻ B1. Implement `TargetManager` (round-robin selection)
- Provide an in-memory, thread-safe manager:
    - [ ] `List<Target> listTargets()`
    - [ ] `Target addTarget(...)`
    - [ ] `void removeTarget(String id)`
    - [ ] `Optional<Target> nextTargetRoundRobin()`
- Behavior:
    - [ ] If there are no targets, `nextTargetRoundRobin()` returns empty.
    - [ ] Round-robin should cycle fairly through all targets.
- Add unit tests:
    - [ ] Single target behavior.
    - [ ] Multiple target cycling.
    - [ ] Removing targets updates the cycle correctly.

### ◻ B2. Implement scheduled probing loop
- Implement a Spring `@Scheduled` method in a scheduler component (or in `ProbeServiceImpl` wrapper) that:
    - [ ] Runs on a configurable fixed interval (`ict.probe.interval-ms`, default 1000ms).
    - [ ] Fetches `nextTargetRoundRobin()` from `TargetManager`.
    - [ ] If a target is present, calls `ProbeService.probe(target)`; otherwise logs a warning but does nothing.
    - [ ] Catches and logs any exception to avoid killing the scheduler thread.
- Add configuration:
    - [ ] Property in `application.yml` / `application.properties` for probe interval.
    - [ ] Reasonable default for MVP (1000ms).

### ◻ B3. Scheduler tests
- [ ] Add unit tests where the scheduling method is invoked directly (no real time delays) to verify:
    - Behavior with 0, 1, and many targets.
    - That it delegates to `ProbeService.probe(...)` exactly as expected.

---

## ✅ C. Elasticsearch Repository Implementation (MVP)

### ◻ C1. Implement `save(ProbeResult result)` in `ElasticProbeRepository`
- [ ] Use Elasticsearch Java API client to index the document into the `probe-results` index.
- [ ] Ensure field mapping matches your `ProbeResult` domain model.
- [ ] Log failures with `log.error(...)` and throw or wrap as a domain exception (depending on your chosen strategy).

### ◻ C2. Implement `findRecent(String targetId, int limit)`
- [ ] Build a bool query filtering by `targetId`.
- [ ] Sort by `timestamp` descending.
- [ ] Limit results (`size = limit`).
- [ ] Map ES hits to `ProbeResult` instances.
- [ ] Return as `List<ProbeResult>`.

### ◻ C3. Implement `findBetween(String targetId, Instant start, Instant end)`
- [ ] Build a bool query:
    - [ ] Filter on `targetId` (keyword or equivalent).
    - [ ] Range query on `timestamp` between `start` and `end`.
- [ ] Sort by `timestamp` ascending.
- [ ] Return ordered `List<ProbeResult>`.

### ◻ C4. Implement `findLatest()` using ES sort/size
- [ ] Search with `size = 1`, sort `timestamp` descending.
- [ ] Wrap the result in `Optional<ProbeResult>`.
- [ ] Log and return `Optional.empty()` on query failures.

### ◻ C5. Elasticsearch integration tests (Testcontainers)
- [ ] Add a Testcontainers-based integration test class for `ElasticProbeRepository`:
    - [ ] Start ephemeral Elasticsearch container.
    - [ ] Create `probe-results` index if needed.
    - [ ] Insert sample `ProbeResult` via `save()`.
    - [ ] Verify `findLatest()`, `findRecent()`, and `findBetween()` behavior.

---

## ✅ D. Target Management API (MVP)

### ◻ D1. Implement target domain + persistence
- [ ] Ensure `Target` domain model is finalized (fields: id/UUID, host, port, label, enabled, etc.).
- [ ] Implement a simple persistence strategy for MVP:
    - In-memory only, OR
    - Elasticsearch index for targets (if desired).
- [ ] Provide a `TargetService` to wrap `TargetManager` and persistence.

### ◻ D2. Implement `TargetController`
- [ ] `GET /api/targets` → list current targets.
- [ ] `POST /api/targets` → add a new target.
- [ ] `DELETE /api/targets/{id}` → remove a target.
- [ ] Basic validation (host non-empty, port in valid range, etc.).
- [ ] Consider simple DTOs for request/response (to avoid leaking internal domain).

### ◻ D3. Tests for target management
- [ ] Unit tests for `TargetService` and/or `TargetManager`.
- [ ] Controller unit tests (using mocked service).
- [ ] Integration test:
    - [ ] Add a target via API.
    - [ ] List targets and assert presence.
    - [ ] Delete and verify removal.

---

## ✅ E. Docker & Docker Compose (MVP)

### ◻ E1. Multi-stage Dockerfile for Spring Boot app
- [ ] Stage 1: Build with Maven (Temurin JDK 21).
- [ ] Stage 2: Minimal runtime image (Temurin JRE or distroless).
- [ ] Copy only the built JAR and required files.
- [ ] Expose app port and define entrypoint.

### ◻ E2. `docker-compose.yml` stack
- [ ] Service: `app` (internet-connectivity-tracker).
- [ ] Service: `elasticsearch` (ES 9.x).
- [ ] Service: `kibana` (Kibana 9.x).
- [ ] Configure:
    - ES heap size, single-node mode for local.
    - Environment variables for ES + Kibana.
    - Health checks or `depends_on` for app startup order.
- [ ] Document commands:
    - `docker compose up --build`
    - `docker compose down -v`

---

## ✅ F. GitHub Actions CI (MVP)

### ◻ F1. CI workflow for build & tests
- [ ] Add `.github/workflows/ci.yml` with:
    - [ ] Checkout repo.
    - [ ] Set up JDK 21.
    - [ ] Cache Maven dependencies.
    - [ ] Run `mvn -B verify` (unit + integration tests, Testcontainers).
- [ ] Ensure Docker is available for Testcontainers (e.g., `services: docker` or appropriate runner configuration).

### ◻ F2. Optional: Docker image build in CI
- [ ] Add a job step to build the Docker image:
    - `docker build -t <image-name> .`
- [ ] Optionally push to GHCR (later phase).

---

## ✅ G. Kibana Dashboards (MVP)

### ◻ G1. Basic Kibana setup
- [ ] Create index pattern for `probe-results`.
- [ ] Create a simple dashboard with:
    - [ ] Line chart: latency over time for a given target.
    - [ ] Data table: last N probe results (timestamp, status, latency, target).
    - [ ] (Optional) uptime visualization (% UP over selected time range).
- [ ] Either:
    - Export dashboard JSON and store in `/docs/kibana/`, OR
    - Document manual setup steps in ARCHITECTURE or a new `KIBANA_SETUP.md`.

---

## ✅ H. Testing & QA for MVP

### ◻ H1. Unit tests (missing pieces)
- [ ] `ProbeServiceImplTest` (service behavior & repository interactions).
- [ ] `TcpProbeStrategyTest` (TCP behavior, timeouts, error mapping).
- [ ] `TargetManagerTest` (round-robin + add/remove).
- [ ] Scheduler method unit tests (invoked directly, not time-based).

### ◻ H2. Integration tests (Spring Boot + ES)
- [ ] `/api/status` integration test:
    - [ ] Seed ES with a latest `ProbeResult`.
    - [ ] Call `/api/status` and validate body.
- [ ] `/api/history` integration test:
    - [ ] Seed ES with multiple results.
    - [ ] Call `/api/history` with and without date filters.
    - [ ] Validate ordering and contents.

---

# **TODO.md — Engineering Backlog (Generated from Past Suggestions)**

This document consolidates all recommended enhancements, next steps, and optional improvements suggested during development of the **Internet Connectivity Tracker**.

---

# ✅ **1. Architecture & Code Structure**

### ◻ Convert `ProbeResult` into a Java 21 `record`

Cleaner, immutable, serialization-friendly.

### ◻ Add DTOs for API boundaries

Avoid exposing internal domain models directly.

### ◻ Move ES index name into configuration

Example:
`ict.elasticsearch.probe-index=probe-results`


And a `@ConfigurationProperties` class.

### ◻ Generate a full, polished `ElasticProbeRepository`

Including:

* mappings
* startup index creation (optional)
* structured queries
* consistent exception handling

### ◻ Apply Lombok cleanup across entire project

Ensure consistent use of `@Slf4j`, `@RequiredArgsConstructor`, etc.

---

# ✅ **2. Probe Service Enhancements**

### ◻ Add more API methods

* `getStatusForTarget(String targetId)`
* `getUptimeSummary()`
* `getLatencySummary()`
* `getRecentFailures()`

### ◻ Add `ProbeServiceImplTest`

Unit tests verifying:

* calls to repository
* calls to strategy
* correct ProbeResult construction
* exception flows

---

# ✅ **3. API Improvements**

### ◻ Add pagination to `/api/history`

`?page=` and `?size=`, or cursor-based.

### ◻ Add filtering to `/api/history`

Filters:

* targetId
* status
* method (TCP/ICMP)

### ◻ Add `/api/targets/{id}/history`

Target-specific endpoint.

### ◻ Add Prometheus `/actuator/prometheus` metrics endpoint

Needs Micrometer + Prometheus registry.

---

# ✅ **4. Elasticsearch & Storage Improvements**

### ◻ Update `ElasticProbeRepository` to fully use `Optional`

Consistent null-safety.

### ◻ Add repository unit tests (Mockito)

Verify:

* ES request DSL correctness
* error handling
* empty results handling

### ◻ Add repository integration tests (Testcontainers Elasticsearch)

Spin up ephemeral ES → write/read → verify.

### ◻ Add ILM (Index Lifecycle Management) in Phase 2

Roll over index based on:

* size
* age

### ◻ Add startup index creation with explicit mappings

E.g. `timestamp` as `date_nanos`.

---

# ✅ **5. Scheduler & Probing Enhancements**

### ◻ Add fake clock–based scheduler unit tests

Test scheduler behavior without delays.

### ◻ Add multiple probe strategies

* ICMP
* Hybrid (fallback TCP → ICMP)
* Future: HTTP, DNS

### ◻ Add circuit breaker for failing targets

Pause probing for X seconds after repeated failures.

### ◻ Add round-robin target selector tests

Test fairness and cycling behavior.

---

# ✅ **6. Observability & Logging**

### ◻ Add structured logging

JSON or key-value logs for:

* probe results
* repository operations
* error handling
* scheduler execution

### ◻ Add request logging middleware

Trace HTTP calls.

### ◻ Add correlation IDs (MDC)

Optional but common in production systems.

### ◻ Add application version endpoint

(e.g., `/api/version`)

---

# ✅ **7. Metrics & Monitoring**

### ◻ Add Prometheus metrics (Phase 3)

Export:

* probe latency histogram
* probe success/failure counters
* uptime gauges

### ◻ Add ES query performance metrics

Track slow queries for debugging.

---

# ✅ **8. Docker & DevOps Enhancements**

### ◻ Generate complete multi-stage Dockerfile

Minimal final image.

### ◻ Create GitHub Actions CI workflow

Steps:

* `mvn verify`
* TestContainers support
* build Docker image
* optionally push to GHCR

### ◻ Add nightly system test workflow

Docker Compose:

* Spring Boot
* Elasticsearch
* Kibana

Automatically run:

* smoke tests
* history queries
* probe simulation

---

# ✅ **9. Frontend & Visualization**

### ◻ Add basic Kibana dashboards

* latency over time
* uptime gauge
* last N probe results table

### ◻ Add optional React UI (Phase 3)

Small dashboard hitting `/api/status` + `/api/history`.

---

# ✅ **10. Documentation**

### ◻ Add `DEVELOPER_GUIDE.md`

Include:

* IntelliJ setup
* Lombok setup
* Annotation processors
* Running tests
* ES + Kibana setup

### ◻ Add `API.md` with detailed endpoint examples

Curl + sample responses.

### ◻ Add `ERROR_HANDLING.md`

Describe exception flow, logging, and repository error wrapping.

### ◻ Add `SCHEDULER_DESIGN.md`

Explain timing, drift avoidance, and round-robin logic.

---

# ✅ **OpenAPI Compliance Checklist (Engineer)**

### Global

* [ ] Add springdoc-openapi dependency
* [ ] Add OpenApiConfig
* [ ] Verify `/swagger-ui.html` functions

### Controllers

* [ ] Add `@Tag` to all Controllers
* [ ] Add `@Operation` to all endpoints
* [ ] Add `@ApiResponses`
* [ ] Add request/response examples
* [ ] Add media type declarations
* [ ] Add query/path param metadata

### DTOs

* [ ] Add `@Schema` annotations
* [ ] Ensure enums have `@Schema(description=...)`
* [ ] Document timestamp formats

### Errors

* [ ] Create ErrorResponse record
* [ ] Document errors in all endpoints

### Integration Tests

* [ ] Add `/v3/api-docs` contract test
* [ ] Verify all paths appear
* [ ] Verify all tags appear

### Documentation

* [ ] Update `/docs/API_SPEC.md`
* [ ] Add OpenAPI endpoint links
* [ ] Document Swagger UI

---
