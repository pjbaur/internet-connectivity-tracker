# **TODO.md — Internet Connectivity Tracker**

**Goal:** Deliver a production-quality **v0.1.0 (MVP)** release of the Internet Connectivity Tracker, followed by structured Phase 2 and Phase 3 improvements.

---

# **📌 Milestone v0.1.0 — MVP (TCP-only, Elasticsearch, API, Docker, CI)**

*(Ordered in exact implementation sequence)*

## **1. Finalize Elasticsearch Query Layer**

### 🔧 Repository Implementation

* [ ] Implement `ElasticProbeRepository.findRecent(targetId, limit)`
* [ ] Implement `ElasticProbeRepository.findBetween(targetId, start, end)`
* [ ] Add proper Elasticsearch 8.x/9.x query DSL:

    * [ ] `DateRangeQuery`
    * [ ] `Sort` by timestamp desc
    * [ ] `Size(limit)`
    * [ ] `match` target id keyword field
* [ ] Ensure index name resolution uses `ict.elasticsearch.index`.

### 🧪 Integration Tests (Testcontainers)

* [ ] Add tests for `findRecent()`:

    * [ ] Multiple probe results per target
    * [ ] Limit enforcement
    * [ ] Correct ordering (newest-first)
* [ ] Add tests for `findBetween()`:

    * [ ] Time-range filtering
    * [ ] Behavior when empty
    * [ ] Behavior when target not found

---

## **2. Finish History Endpoints**

Located in `ProbeController`.

* [ ] Wire `GET /api/history` → `findRecent()`
* [ ] Add optional query params:

    * `targetId`
    * `limit`
    * `start` / `end` (RFC 3339 timestamps)
* [ ] Validate query parameters & handle invalid ranges
* [ ] Update Swagger/OpenAPI docs
* [ ] Add DTO abstractions if needed (avoid leaking domain objects directly)

### 🧪 Controller Tests

* [ ] Add `ProbeControllerIT` (MockMvc):

    * [ ] 200 OK with valid results
    * [ ] 400 on invalid timestamps
    * [ ] 404 when target ID does not exist
    * [ ] JSON schema validation

---

## **3. Resolve `/api/status` Endpoint Split**

The PRD requires:

* **Status snapshot** (overall system view)
* **Latest probe result** per target or global

Your current `/api/status` mixes these concepts.

### Decision:

* [ ] Convert **`/api/status` → StatusSnapshot`** only
* [ ] Add new endpoint:

    * [ ] `/api/probe-results/latest` or `/api/latest-probe-result`
    * [ ] Returns latest `ProbeResult`

### Required updates:

* [ ] Update `StatusController`
* [ ] Update `ProbeService` naming & methods
* [ ] Update `API_SPEC.md`
* [ ] Update `OpenApiDocsIT`
* [ ] Update system documentation (`ARCHITECTURE.md` / PRD references)

---

## **4. Global Error Handling**

Introduce a consistent, modern Spring error model.

### Add Global Controller Advice

* [ ] Create `@RestControllerAdvice` with handlers for:

    * [ ] `MethodArgumentNotValidException` → 400
    * [ ] `ConstraintViolationException` → 400
    * [ ] Custom `NotFoundException` → 404
    * [ ] `IllegalArgumentException` → 400
    * [ ] Generic `Exception` → 500

### Integrate `ErrorResponse`

* [ ] Ensure all controllers return the unified `ErrorResponse` JSON
* [ ] Remove TODOs related to inconsistent error formats

### Tests

* [ ] Add global error handling unit tests
* [ ] Add controller-level tests verifying the new error shapes

---

## **5. Controller Integration Tests (Coverage Expansion)**

* [ ] `TargetControllerIT`

    * [ ] Create target
    * [ ] Delete target
    * [ ] Bad UUID returns 400
* [ ] `ProbeControllerIT`

    * [ ] Manual probe execution works
    * [ ] History endpoint returns filtered results
* [ ] `StatusControllerIT`

    * [ ] New `/api/probe-results/latest` endpoint
* [ ] `HealthControllerIT`

    * [ ] Returns correct system info JSON

This completes test coverage for the API layer.

---

## **6. Kibana Dashboard Set**

### Create Basic Visualizations

Using local `docker-compose`:

* [ ] Create index pattern for `probe-results`
* [ ] Create visualizations:

    * [ ] Latency over time (per target)
    * [ ] Availability uptime chart
    * [ ] Table of latest results
* [ ] Combine into a single “Connectivity Overview” dashboard

### Export & Commit

* [ ] Export as `.ndjson` into `docs/kibana/`
* [ ] Add import instructions in README

---

## **7. Documentation Alignment (Must for MVP Polish)**

* [ ] Update `ROADMAP.md` (remove ICMP from MVP)
* [ ] Update `API_SPEC.md` (new endpoint names)
* [ ] Update `PRD.md` if needed
* [ ] Update `ARCHITECTURE.md` where endpoints or flows changed
* [ ] Update `TEST_PLAN.md` to describe new test cases
* [ ] Update `README.md` for:

    * [ ] Running with Docker Compose
    * [ ] Accessing Kibana
    * [ ] API examples
    * [ ] Notes that MVP is TCP-only

---

## **8. Release Prep**

* [ ] Bump version to `v0.1.0` in `pom.xml`
* [ ] Tag release in Git
* [ ] Create GitHub Release:

    * [ ] Attach Kibana dashboard export
    * [ ] Add screenshots
    * [ ] Add architecture diagram

**At this point your MVP is complete.**

---

# **📦 Milestone v0.2.x — Phase 2 (ICMP, Outage Detection, Summary Aggregation)**

## **ICMP & Hybrid Probe Strategies**

* [ ] Implement `IcmpProbeStrategy`
* [ ] Implement `HybridProbeStrategy` fallback chain
* [ ] Add configuration to choose probe type

## **Outage Detection**

* [ ] Implement outage detection state machine
* [ ] Persist `OutageEvent` to Elasticsearch
* [ ] Add “active outage” endpoint
* [ ] Extend Kibana dashboard to show outage timelines

## **Aggregation**

* [ ] Add scheduled jobs to compute:

    * [ ] Hourly summary
    * [ ] Daily summary

## **Performance Improvements**

* [ ] Use ILM + rollover indices
* [ ] Add bulk ingestion mode
* [ ] Improve scheduler throughput

---

# **🚀 Milestone v0.3.x — Phase 3 (Prometheus, Advanced Features)**

## **Prometheus & Metrics**

* [ ] Implement `/actuator/prometheus` or custom metrics endpoint
* [ ] Add Micrometer timers for probe duration
* [ ] Add gauges for active targets

## **Alerting**

* [ ] Basic alert rules in code (email/webhook)
* [ ] Optional integration with Alertmanager
* [ ] Threshold-based latency alerting

## **Advanced Query Features**

* [ ] Paginated history
* [ ] Dashboard widgets for top-N outages
* [ ] Store metadata (RTT distribution, jitter, variance)

---

# **🌐 Milestone v0.4.x — Phase 4 (Optional UI, Cloud Deployment)**

## **Custom Dashboard UI**

* [ ] React/Vite front-end for probe history
* [ ] Latency charts via Recharts or ECharts
* [ ] Target management UI

## **Cloud Deployment Examples**

* [ ] Terraform for AWS
* [ ] Optional serverless ingestion endpoint
* [ ] Managed Elasticsearch blueprint (OpenSearch, Elastic Cloud)

---

# **✨ Milestone v1.0.0 — Production-Grade**

* [ ] Robust distributed scheduler
* [ ] Distributed target caching
* [ ] Kafka (or lightweight queue) ingestion path
* [ ] Multi-node deployment
* [ ] Pluggable probe strategies (DNS, HTTP, TLS, etc.)
* [ ] Enterprise authentication (Keycloak / OAuth2)
* [ ] Full UI + role-based access control

---

# **🟢 Summary**

This updated `TODO.md` gives you:

* A **precise, ordered roadmap** to reach v0.1.0
* Clearly broken down technical tasks
* Integration of your **current architecture**
* Clean boundaries between MVP and later expansions
* Future-proofing through multi-phase releases
