# **Internet Connectivity Tracker**

A Java 21 + Spring Boot application that monitors internet connectivity using OS-agnostic ICMP strategies and scheduled checks. Includes Docker support, GitHub Actions CI, structured documentation, and optional metrics for observability.

---

## **Features**

* OS-agnostic connectivity checks
* Spring Boot scheduled tasks
* REST endpoints for status and history
* Elasticsearch storage and querying
* Dockerized application
* GitHub Actions CI pipeline
* Optional Prometheus metrics (future milestone)

---

## **Tech Stack**

* Java 21 (Temurin)
* Spring Boot
* Elasticsearch (Java API Client 8.12.x)
* Docker (multi-stage builds)
* GitHub Actions
* JUnit 5 for testing
* IntelliJ Community Edition on macOS/Intel

---

## **Project Documentation**

Documentation lives in the `/docs` directory:

* `MASTER_ROLE_SELECTOR.md`
* `ARCHITECTURE.md`
* `ROADMAP.md`
* `API_SPEC.md`
* `TEST_PLAN.md`

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

This project was developed using modern AI tools as part of a professional, real-world engineering workflow.
AI was used to:

* accelerate boilerplate generation
* propose architectural alternatives
* assist with test case design
* draft documentation
* support structured role-based development (PO, Architect, DevOps, QA, etc.)

All final design decisions, debugging, business logic, and code validation were performed manually.

AI served as a **force multiplier**, not a substitute for engineering ability.
This reflects the way modern engineering teams use AI today — as a productivity tool that enhances clarity, speed, and structure while preserving human ownership of technical outcomes.

---

# **Running the Application (Docker)**

```
docker build -t connectivity-tracker .
docker run -p 8080:8080 connectivity-tracker
```

---

# **Contributing**

Although this is a personal project, see `/docs/MASTER_ROLE_SELECTOR.md` for workflow roles.
Standard GitFlow-based branching is used for clarity.

---
