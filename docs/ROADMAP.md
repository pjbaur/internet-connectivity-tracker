# **ROADMAP.md**

# **Roadmap — Internet Connectivity Tracker**

This roadmap outlines functional milestones **and** the integration of **AI-assisted workflows** used to accelerate development, documentation, testing, and DevOps maintenance.

---

# **Milestone 1 — MVP (v0.1.0)**

**Goal:** Establish core probing, basic API, and containerized runtime.

### **Application Features**

* Basic scheduler (1-second interval, configurable)
* TCP probe strategy implementation (MVP)
* REST endpoint: `/api/status`
* Dockerfile (multi-stage)
* GitHub Actions CI pipeline
* Elasticsearch 9.x index creation & mappings
* Vertical Slice architecture baseline
* Integration tests using Testcontainers

### **AI-Assisted Development Tasks**

* Generate initial project structure and slice boundaries
* Scaffold the probe slice (controller, service, strategy, repository, tests)
* Produce Dockerfile and CI pipeline templates
* Generate documentation skeleton:

    * `MASTER_ROLE_SELECTOR.md`
    * `ARCHITECTURE.md`
    * `/docs/API_SPEC.md`
    * `TEST_PLAN.md`
* Auto-generate basic Testcontainers integration setup
* Maintain code consistency via agent-based refactoring

---

# **Milestone 2 — History + Charts (v0.2.0)**

**Goal:** Add persistence and historical visibility.

### **Application Features**

* Persist probe results to Elasticsearch (`probe-results` index)
* `/api/history` endpoint for historical queries
* Uptime/latency metrics
* Basic dashboard UI (Kibana or minimal frontend)
* Round-robin multi-target probing
* Summary transformations (aggregations in ES)

### **AI-Assisted Development Tasks**

* Scaffold `history` feature slice (controller → service → repo queries → tests)
* Generate ES queries (range queries, aggregations, filters)
* Add OpenAPI annotations & documentation updates
* Create Kibana dashboard JSON via AI-assisted modeling
* Auto-update integration tests for ES read/write behavior
* Maintain architectural diagrams (Mermaid)
* Improve API documentation and examples in `/docs/API_SPEC.md`

---

# **Milestone 3 — Advanced Features (v0.3.0)**

**Goal:** Introduce observability, alerting, and configurability.

### **Application Features**

* Prometheus metrics endpoint (`/api/metrics`)
* Basic alerting (threshold-based or heuristic)
* Configurable targets (managed via API)
* Outage detection logic
* Improved target lifecycle management
* ILM / rollover indices (optional)

### **AI-Assisted Development Tasks**

* Generate Prometheus metrics instrumentation plan
* Assist in creating alerting logic and tests
* Auto-generate new feature slices (alerting, outage)
* Suggest ES index lifecycle policies and mappings
* Update Docker Compose with metrics stack components
* Auto-update documentation and roadmap entries
* Propose additional architectural refinements (Phase 3+)

---

# **Future Milestones (Post-v0.3.0)**

### **Phase 4 — ICMP + Hybrid Probing**

* ICMP probe strategy (cross-platform)
* Hybrid strategy fallback chain (TCP → ICMP)
* Enhanced error classification
* Additional performance tuning

**AI-Assisted Tasks**

* Help generate ICMP system-call logic or OS-agnostic abstractions
* Update strategies and ProbeService integrations
* Expand testing matrix

---

### **Phase 5 — UI Enhancements**

* Lightweight frontend in React/Vite (optional)
* Real-time data visualization
* More robust dashboards

**AI-Assisted Tasks**

* Produce initial UI wireframes
* Scaffold frontend components
* Integrate with backend APIs

---

### **Phase 6 — Reliability & Scaling**

* Distributed probing
* Multi-node support
* Advanced anomaly detection
* Cloud deployment examples (ECS, GKE, etc.)

**AI-Assisted Tasks**

* Generate cloud deployment templates
* Propose scaling strategies
* Improve observability stack (Grafana, Loki, Prometheus)

---

# **AI-Assisted Development Principles Across All Milestones**

AI support will follow these rules:

1. **Human-led decisions, AI-accelerated implementation**
2. **AI may modify multiple files but must respect vertical-slice boundaries**
3. **All generated code must pass tests and be idiomatic**
4. **Documentation must always stay synchronized**
5. **AI does not alter runtime behavior autonomously**

---

# **Summary**

This roadmap combines traditional milestones with modern AI-assisted workflows to accelerate development while maintaining architectural clarity, test coverage, and high-quality documentation. AI acts as a **development accelerator**, not a runtime component, and is responsible for scaffolding, refactoring, documentation maintenance, and DevOps automation throughout the project.

---
