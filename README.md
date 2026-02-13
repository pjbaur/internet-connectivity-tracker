# **Internet Connectivity Tracker**

A production-ready Java 21 + Spring Boot application that monitors internet connectivity using sophisticated probing strategies, event-driven architecture, distributed caching, historical analytics, and multi-node coordination. Built for observability, resilience, and scalability.

---

## **Features**

### **Core Probing**
* OS-agnostic connectivity checks (TCP and ICMP)
* Spring Boot scheduled tasks with round-robin target selection
* Configurable probe intervals (default: 1 second)
* Automatic latency measurement and success/failure tracking
* Support for multiple probe targets

### **REST API**
* Status endpoints (`/api/status`, `/api/probe-results/latest`)
* Historical data queries (`/api/history` with time-range filtering)
* Target management (`/api/targets` - GET/POST/DELETE)
* Analytics API (uptime, latency, state changes, time-series)
* Versioned API support (`/api/v1/**`)
* OpenAPI/Swagger documentation (`/swagger-ui.html`)

### **Historical Analytics**
* Uptime percentage calculation
* Latency statistics (min/avg/max)
* State change tracking (UP/DOWN transitions)
* Time-series data with configurable bucketing
* Elasticsearch aggregations for efficient queries

### **Event-Driven Architecture**
* Spring Events for async processing
* Automatic state change detection (UP ↔ DOWN)
* Decoupled event listeners for storage, caching, and notifications
* Non-blocking async execution

### **Distributed Caching**
* Redis-backed cache with Spring Cache abstraction
* Multiple cache regions (probe-results: 60s, target-status: 30s, analytics: 5min)
* Event-driven cache invalidation on new probe results
* Distributed cache support for multi-node deployments

### **Notifications**
* Webhook notifications on state changes (UP→DOWN, DOWN→UP)
* Configurable notification filters
* Async notification delivery
* Per-target notification configuration

### **Multi-Node Support**
* Redis-based leader election for distributed deployments
* Automatic failover (30-second lease)
* Distributed locking with Redisson
* Only leader executes scheduled probes (prevents duplicate work)

### **Resilience & Error Handling**
* Circuit breaker for Elasticsearch (Resilience4j)
* Retry logic with exponential backoff
* Rate limiting (100 req/min with token bucket algorithm)
* Graceful degradation and comprehensive error handling

### **Observability**
* Spring Boot Actuator with custom health indicators
* Prometheus metrics endpoint (`/actuator/prometheus`)
* Custom metrics for probe execution, latency, and failures
* Structured JSON logging with correlation IDs
* Logstash integration for log aggregation

### **DevOps & Deployment**
* Dockerized application (multi-stage builds)
* Docker Compose stack (app + Elasticsearch + Kibana + Redis + Logstash)
* GitHub Actions CI pipeline with Testcontainers
* Startup target seeding from `targets.yml` (idempotent)
* Index Lifecycle Management (ILM) for Elasticsearch

---

## **Tech Stack**

### **Core**
* Java 21 (Eclipse Temurin)
* Spring Boot 3.3.0
* Maven

### **Storage & Caching**
* Elasticsearch 8.15.0 (Java API Client 8.12.2)
* Redis 7 Alpine (spring-boot-starter-data-redis)
* Spring Cache abstraction

### **Resilience & Reliability**
* Resilience4j 2.1.0 (retry, circuit breaker)
* Bucket4j 8.10.1 (token bucket rate limiting)
* Redisson 3.25.2 (distributed locks, leader election)

### **Observability**
* Spring Boot Actuator
* Micrometer Prometheus registry
* Logstash Logback Encoder 7.4
* Custom health indicators and metrics

### **Async & Concurrency**
* Spring @Async and @EnableScheduling
* ThreadPoolTaskExecutor (configurable thread pools)
* Spring Application Events

### **HTTP & REST**
* Spring WebFlux (WebClient for webhooks)
* Spring Boot Starter Web
* Spring Boot Starter Validation
* OpenAPI/Swagger (springdoc-openapi 2.6.0)

### **Testing**
* JUnit 5
* Mockito & AssertJ
* Testcontainers (Elasticsearch, PostgreSQL)
* Spring Boot Test

### **Containerization**
* Docker (multi-stage builds)
* Docker Compose (5 services: app, Elasticsearch, Kibana, Redis, Logstash)

### **CI/CD**
* GitHub Actions
* Maven caching and Testcontainers support

### **Development**
* IntelliJ IDEA Community Edition on macOS/Intel
* Lombok for boilerplate reduction

---

## **Project Documentation**

Comprehensive documentation lives in the `/docs` directory:

* **`ARCHITECTURE.md`** - System architecture, vertical slice design, event-driven patterns, testing strategy
* **`API_SPEC.md`** - REST API endpoint specifications and examples
* **`ROADMAP.md`** - Feature milestones and development phases
* **`TEST_PLAN.md`** - Testing strategy and coverage guidelines
* **`MASTER_ROLE_SELECTOR.md`** - AI-assisted development workflow and role-based prompting
* **`CLAUDE.md`** (root) - Complete transparency documentation about Claude AI's contributions to this project

---

## Target Seeding

At startup the app seeds probe targets from `src/main/resources/targets.yml`:

```yaml
schemaVersion: 1
targets:
  - label: "Cloudflare DNS"
    host: "1.1.1.1"
    port: 443
    method: "TCP"          # optional in MVP
```

Rules:
- `host` and `port` are required; `label` and `method` are optional in MVP.
- Seeding is idempotent (existing host+port pairs are left untouched).
- Malformed YAML fails fast; invalid rows are skipped with warnings.
- The file can be overridden using Spring’s additional config locations (e.g., `--spring.config.additional-location=file:/etc/ict/targets.yml`).

---

## **Configuration**

The application supports extensive configuration via `application.yml` or environment variables:

### **Elasticsearch**
```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200
```

### **Redis (Caching & Coordination)**
```yaml
spring:
  redis:
    host: localhost
    port: 6379
  cache:
    type: redis
```

### **Probe Scheduling**
```yaml
ict:
  probe:
    interval-ms: 1000  # Probe interval in milliseconds
```

### **Leader Election (Multi-Node)**
```yaml
ict:
  coordination:
    leader-election:
      enabled: true
      lease-duration-seconds: 30
```

### **Notifications**
```yaml
ict:
  notifications:
    enabled: true
    default-webhook-url: https://your-webhook-url.com/notifications
```

### **Resilience (Circuit Breaker & Retry)**
```yaml
resilience4j:
  circuitbreaker:
    instances:
      elasticsearch:
        failure-rate-threshold: 50
        sliding-window-size: 10
  retry:
    instances:
      elasticsearch:
        max-attempts: 3
        wait-duration: 2s
```

### **Caching TTLs**
```yaml
spring:
  cache:
    cache-names: probe-results,target-status,analytics
    redis:
      time-to-live: 60000  # Default TTL in milliseconds
```

### **Observability**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,prometheus,info,metrics
  metrics:
    export:
      prometheus:
        enabled: true
```

---

## Security

This is a dev/homelab project. Do not expose this docker-compose stack directly to the internet. It runs Elasticsearch and Kibana without authentication and the app has no auth (MVP assumption).

---

# **Why This Project Uses Elasticsearch Java API Client 8.12.x**

This project originally attempted to use the **Elasticsearch Java API Client v9.x**.
That journey turned into a multi-hour investigation due to:

### ❌ API shape changes in 9.x

In 9.x, range queries (and several other query types) moved to a new, partially undocumented “variant” builder model.
Methods like:

```java
.field(...)
.gte(...)
.lte(...)
```

—commonly found in official examples, community snippets, and Elastic’s older docs—
**no longer exist on the 9.x range query builder**.

### ❌ IntelliJ false positives

Due to multiple overlapping generated types (`RangeQuery`, `RangeQueryVariant`, `RangeQueryBase`, etc.), IntelliJ would highlight valid code as errors even when built against the correct artifact.

### ❌ Unfinished/underdocumented DSL redesign

The 9.x Java client uses a new TaggedUnion system that requires building queries indirectly via variants or raw JSON nodes.
As a result, simple queries (like date ranges) become significantly more verbose.

### ✔ Returned to Elasticsearch Java API Client 8.12.x

The 8.12.x client is:

* stable
* well documented
* fully type-safe
* supported by official examples
* consistent with the JSON DSL
* IDE-friendly
* compatible with Testcontainers’ Elasticsearch module

Using 8.12.x restored clean functional DSL like:

```java
Query.of(q -> q.range(r -> r
        .field("timestamp")
        .gte(JsonData.of(startMillis))
        .lte(JsonData.of(endMillis))
));
```

which compiles cleanly, reads naturally, and works reliably.

If/when the 9.x client’s DSL stabilizes, the project can migrate forward.
For now, **8.12.x provides the correct balance of stability, clarity, and developer ergonomics**.

---

# **How AI Assisted This Project**

This project was developed using **Claude AI** (Anthropic) as part of a professional, real-world engineering workflow. Claude contributed approximately **80-90% of the initial code generation**, while **100% of architectural decisions, code review, testing, and validation** were performed by the human developer.

**AI was used to:**
* Generate complete vertical slices (controller → service → repository → tests)
* Implement all 6 workstreams (Observability, Resilience, Async/ICMP, Events/Caching, Analytics, Notifications/Multi-Node)
* Create comprehensive test suites (23 test files with JUnit 5, Mockito, AssertJ, Testcontainers)
* Build Docker and Docker Compose infrastructure
* Generate extensive documentation with Mermaid diagrams
* Accelerate boilerplate generation and refactoring
* Propose architectural alternatives and design patterns

**All critical decisions were human-led:**
* Architectural patterns (Vertical Slice Architecture, Event-Driven Architecture)
* Technology selection (Spring Boot, Elasticsearch 8.12.x, Redis, Resilience4j)
* Business logic and configuration (cache TTLs, retry policies, rate limits)
* Security model and deployment strategy
* Code review and quality validation

AI served as a **force multiplier**, not a substitute for engineering ability. This reflects how modern engineering teams use AI today — as a productivity tool that enhances clarity, speed, and structure while preserving human ownership of technical outcomes.

**📄 For complete transparency about Claude's contributions, see [`CLAUDE.md`](CLAUDE.md)** - a comprehensive document detailing exactly what AI built, how it was used, limitations encountered, and best practices learned.

---

# **Quick Start**

## **Running with Docker Compose (Recommended)**

The easiest way to run the full stack (app + Elasticsearch + Kibana + Redis + Logstash):

```bash
# Start the entire stack
docker-compose up --build

# Or run in detached mode
docker-compose up --build -d

# View logs
docker-compose logs -f app

# Stop the stack
docker-compose down
```

**Access Points:**
* Application API: `http://localhost:8080`
* Swagger UI: `http://localhost:8080/swagger-ui.html`
* Kibana: `http://localhost:5601`
* Prometheus Metrics: `http://localhost:8080/actuator/prometheus`
* Health Check: `http://localhost:8080/actuator/health`

## **Running Standalone (Docker)**

Build and run just the application (requires external Elasticsearch and Redis):

```bash
docker build -t connectivity-tracker .
docker run -p 8080:8080 \
  -e SPRING_ELASTICSEARCH_URIS=http://elasticsearch:9200 \
  -e SPRING_REDIS_HOST=redis \
  connectivity-tracker
```

## **Running Locally (Development)**

```bash
# Ensure Elasticsearch and Redis are running (via Docker Compose or locally)
# Then run with Maven:
mvn spring-boot:run

# Or build and run the JAR:
mvn clean package
java -jar target/internet-connectivity-tracker-0.1.0-SNAPSHOT.jar
```

## **Example API Usage**

```bash
# Get current status
curl http://localhost:8080/api/status

# Get latest probe results
curl http://localhost:8080/api/probe-results/latest

# Get probe history (last hour)
curl "http://localhost:8080/api/history?hours=1"

# Add a new target
curl -X POST http://localhost:8080/api/targets \
  -H "Content-Type: application/json" \
  -d '{"label":"Google DNS","host":"8.8.8.8","port":443,"method":"TCP"}'

# Get uptime analytics
curl http://localhost:8080/api/analytics/targets/{targetId}/uptime?hours=24

# Get latency statistics
curl http://localhost:8080/api/analytics/targets/{targetId}/latency?hours=24
```

---

# **Implementation Status**

This project has completed **all 6 planned workstreams**:

✅ **Workstream 1: Observability** - Spring Boot Actuator, Prometheus metrics, custom health indicators, structured logging

✅ **Workstream 2: Resilience & Error Handling** - Resilience4j (retry, circuit breaker), rate limiting with Bucket4j, comprehensive error handling

✅ **Workstream 3: Async Scheduling & ICMP Probe Strategy** - ThreadPoolTaskExecutor, @EnableAsync, IcmpProbeStrategy with OS detection, round-robin target selection

✅ **Workstream 4: Event-Driven Architecture & Caching** - Spring Events, async listeners, Redis-backed caching, event-driven cache invalidation

✅ **Workstream 5: Data Management & Historical Analytics** - Analytics API (uptime, latency, state changes, time-series), Elasticsearch aggregations, configurable time bucketing

✅ **Workstream 6: Notifications & Multi-Node Support** - Webhook notifications, Redis-based leader election, distributed locks with Redisson, automatic failover

**Current Version:** 0.1.0-SNAPSHOT (production-ready)

**Test Coverage:** 23 test files with unit tests, integration tests (Testcontainers), and comprehensive test coverage

---

# **Contributing**

Although this is a personal project, see `/docs/MASTER_ROLE_SELECTOR.md` for workflow roles.
Standard GitFlow-based branching is used for clarity.

---
