# **TEST_PLAN.md**

# **Test Plan — Internet Connectivity Tracker**

This document defines the complete testing strategy and implementation plan for the Internet Connectivity Tracker.
Tests are written using **JUnit 5**, **AssertJ**, **Mockito**, and **Testcontainers** for Elasticsearch integration.

AI-assisted development is used to **generate scaffolding**, **expand coverage**, and **maintain consistency**, but all tests remain human-reviewed.

---

# **1. Test Categories**

The test suite is divided into:

1. **Functional Tests**
2. **Negative Tests**
3. **Integration Tests**
4. **System Tests**
5. **AI-Generated Test Scaffolding (New)**

Each category is described below.

---

# **2. Functional Tests**

Functional tests validate expected behavior of individual components under normal conditions.

### **Probe Functionality**

* TCP (MVP) or ICMP (Phase 2) checks return expected success/failure results
* Latency is measured correctly
* ProbeResult fields are populated consistently
* Round-robin target selection cycles correctly

### **Scheduler**

* Scheduler executes at configured interval
* Scheduler triggers ProbeService with correct target

### **REST Endpoints**

* `/api/status` returns latest result
* `/api/history` returns results within range
* `/api/targets` returns configured target list
* JSON serialization and DTO mapping are correct

---

# **3. Negative Tests**

Negative tests validate system behavior under error conditions.

### **Network Conditions**

* Target unreachable (connection refused)
* Timeout exceeded
* Host lookup failure (DNS error)
* Connection dropped mid-probe

### **Application-Level Conditions**

* Null/invalid target
* Invalid port values
* Empty target lists
* Elasticsearch write failure (repository exception path)
* Scheduler handling when no targets are available

### **Error Logging**

* Failures produce correct log messages
* ProbeResult includes errorMessage when applicable

---

# **4. Integration Tests**

Integration tests validate the interplay of multiple components within the Spring Boot context.

### **Controller → Service**

* `/api/status` invokes ProbeService correctly
* `/api/history` performs ES queries using repository layer
* Target management endpoints behave correctly

### **Service → Repository**

* ProbeService writes results to Elasticsearch using repository implementation
* Repository returns the latest result
* History queries perform correct range/range+target filtering

### **Scheduler Integration**

* Simulated scheduler triggers probe execution
* Correct handoff to ProbeService
* Round-robin behavior verified without waiting real time

### **Testcontainers Elasticsearch**

Each integration test suite will use:

* A real ephemeral Elasticsearch instance
* Index auto-creation
* Insert + readback verification
* Mapping and field-type validation

---

# **5. System Tests**

System tests validate **end-to-end behavior** using the full **Docker Compose stack**:

* Spring Boot
* Elasticsearch 9.x
* Kibana

### **System Test Activities**

* Bring up the full stack under Docker
* Register multiple targets
* Allow probes to run for several minutes
* Query `/api/status` and `/api/history`
* Validate ES documents via ES API
* Validate Kibana dashboard views data correctly

These test the system **under conditions similar to production**.

---

# **6. AI-Assisted Test Scaffolding (New)**

AI tooling is used to **accelerate test generation**, ensure coverage consistency, and maintain adherence to project architecture.

AI never executes tests nor determines correctness — all generated code is **human-reviewed and validated**.

## **6.1 AI-Generated Unit Test Scaffolding**

AI may generate:

* Test class skeletons for all feature slices
* Initial Given/When/Then structure
* Required mocks for services, repositories, and strategies
* AssertJ assertions
* Test data builders / object factories

Example (Engineer role):

```
QA: Generate unit test scaffolding for IcmpProbeStrategy,
including failure paths, timeouts, and latency calculations.
```

## **6.2 AI-Generated Integration Test Scaffolding**

AI may prepare:

* Spring Boot test classes
* Testcontainers initialization boilerplate
* Index creation templates
* JSON test payloads
* MockMvc test paths
* Test methods for REST endpoints

Example:

```
QA: Create the integration test scaffolding for ProbeControllerIT,
including Testcontainers Elasticsearch and expected JSON format.
```

## **6.3 AI-Assisted Negative Test Enumeration**

AI can enumerate:

* Edge cases
* Timeout scenarios
* Network failure simulations
* Repository exception propagation
* Data validation scenarios

## **6.4 Test Maintenance via AI**

When code changes:

* AI can update affected tests
* AI can regenerate failing tests
* AI can adjust test inputs to match updated models
* AI ensures documentation and test plan stay synchronized

All changes must pass:

* `mvn verify`
* Unit tests
* Integration tests
* Static analysis

---

# **7. Test Coverage Goals**

| Layer        | Goal Coverage  | Notes                                    |
| ------------ | -------------- | ---------------------------------------- |
| Controllers  | 90%+           | Validate mappers + JSON + HTTP contracts |
| Services     | 95%+           | Primary business logic                   |
| Strategies   | 100%           | Deterministic, isolated logic            |
| Repositories | 85%+           | Integration-tested with ES               |
| Scheduler    | 80%+           | Integration-only                         |
| System Tests | Representative | Required for Dockerized behavior         |

---

# **8. Tooling and Environment**

### **Frameworks**

* JUnit 5 (unit, integration, and system tests)
* AssertJ (assertions)
* Mockito (mocking)
* Testcontainers (Elasticsearch)

### **Execution**

* `mvn test` → unit tests
* `mvn verify` → full suite (including Testcontainers)
* GitHub Actions executes full matrix on PRs

---

# **9. Summary**

This Test Plan ensures:

* High coverage of probing behavior
* Verified correctness across vertical slices
* Real data persistence testing with Elasticsearch
* End-to-end validation via Docker Compose
* AI-assisted scaffolding to increase speed without sacrificing quality
* Human-reviewed tests aligned to industry-standard engineering practices

This combination provides strong confidence in correctness, reliability, and maintainability.

---
