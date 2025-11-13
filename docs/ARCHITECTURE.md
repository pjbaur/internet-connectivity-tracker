# Architecture Overview

```mermaid
flowchart TD
    Scheduler --> Service
    Service --> ICMPClient
    Service --> Repository
    API --> Service
    Repository --> Database[(Storage)]

## Components

- Scheduler – triggers periodic checks
- Service – orchestrates logic
- ICMP Client – provides cross-platform ping
- Controller – REST endpoints
- Repository – persistence layer

See API_SPEC.md for endpoint definitions.