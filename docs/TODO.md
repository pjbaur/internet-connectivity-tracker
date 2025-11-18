Here is a clean, consolidated **ENGINEERING TODO.md** containing *all past suggested next steps*, enhancements, cleanups, and optional improvements mentioned throughout our project history.

You can drop this into your repo at:

```
/docs/TODO.md
```

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

```
ict.elasticsearch.probe-index=probe-results
```

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

# **Need Anything Expanded or Implemented?**

If you want, I can now:

✔ Generate full files for any TODO item
✔ Write the Dockerfile
✔ Write a fully working GitHub Actions workflow
✔ Implement pagination, filters, DTOs
✔ Create Testcontainers integration tests
✔ Draft documentation in `/docs`
✔ Add Kibana dashboard JSON exports

Just say:

**Engineer: Implement item X**
