# ✅ **1. Elasticsearch Query Layer (findRecent + findBetween)**

### **Master Copilot Prompt**

> **Copilot: Implement the remaining Elasticsearch query methods in `ElasticProbeRepository`. Add `findRecent(targetId, limit)` and `findBetween(targetId, start, end)` using the ES Java API Client (9.x), including match queries, date range queries, sorting by timestamp desc, and limit/size. Follow the patterns in `save(…)`. Use the index name from `ict.elasticsearch.index`. Add JavaDoc.**

### **Micro-prompts**

* *“Write the ES query for findRecent using match + sort desc + size(limit).”*
* *“Write the ES query for findBetween with DateRangeQuery filtering timestamps.”*
* *“Refactor result parsing into a helper method to avoid duplication.”*
* *“Add logging and wrap ES exceptions in a repository-specific exception.”*

---

# ✅ **2. Integration Tests for ES Query Methods**

### **Master Copilot Prompt**

> **Copilot: In `ElasticProbeRepositoryIT`, add integration tests for `findRecent` and `findBetween`. Use the existing Testcontainers Elasticsearch container. Insert multiple documents, test ordering, test limit enforcement, and test start/end date filtering. Ensure the test creates realistic timestamps.**

### **Micro-prompts**

* *“Generate several ProbeResult instances with staggered timestamps.”*
* *“Write a test asserting findRecent returns newest-first order.”*
* *“Write a test asserting findBetween filters correctly by date range.”*
* *“Add a negative test for empty results.”*

---

# ✅ **3. Finish `/api/history` and Wire to Repository**

### **Master Copilot Prompt**

> **Copilot: Update `ProbeController` to fully implement the `/api/history` endpoint. Support params: targetId, limit, start, end. Validate parameters, use the repository methods, and return JSON DTOs. Update Swagger annotations.**

### **Micro-prompts**

* *“Improve validation and return 400 for invalid date ranges.”*
* *“Add DTOs to avoid leaking domain objects directly.”*
* *“Add `@Operation` and `@Parameter` annotations for OpenAPI.”*

---

# ✅ **4. Split `/api/status` and Add Latest-Probe Endpoint**

### **Master Copilot Prompt**

> **Copilot: Refactor `StatusController` so `/api/status` returns only a `StatusSnapshot`. Add a new endpoint `/api/probe-results/latest` that returns the latest ProbeResult. Update service methods and OpenAPI annotations accordingly.**

### **Micro-prompts**

* *“Rename service methods to reflect the new endpoint responsibilities.”*
* *“Update `API_SPEC.md` references in comments.”*
* *“Add integration test updates to `OpenApiDocsIT`.”*

---

# ✅ **5. Implement Global @RestControllerAdvice Error Handling**

### **Master Copilot Prompt**

> **Copilot: Create a new `GlobalExceptionHandler` class annotated with `@RestControllerAdvice`. Convert common exceptions (validation errors, not-found, illegal arguments) into a unified `ErrorResponse` structure. Ensure all controllers use this model.**

### **Micro-prompts**

* *“Add a handler for MethodArgumentNotValidException.”*
* *“Add a handler for ConstraintViolationException.”*
* *“Add a handler for custom NotFoundException.”*
* *“Add fallback handler for Exception.”*

---

# ✅ **6. Controller Integration Tests**

### **Master Copilot Prompt**

> **Copilot: Create integration tests for `TargetController`, `ProbeController`, `StatusController`, and `HealthController` using `@SpringBootTest` + MockMvc. Verify correct JSON, status codes, validation behavior, and happy-path flows.**

### **Micro-prompts**

* *“Write a test for creating and deleting a target.”*
* *“Write a test for history with valid timestamps.”*
* *“Write a test for the latest probe result endpoint.”*
* *“Write negative-path tests that trigger the global error handler.”*

---

# ✅ **7. Create Kibana Dashboard & NDJSON Export**

*(These are mainly documentation-driven, but Copilot can help write instructions, configs, and scripts.)*

### **Master Copilot Prompt**

> **Copilot: Generate documentation steps for creating a Kibana dashboard for latency-over-time and availability charts. Include steps for creating the index pattern, visualizations, and how to export as NDJSON. Add this to `docs/kibana/README.md`.**

### **Micro-prompts**

* *“Write a Markdown section explaining how to import the dashboard.”*
* *“Add a shell script for launching docker-compose and waiting for ES/Kibana to be ready.”*

---

# ✅ **8. Documentation Alignment**

### **Master Copilot Prompt**

> **Copilot: Update ROADMAP.md to correct the mismatch between PRD and Roadmap (TCP-only MVP). Update API_SPEC.md to reflect the new latest-probe endpoint. Update ARCHITECTURE.md, TEST_PLAN.md, and README.md to match the new API, query layer, and Kibana workflow. Keep language concise and professional.**

### **Micro-prompts**

* *“Rewrite the API_SPEC section for status + latest probe.”*
* *“Add short examples for curl usage.”*
* *“Update architecture diagrams if needed.”*

---

# ✅ **9. Final Release Prep (v0.1.0)**

### **Master Copilot Prompt**

> **Copilot: Update `pom.xml` to version `0.1.0`. Add a CHANGELOG.md entry summarizing all completed work. Create a release checklist in Markdown including build, docker-compose verification, Kibana export, docs update, and GitHub Release steps.**

### **Micro-prompts**

* *“Generate a CHANGELOG.md entry for v0.1.0.”*
* *“Write a GitHub Release description template.”*

---

# 🔮 **Phase 2 Copilot Prompts (ICMP, outages, summaries)**

### **Master Prompt**

> **Copilot: Implement the `IcmpProbeStrategy` using Java's ICMP echo via privileged execution or NIO-based Echo. Integrate it into the Strategy Pattern and add configuration to select TCP/ICMP/Hybrid. Add tests covering fallback logic in HybridProbeStrategy.**

---

# 🌐 **Phase 3 Copilot Prompts (Prometheus, alerting)**

### **Master Prompt**

> **Copilot: Add Micrometer counters, timers, and gauges for latency, probe attempts, probe failures, and target count. Expose via `/actuator/prometheus`. Add documentation and basic alert-rule examples for external monitoring.**

---

# 🖥️ **Phase 4 Copilot Prompts (UI + Cloud)**

### **Master Prompt**

> **Copilot: Scaffold a new React/Vite frontend with pages for targets, results, and latency charts (Recharts). Connect to backend API. Add Dockerfile and NGINX reverse proxy configuration for local Docker Compose deployment.**

---

# ✔️ Ready for Integration Into GitHub Issues

If you want, I can now:

* Convert every **master prompt** into a **GitHub Issue**
* Group them under Milestones (`v0.1.0`, `v0.2.x`, etc.)
* Provide **labels** for each (backend, testing, docs, ES, CI, etc.)
* Generate a **Kanban board layout**

Just tell me: **“Create GitHub Issues for all these prompts.”**
