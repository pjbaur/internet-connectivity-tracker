## 1. Architectural issues I see so far

Here are the bigger-picture / architectural issues that stood out, grouped roughly by area.

### 1.1 Conflicting `/api/status` controllers (major)

You currently have **two** controllers that both expose `GET /api/status`:

1. `me.paulbaur.ict.status.StatusController`

    * Returns a `ProbeResult` from `ProbeService.getLatestStatus()`.
2. `me.paulbaur.ict.system.api.StatusController`

    * Returns a `StatusSnapshot` and is clearly meant to be the “system-level” view.

Problems:

* **Ambiguous mapping**: Spring will see two `@GetMapping("/api/status")` handlers. That’s a conflict and a runtime error (or at least very confusing for maintainers).
* **Violates your vertical slice story**: You want a `system` slice for high-level status; the extra `status` package is an orphan slice.

Suggestion:

* **Pick one**: Use the `system` slice as the canonical `/api/status`.
* Remove (or repurpose) `me.paulbaur.ict.status.StatusController`.
* Have `system.api.StatusController` internally call `ProbeService` and compute `StatusSnapshot` (e.g., using latest results + per-target rollups).

This keeps status concerns centralized in the `system` slice and avoids API ambiguity.

---

### 1.2 `ProbeController` is incomplete / not aligned with API spec

In `me.paulbaur.ict.probe.api.ProbeController`:

* You have a `recent(...)` method that calls `probeService.getRecentResults(...)`, but it **lacks a mapping annotation** (`@GetMapping`, path, etc.).
* The class is annotated with `@RestController` and `@RequestMapping("/api/probe")`, but:

    * There’s no mapping for a scheduled-probe trigger (probably fine; scheduler uses `ProbeScheduler`).
    * There’s no mapping for an on-demand probe (`/api/probe/{targetId}` or similar).
* The PRD / API spec say:

    * `GET /api/status`
    * `GET /api/history`
    * Future `/api/metrics`
    * Plus “probe result querying” and “target management”

Right now, `/api/history` isn’t implemented; `recent` is close to that but not wired.

Suggestions:

* Implement something like:

  ```java
  @GetMapping("/targets/{targetId}/recent")
  public List<ProbeResult> recent(
          @PathVariable String targetId,
          @RequestParam(defaultValue = "20") int limit
  ) {
      return probeService.getRecentResults(targetId, limit);
  }
  ```

* Optionally add a **true history endpoint** matching `/docs/API_SPEC.md`, maybe in a `history` slice or reuse `probe` if you keep it simple for MVP.

* Decide whether an **on-demand probe** endpoint belongs here (`POST /api/probe/{targetId}` → calls `probeService.probe(targetId)`).

---

### 1.3 `ProbeServiceImpl` responsibilities & API shape

`ProbeService`:

```java
public interface ProbeService {

    void probe(String target);

    ProbeResult getLatestStatus();

    void runScheduledProbes();

    List<ProbeResult> getRecentResults(String targetId, int limit);
}
```

Issues:

1. **`probe(String target)` is unimplemented** in `ProbeServiceImpl` (just a comment).

    * That means any on-demand probe API would be a stub right now.
2. `runScheduledProbes()` and `probe(String target)` likely should **share core probing logic**:

    * Build a `ProbeRequest`
    * Call `probeStrategy.probe(...)`
    * Save via `probeRepository`
3. `runScheduledProbes()` pulls the `Target` via `RoundRobinTargetSelector`, so there’s already some orchestration built in. That’s good, but right now it uses inline logic only there.

Suggestions:

* Extract a private method:

  ```java
  private ProbeResult probeTarget(Target target) { ... }
  ```

  Then:

    * `runScheduledProbes()` does: `Target t = selector.nextTarget(); if (t != null) { probeTarget(t); }`
    * `probe(String targetId)` can:

        * Look up `Target` by id in `TargetRepository` (you’ll need that method), then call `probeTarget(target)`.

* Or, if you want to keep the service “thin”, consider a **dedicated “probing orchestrator”** in the `probe` slice that handles both scheduled and on-demand flows.

---

### 1.4 Elasticsearch repository design issues

In `ElasticProbeRepository`:

* Most methods use the **configurable index** (`this.index` via `@Value("${ict.elasticsearch.index:probe-results}")`).

* But `findLatest()` uses the **hard-coded constant** `INDEX = "probe-results"` instead:

  ```java
  SearchResponse<ProbeResult> response = client.search(s -> s
          .index(INDEX)
          ...
  );
  ```

  That breaks configurability and will confuse you the moment you change the index name.

* There’s also a typo on the field name: `"timesteamp"` instead of `"timestamp"` – that’s more of an implementation bug, but it interacts with your mapping design.

Architecturally:

* It’s best if the repository **never hard-codes** the index name; it should always use the injected config.
* All methods should assume a **consistent mapping**:

    * `timestamp` as a date field
    * `targetId` as keyword
    * etc.

Suggestions:

* Remove `INDEX` constant; use the injected `index` everywhere.
* Fix the `"timesteamp"` typo.
* For the date field, consider using `Instant`-friendly queries directly (you’re currently using `JsonData.of(start.toEpochMilli())`; that’s OK if the field is mapped as a long, but your PRD sounds more like a date field).

---

### 1.5 Status slice vs system slice vs probe slice

Right now the slices are:

* `probe` (API, service, repository, domain)
* `target` (API, service, repository, domain)
* `system` (API, domain)
* `status` (API only) ← this is the odd one

The **Vertical Slice Architecture** you described wants:

* `probe` slice handling probe workflow + storage.
* `target` slice for managing targets.
* `system` slice for health/status/version.

The standalone `status` package is:

* Overlapping with `system`.
* Violating your own “each feature has its own slice” story.

Suggestion:

* Fold `me.paulbaur.ict.status.StatusController` into the `system` slice or remove it.
* Use `system.api.StatusController` as the *only* `/api/status`.

---

### 1.6 Target repository design for future persistence

`InMemoryTargetRepository` is fine for the MVP, but architecturally:

* It lives in `target.service` and is annotated `@Repository`.
* There’s no interface segregation for “read vs write” use-cases.
* There’s no notion of persistence beyond process lifetime (which you’ll probably want later, even for homelab use).

Suggestions (medium-term):

* Keep `TargetRepository` as a **domain interface**.
* Extract persistence impls in a subpackage, e.g.:

    * `target.store.InMemoryTargetRepository`
    * `target.store.ElasticTargetRepository` (if you later store targets in ES) or `target.store.JdbcTargetRepository`.

That keeps your vertical slice cleaner and paves the way for swapping the persistence mechanism.

---

### 1.7 Config and environment concerns

Not strictly “architecture”, but they touch the overall design:

* `ElasticsearchConfig` hard-codes `localhost:9200` in the `RestClient`.
* For a Docker-first application, the app will typically talk to ES via the **service name in the Docker network** (e.g. `elasticsearch:9200`) and should be configurable via `application.yml` / environment variables.

Suggestion:

* Externalize host/port (and scheme) into properties, e.g.:

  ```yaml
  ict:
    elasticsearch:
      host: elasticsearch
      port: 9200
      scheme: http
  ```

* Wire those into `ElasticsearchConfig`.

This keeps config concerns out of the core slices and makes the app environment-agnostic.

---

If you’d like, next step we can:

* Add a small `ProbeServiceImpl` refactor to implement `probe(String targetId)` properly and share logic with `runScheduledProbes()`.
* Clean up the `/api/status` duplication and sketch what `StatusSnapshot` aggregation should look like over `ProbeRepository`.
