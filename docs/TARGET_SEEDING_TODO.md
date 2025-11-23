# ✅ **Architect Plan: “Target Seeding From Text File”**

## 1. **Design Decision**

Use **YAML** as the canonical format. JSON allowed later, but YAML will be MVP.

### Why YAML (Architectural Justification)

* Human-readable and easy to maintain.
* Flexible (supports nested structures and future metadata).
* Native to Spring Boot workflows.
* Works well with Jackson (already in Spring Boot).

---

# 2. **File Location & Naming**

Use:

```
src/main/resources/targets.yml
```

Rationale:

* Bundled in the application JAR.
* Easy to override externally if needed (`--spring.config.additional-location`).
* Matches Spring conventions.

---

# 3. **Schema Definition (MVP + Future-Proofed)**

### `targets.yml`

```yaml
schemaVersion: 1
targets:
  - label: "Cloudflare DNS"
    host: "1.1.1.1"
    port: 443
    method: "TCP"          # Optional in MVP, future: enum ProbeMethod
    intervalSeconds: 60    # Optional, future extension
  - label: "Google DNS"
    host: "8.8.8.8"
    port: 53
```

### Required fields (MVP)

* `host`
* `port`

### Optional fields (MVP)

* `label`
* `method`

### Optional fields (future)

* `intervalSeconds`
* `tags`
* `timeoutMillis`
* `strategy` (maps to ProbeStrategy)

---

# 4. **Java Types (Architect Specification)**

### New DTO

`me.paulbaur.ict.target.seed.TargetSeedProperties`

```java
public record TargetSeedProperties(
    int schemaVersion,
    List<TargetDefinition> targets
) {}

public record TargetDefinition(
    String label,
    String host,
    Integer port,
    String method,
    Integer intervalSeconds
) {}
```

### Loader abstraction

`TargetSeedLoader` (interface)

```java
public interface TargetSeedLoader {
    List<TargetDefinition> loadSeeds();
}
```

### YAML implementation

`YamlTargetSeedLoader`

```java
@Service
public class YamlTargetSeedLoader implements TargetSeedLoader {
    // loads /targets.yml via ClassPathResource and Jackson YAML mapper
}
```

### Integration point

Modify `TargetManager`:

* Add a startup initializer that:

  * Loads seeds
  * For each seed, checks if target already exists (host+port uniqueness)
  * Inserts if missing
  * Logs results

---

# 5. **Lifecycle Behavior (Architect Specification)**

### Startup sequence:

1. Spring Boot starts.
2. `YamlTargetSeedLoader` loads and parses `targets.yml`.
3. `TargetManager.initializeFromSeeds(seeds)` runs.
4. Only **missing** targets are added (idempotent).
5. Log each addition:

   * Info: `Seeding target: host=1.1.1.1 port=443`
   * Warn: if seed missing required fields.
   * Error: if parsing fails (fail fast).

### Policy:

* **Fail fast** on invalid YAML or schema mismatch.
* **Skip** invalid targets but log them.

### No runtime auto-reload in MVP.

---

# 6. **Error Handling Strategy**

### Parsing errors

Throw `TargetSeedException`:

```java
public class TargetSeedException extends RuntimeException {
    public TargetSeedException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### Target creation errors

Wrap in `TargetInitializationException`.

### Logging

Use structured logging:

```java
log.info("Seeded target {}", kv("host", def.host()), kv("port", def.port()));
```

---

# 7. **Engineer Implementation Plan**

Below list is what the **Engineer** must build.

---

## 🧩 **Task 1 — Add DTOs**

Create:

```
TargetSeedProperties
TargetDefinition
TargetSeedException
```

Add null-checks and MVP validation:

* host not null
* port not null

---

## 🧩 **Task 2 — Add YAML loader**

Create class:

`me.paulbaur.ict.target.seed.YamlTargetSeedLoader`

Details:

* Annotate with `@Service`
* Use Jackson YAML mapper:

```xml
<dependency>
  <groupId>com.fasterxml.jackson.dataformat</groupId>
  <artifactId>jackson-dataformat-yaml</artifactId>
</dependency>
```

Load via:

```java
var resource = new ClassPathResource("targets.yml");
```

Convert into `TargetSeedProperties`.

---

## 🧩 **Task 3 — Integrate with TargetManager**

Add public method:

```java
public void initializeFromSeeds(List<TargetDefinition> seeds)
```

Responsibilities:

* For each seed:

  * Validate basic fields.
  * Check repository for existing target (`host`, `port`).
  * If missing → create and save.
  * Log result.

**Use only constructor injection**.

---

## 🧩 **Task 4 — Update Startup Configuration**

Add `@PostConstruct` method in a new config class:

`TargetSeedConfiguration`

Example:

```java
@Configuration
@RequiredArgsConstructor
public class TargetSeedConfiguration {

    private final TargetSeedLoader loader;
    private final TargetManager manager;

    @PostConstruct
    public void init() {
        var seeds = loader.loadSeeds();
        manager.initializeFromSeeds(seeds);
    }
}
```

---

## 🧩 **Task 5 — Unit Tests (JUnit 5 + AssertJ)**

### 1. YamlTargetSeedLoaderTest

* Loads test resource `targets.yml`
* Asserts number of targets
* Asserts values parsed correctly
* Asserts schemaVersion read

### 2. TargetManager seed initialization test

* Use Mockito or in-memory repository
* Provide a list with duplicates
* Assert:

  * Only non-existing targets are created
  * Logs emitted (use a log capturing extension)

### 3. Failure cases

* Missing YAML → throw TargetSeedException
* Malformed YAML → throw TargetSeedException
* Missing required fields → skip + log warn

---

## 🧩 **Task 6 — Integration Test (Testcontainers)**

Create:

`TargetSeedIntegrationTest`

Flow:

1. Boot Spring context with Testcontainers Postgres
2. Place a real `targets.yml` under `src/test/resources`
3. Start app
4. Query `TargetRepository`:

   * Assert targets exist
   * Assert idempotency (restart doesn’t duplicate)

---

## 🧩 **Task 7 — Update Documentation**

Engineer must update:

* `/docs/API_SPEC.md`

  * Describe seed behavior, optional fields, schemaVersion.
* `/docs/ARCHITECTURE.md`

  * Add “Startup target seeding” section.
* `/docs/ROADMAP.md`

  * Add Phase 2: external override file.
  * Add Phase 3: live reloading / hot reconfiguration.

---

## 🧩 **Task 8 — Add Logging Patterns**

Follow the project’s structured logging guidelines.

Recommend:

```java
log.info("Seeding target: {}", Map.of("host", host, "port", port));
```

---

# 8. Future Expansion (Architect Notes)

### Phase 2

* Allow external file override via environment variable:
  `ICT_TARGETS_FILE=/etc/ict/targets.yml`

### Phase 3

* Implement live reload via:

  * Spring Cloud Config
  * or FileSystemWatcher

### Phase 4

* Add web endpoint `/api/targets/reload`.

### Phase 5

* Add per-target strategies, intervals, tags.

---
