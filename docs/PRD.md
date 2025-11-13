# Product Requirements Document (PRD)

## Internet Connectivity Tracker

## 1. Overview

The Internet Connectivity Tracker is a Java 21 + Spring Boot application that continuously monitors internet connectivity and latency across multiple configurable targets. Results are stored in Elasticsearch and visualized in Kibana. The system is containerized using Docker and architected using the Vertical Slice Architecture pattern.

This project is designed for technical demonstration, portfolio presentation, and practical homelab use.

## 2. Goals & Non-Goals

### Goals

* Monitor connectivity to multiple targets.
* Store time-series probe data in Elasticsearch.
* Visualize latency and availability trends in Kibana.
* Showcase modern backend and DevOps engineering practices.
* Provide a clean API for querying data.

### Non-Goals

* Provide a full enterprise network monitoring system.
* Implement multi-tenant authentication.
* Implement advanced alerting rules in MVP.
* Build a custom frontend dashboard in MVP.

## 3. Users & Use Cases

### Users

* Homelab and power users.
* Software/network engineers.
* Recruiters reviewing engineering proficiency.

### Use Cases

* Detect intermittent or brief outages.
* Measure latency trends over time.
* Correlate network issues with ISP or environment.
* Demonstrate backend and observability skills.

## 4. Functional Requirements (MVP)

* Manage a list of probe targets.
* Probe targets using TCP-based connectivity checks only.
* Default probe interval: 1 second (configurable).
* Probes distributed round-robin across targets.
* Store probe results in a single Elasticsearch index (`probe-results`).
* Expose API endpoints for:

  * Target management
  * Probe result querying
  * Current status
  * Health and version info
* Provide Kibana dashboards for visualization.

## 5. Non-Functional Requirements

* Docker-first, OS-agnostic execution.
* Handle 1-second probe intervals reliably.
* Use Elasticsearch for time-series storage.
* Log errors and key events.
* Local trusted environment for MVP; no authentication.

## 6. Architecture Decisions

### Technology Stack

* Java 21
* Spring Boot 3.x
* Maven
* Elasticsearch (ES 9.x compatible)
* Kibana
* Docker & Docker Compose

### Architecture Style

* Vertical Slice Architecture
* Feature slices: `probe`, `target`, `outage` (Phase 2), `system`

### Probe Strategy

* MVP: `TcpProbeStrategy`
* Phase 2: `IcmpProbeStrategy` + `HybridProbeStrategy`
* Implemented using the Strategy Pattern

### Data Storage

* Elasticsearch single index for MVP
* Raw probe results + aggregated summaries
* Phase 2: ILM and rollover indices

### Target Identification

* UUID for internal identity
* Optional user-provided labels

## 7. Data Model

### ProbeResult

* `timestamp`: date/time
* `target`: string
* `latencyMs`: integer/long
* `status`: enum (UP/DOWN)
* `method`: enum (TCP, ICMP later)
* `errorMessage`: string (nullable)

### OutageEvent (Phase 2)

* `startTime`
* `endTime`
* `durationMs`
* `targetsAffected`
* `details`

## 8. Technical Decisions Summary

* Maven build tool
* JUnit + AssertJ + Mockito + Spring Boot Test
* Elasticsearch Java API Client
* Single index for MVP
* No retention policy in MVP

## 9. Release Plan

### MVP (v0.1.0)

* TCP-only probing
* Elasticsearch integration
* Target management API
* Probe result API
* Basic Kibana visualization
* Docker Compose stack

### Phase 2 (v0.2.x)

* ICMP + Hybrid probe strategies
* Outage detection and storage
* Summary aggregation
* Basic alerting
* ILM policies

### Phase 3+

* Custom UI (optional)
* Prometheus metrics endpoint
* Cloud deployment examples
