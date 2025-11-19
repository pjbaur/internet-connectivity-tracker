# **API Specification (Updated)**

## **GET /api/probe-results/latest**

Returns the **most recent probe result** recorded by the system.
This endpoint replaces the former `/api/status`, which was ambiguous.

### Response (200 OK)

```json
{
  "timestamp": "2025-11-16T12:34:56Z",
  "target": "8.8.8.8",
  "latencyMs": 42,
  "status": "UP",
  "method": "TCP",
  "errorMessage": null
}
```

---

## **GET /api/probe-results**

Returns historical probe results with optional filtering.

### Query Parameters

| Param    | Type             | Description                              |
| -------- | ---------------- | ---------------------------------------- |
| `target` | string           | Optional target filter                   |
| `limit`  | int              | Max results to return                    |
| `since`  | ISO8601 datetime | Return results newer than this timestamp |
| `until`  | ISO8601 datetime | Return results older than this timestamp |

---

## **GET /api/targets**

Returns the list of configured probe targets.

### Response Example

```json
[
  {
    "id": "uuid-1234",
    "host": "8.8.8.8",
    "port": 53,
    "label": "Google DNS"
  }
]
```

---

## **POST /api/targets**

Creates a new probe target.

### Request Body

```json
{
  "host": "1.1.1.1",
  "port": 53,
  "label": "Cloudflare DNS"
}
```

---

## **DELETE /api/targets/{id}**

Deletes a target by ID.

---

## **GET /api/history**

(Deprecated for MVP)
Will be replaced by parameterized `/api/probe-results` queries.

---

## **GET /api/system/status**

Returns internal system status such as scheduler health, probe queue state, and ES connectivity.
This prevents conflict with the now-renamed probe endpoint.

---

## **GET /api/metrics**

Optional Prometheus metrics endpoint (Phase 2+).
Exposed only when metrics are enabled.

---

## OpenAPI / Swagger

The canonical API contract is generated at runtime by springdoc-openapi. Use the following endpoints as the source of truth:

- OpenAPI JSON: `/v3/api-docs`
- Swagger UI: `/swagger-ui.html`

The main API groups (tags) and representative endpoints include:

- Status
  - GET `/api/status` (or `/api/probe-results/latest`) — latest probe result
- History
  - GET `/api/history` (deprecated) or GET `/api/probe-results` — historical probe results with filters
- Targets
  - GET `/api/targets` — list targets
  - POST `/api/targets` — create target
  - DELETE `/api/targets/{id}` — delete target
- System
  - GET `/api/system/status` — internal system health and details
- Metrics (optional)
  - GET `/api/metrics` — Prometheus metrics (text/plain) when enabled
