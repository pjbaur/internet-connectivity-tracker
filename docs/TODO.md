# **TODO.md — Internet Connectivity Tracker**

**Goal:** Deliver a production-quality **v0.1.0 (MVP)** release of the Internet Connectivity Tracker, followed by structured Phase 2 and Phase 3 improvements.

---

# **📌 Milestone v0.1.0 — MVP (TCP-only, Elasticsearch, API, Docker, CI)**

*(Ordered in exact implementation sequence)*

## **1. Finalize Elasticsearch Query Layer**

### 🔧 Repository Implementation

* [x] Implement `ElasticProbeRepository.findRecent(targetId, limit)`
* [x] Implement `ElasticProbeRepository.findBetween(targetId, start, end)`
* [x] Add proper Elasticsearch 8.x/9.x query DSL:

    * [ ] `DateRangeQuery`
    * [x] `Sort` by timestamp desc
    * [ ] `Size(limit)`
    * [x] `match` target id keyword field
* [x] Ensure index name resolution uses `ict.elasticsearch.index`.

### 🧪 Integration Tests (Testcontainers)

* [x] Add tests for `findRecent()`:

    * [x] Multiple probe results per target
    * [x] Limit enforcement
    * [x] Correct ordering (newest-first)
* [x] Add tests for `findBetween()`:

    * [x] Time-range filtering
    * [x] Behavior when empty
    * [x] Behavior when target not found

---

### More tasks for consideration:
* Replace `Thread.sleep` refresh waits with an explicit refresh API call in tests or use the `IndexRequest` refresh parameter.
* Add more tests to exercise paging and boundary conditions (empty results, large limits).
* Add an integration test that asserts `findLatest()` behavior.

#### Yet more:

I'll create several ready-to-paste Java snippets that produce multiple `ProbeResult` instances with staggered timestamps 
(single-target, multi-target, and a small helper to generate any count/interval). You can drop these into your tests 
(for example `ElasticProbeRepositoryIT`) or use them from a small helper.

Checklist
* Example: fixed list of staggered `ProbeResult` instances for one target
* Example: staggered instances across multiple targets
* Helper method to generate N `ProbeResult` instances with a given interval
* Example usage showing how to save them via `repo.save(...)` and wait for ES refresh
 
#### Fixed list for one target (7 entries, 10s apart)
```java
import java.time.Instant;
import java.util.List;
import me.paulbaur.ict.probe.domain.ProbeResult;
import me.paulbaur.ict.common.model.ProbeStatus;
import me.paulbaur.ict.common.model.ProbeMethod;

List<ProbeResult> results = List.of(
    new ProbeResult(Instant.now().minusSeconds(60), "target-1", "host1.example", 25L, ProbeStatus.UP, ProbeMethod.TCP, null),
    new ProbeResult(Instant.now().minusSeconds(50), "target-1", "host1.example", 30L, ProbeStatus.UP, ProbeMethod.TCP, null),
    new ProbeResult(Instant.now().minusSeconds(40), "target-1", "host1.example", 28L, ProbeStatus.UP, ProbeMethod.TCP, null),
    new ProbeResult(Instant.now().minusSeconds(30), "target-1", "host1.example", 35L, ProbeStatus.DOWN, ProbeMethod.TCP, "timeout"),
    new ProbeResult(Instant.now().minusSeconds(20), "target-1", "host1.example", 22L, ProbeStatus.UP, ProbeMethod.TCP, null),
    new ProbeResult(Instant.now().minusSeconds(10), "target-1", "host1.example", 20L, ProbeStatus.UP, ProbeMethod.TCP, null),
    new ProbeResult(Instant.now(),                  "target-1", "host1.example", 18L, ProbeStatus.UP, ProbeMethod.TCP, null)
)
```
        
#### Multiple targets with staggered timestamps (interleaved)
```java
import java.time.Instant;
import java.util.List;

List<ProbeResult> multi = List.of(
    new ProbeResult(Instant.now().minusSeconds(30), "tA", "a.example", 10L, ProbeStatus.UP,   ProbeMethod.TCP, null),
    new ProbeResult(Instant.now().minusSeconds(25), "tB", "b.example", 50L, ProbeStatus.DOWN, ProbeMethod.TCP, "conn refused"),
    new ProbeResult(Instant.now().minusSeconds(20), "tA", "a.example", 12L, ProbeStatus.UP,   ProbeMethod.TCP, null),
    new ProbeResult(Instant.now().minusSeconds(15), "tC", "c.example", 70L, ProbeStatus.DOWN, ProbeMethod.TCP, "timeout"),
    new ProbeResult(Instant.now().minusSeconds(10), "tB", "b.example", 45L, ProbeStatus.UP,   ProbeMethod.TCP, null),
    new ProbeResult(Instant.now().minusSeconds(5),  "tA", "a.example", 11L, ProbeStatus.UP,   ProbeMethod.TCP, null),
    new ProbeResult(Instant.now(),                  "tC", "c.example", 60L, ProbeStatus.UP,   ProbeMethod.TCP, null)
);
```

#### Helper to generate N staggered ProbeResult instances for a single target
```java
import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import me.paulbaur.ict.probe.domain.ProbeResult;
import me.paulbaur.ict.common.model.ProbeStatus;
import me.paulbaur.ict.common.model.ProbeMethod;

public static List<ProbeResult> generateStaggered(String targetId, String host, int count, Duration interval, Instant start) {
    List<ProbeResult> out = new ArrayList<>(count);
    
    for (int i = 0; i < count; i++) {
        Instant ts = start.minusSeconds(interval.getSeconds() * (count - 1 - i)); // oldest first
        
        // Alternate UP/DOWN for variety
        boolean up = (i % 4) != 3;
        out.add(new ProbeResult(
            ts,
            targetId,
            host,
            up ? 20L + i : null, // latency null when DOWN
            up ? ProbeStatus.UP : ProbeStatus.DOWN,
            ProbeMethod.TCP,
            up ? null : "simulated failure"
        ));
    }
    return out;
}

// Example usage:
List<ProbeResult> generated = generateStaggered("target-x", "hostx", 8, Duration.ofSeconds(10), Instant.now());
```

Saving generated results into repository (example snippet for tests)
```java
// repo is an autowired ProbeRepository in the test
List<ProbeResult> docs = generateStaggered("target-x", "hostx", 8, Duration.ofSeconds(10), Instant.now());
for (ProbeResult r : docs) {
    repo.save(r);
}

// Option A (simple): wait for ES refresh
Thread.sleep(1100);

// Option B (better): if you want to avoid sleeps, either update repo.save to index with refresh or call ES refresh API from test.
```

Notes and tips
* The project uses ISO-8601 Instants (we configured Jackson to serialize Instant correctly), so these `Instant` values will index and be queryable by date-range.
* In tests you should either wait for the ES refresh (as shown) or index with an explicit refresh (not done in the repository currently).
* For ordering assertions, ensure you depend on the timestamp field (descending) and test limit enforcement by calling repo.findRecent(target, limit).

If you want, I can:
* Insert one of these example blocks directly into `ElasticProbeRepositoryIT` for you and run tests, or
* Create a small utility test helper class (e.g., `TestProbeData`) with the generator method and some pre-built datasets. Which would you prefer?

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
