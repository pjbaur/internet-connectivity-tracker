> Engineer: Are all of the Controllers Open-API compliant? If no, create a detailed plan to modify them so that they are Open-API compliant (but don't make any code changes).

Engineer: **Short answer — No, not all controllers are currently OpenAPI-compliant.**

Your existing controllers (Status, History, Probe/Target, Metrics) are only *implicitly* documented in `/docs/API_SPEC.md`, and they lack:

* `@Operation` metadata
* `@ApiResponse` definitions
* Request/response schema annotations
* Example payloads
* Media-type declarations (`application/json`)
* Proper HTTP status documentation
* Tagging for grouping endpoints
* Global OpenAPI configuration (title, version, etc.)

Below is a **detailed, actionable plan** to make the entire API 100% OpenAPI-compliant using **springdoc-openapi** without making code changes right now.

---

# ✅ **Detailed Plan to Make All Controllers Fully OpenAPI-Compliant**

This plan covers:

1. Add springdoc dependencies
2. Add global OpenAPI configuration
3. Apply annotations to controllers
4. Standardize DTOs
5. Provide examples
6. Handle errors using a uniform schema
7. Add integration tests for OpenAPI links
8. Update documentation files

---

# 1. **Add SpringDoc Dependencies (pom.xml)**

Use:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
</dependency>
```

This gives you:

* `/v3/api-docs` (JSON)
* `/swagger-ui.html` (interactive UI)

---

# 2. **Add a Global OpenAPI Config Class**

This ensures a clean API title, version, contact info, and servers list.

Plan:

* Create `OpenApiConfig.java` under `system` slice
* Use `@OpenAPIDefinition`
* Add:

    * Title
    * Version
    * Description
    * Contact
    * Server URLs

---

# 3. **Annotate Each Controller**

For each controller:

## A. Add these annotations at the class level:

* `@Tag(name = "Probe", description = "Probe operations")`
* Or: "Targets", "System", "Status", etc.

## B. Add these annotations for each method:

* `@Operation(summary = "...", description = "...")`

* `@ApiResponses({...})` containing:

    * `200` success response
    * `400` validation error
    * `404` not found
    * `500` internal error

* `@Parameter` annotations for:

    * Path variables
    * Query parameters
    * Optional filters

* Explicit `@RequestBody` metadata (where applicable)

## C. Add media types:

```java
@Produces("application/json")
@Consumes("application/json")
```

(Or Spring’s `@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)`)

## D. Add examples (very important for employers):

Springdoc supports:

```java
@ExampleObject(name = "Successful probe result", value = "{...json...}")
```

Every major endpoint should have at least one example response.

---

# 4. **Ensure All DTOs Are Annotated With Schema Metadata**

For DTOs (e.g., ProbeResult, ProbeRequest, Target):

Use:

* `@Schema(description = "...")`
* `@Schema(example = "123")`
* `@Schema(implementation = ProbeResult.class)`

This ensures:

* Correct schema generation
* Nullable properties documented
* Enums represented properly
* Timestamps documented as ISO-8601

---

# 5. **Add an Error Response Standard**

Define a reusable error model:

### `ErrorResponse`

```java
@Schema(name = "ErrorResponse", description = "Standard error response")
public record ErrorResponse(
    @Schema(description = "Error message") String message,
    @Schema(description = "Error code") String code,
    @Schema(description = "When the error occurred") Instant timestamp
) {}
```

Then use:

```java
@ApiResponse(responseCode = "400", description = "Bad request",
             content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
```

---

# 6. **Review and Update Each Controller Slice**

Below is the plan by slice.

---

## **A. Status Controller**

**Endpoints:**

* `/api/status`
* (New renamed endpoint per your architecture: `/api/latest-probe-result`)

Plan:

* Add summary, description, examples
* Document returned model (ProbeResult)
* Add `404` if no data exists (historical caveat)
* Tag: `@Tag(name = "Status")`

---

## **B. Probe Results / History Controller**

**Endpoints:**

* `/api/history`
* Possibly `/api/history?from=...&to=...`

Plan:

* Add query parameter metadata
* Add paging metadata (if added later)
* Add multiple example responses
* Tag: `@Tag(name = "History")`

---

## **C. Target Controller (Create/List/Delete)**

Even if you haven’t created these yet, plan for:

* `POST /api/targets`
* `GET /api/targets`
* `DELETE /api/targets/{id}`

Plan:

* Define request schema (`CreateTargetRequest`)
* Define response schema (`TargetDto`)
* Add 400, 404, 409 error schemas
* Tag: `@Tag(name = "Targets")`

---

## **D. Metrics Controller (Prometheus)**

Even though Prometheus is a later phase:

* Document `/api/metrics`
* Add a note that content type is `text/plain`

Tag: `@Tag(name = "Metrics")`

---

## **E. System Controller (Health, Version)**

* `/api/health`
* `/api/version`

Plan:

* Document simple JSON outputs
* Add example payloads
* Tag: `@Tag(name = "System")`

---

# 7. **Integration Tests for OpenAPI Contract**

Add tests that:

* Hit `/v3/api-docs`
* Validate JSON schema presence
* Validate all tags appear
* Validate all endpoints appear
* (Optional) Export OpenAPI spec as a file in CI

Example IT plan:

1. Test: `/v3/api-docs` returns 200
2. Test: Response contains `"paths"`
3. Test: Response contains `/api/status` etc
4. Test: At least one tag exists (`"tags":[...]`)

---

# 8. **Update Documentation**

Update:

### `/docs/API_SPEC.md`

Replace current minimal list with:

* Auto-generated OpenAPI spec URL
* Full endpoint list
* Summary of example payloads
* Link to Swagger UI

### `/docs/ARCHITECTURE.md`

Add note:

> All controllers follow OpenAPI standards using springdoc-openapi.
> The API is self-documenting at `/swagger-ui.html` and `/v3/api-docs`.

---

# 9. **Final Deliverable Checklist**

Below is the exact developer checklist you can paste into your TODOs.

---

# ✅ **OpenAPI Compliance Checklist (Engineer)**

### Global

* [ ] Add springdoc-openapi dependency
* [ ] Add OpenApiConfig
* [ ] Verify `/swagger-ui.html` functions

### Controllers

* [ ] Add `@Tag` to all Controllers
* [ ] Add `@Operation` to all endpoints
* [ ] Add `@ApiResponses`
* [ ] Add request/response examples
* [ ] Add media type declarations
* [ ] Add query/path param metadata

### DTOs

* [ ] Add `@Schema` annotations
* [ ] Ensure enums have `@Schema(description=...)`
* [ ] Document timestamp formats

### Errors

* [ ] Create ErrorResponse record
* [ ] Document errors in all endpoints

### Integration Tests

* [ ] Add `/v3/api-docs` contract test
* [ ] Verify all paths appear
* [ ] Verify all tags appear

### Documentation

* [ ] Update `/docs/API_SPEC.md`
* [ ] Add OpenAPI endpoint links
* [ ] Document Swagger UI

---
