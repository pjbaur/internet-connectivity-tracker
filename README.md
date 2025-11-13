
# **Internet Connectivity Tracker**

A Java 21 + Spring Boot application that monitors internet connectivity using OS-agnostic ICMP strategies and scheduled checks. Includes Docker support, GitHub Actions CI, structured documentation, and optional metrics for observability.

---

## **Features**

* OS-agnostic connectivity checks
* Spring Boot scheduled tasks
* REST endpoints for status and history
* Dockerized application
* GitHub Actions CI pipeline
* Optional Prometheus metrics (future milestone)

---

## **Tech Stack**

* Java 21 (Temurin)
* Spring Boot
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

Although this is a personal project, See `/docs/MASTER_ROLE_SELECTOR.md` for workflow roles.
Standard GitFlow-based branching is used for clarity.

---
