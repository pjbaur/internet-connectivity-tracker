# Project Context for AI Assistant

## What this project does
The Internet Connectivity Tracker is a Java 21 + Spring Boot application that continuously measures internet connectivity and latency across multiple targets. It performs round-robin TCP probes every second, stores results in Elasticsearch, and provides REST APIs for status, history, and metrics. The stack is fully containerized with Docker and includes optional Kibana dashboards for visualization. It’s designed as a clean, modern, portfolio-ready backend showcasing scheduling, strategy patterns, Elasticsearch integration, and DevOps best practices.

## Tech Stack
* Java 21 (Temurin)
* Spring Boot
* Elasticsearch (Java API Client 8.12.x)
* Docker (multi-stage builds)
* GitHub Actions
* JUnit 5 for testing
* IntelliJ Community Edition on macOS/Intel

## What I want analyzed
- Review docs/ARCHITECTURE_ASSESSMENT_20251116.md and list issues that have been resolved and issues that remain.

## Known pain points
- None

