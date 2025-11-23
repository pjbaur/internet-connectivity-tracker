# Kibana Dashboards

Step-by-step instructions for building a latency-over-time and availability dashboard in Kibana and exporting it as NDJSON.

## Prerequisites

- Elasticsearch, Kibana, and Logstash are running (e.g., `docker compose up` from the repo root). The compose file enables JSON logs and ships them to Logstash → Elasticsearch by default.
- Probe data is indexed into `probe-results` with fields like `timestamp`, `targetHost`, `latencyMs`, `status`, and `method`.

## Enable JSON application logs

The `json-logs` Spring profile emits structured logs and streams them to Logstash:

- `docker compose up` sets `SPRING_PROFILES_ACTIVE=json-logs` and sends logs to `logstash:5044`, which indexes them into `app-logs-*`.
- To disable JSON shipping, remove `SPRING_PROFILES_ACTIVE=json-logs` (or set another profile) in `docker-compose.yml`. Text console/file logs remain.

## Create the data view (index pattern)

1. In Kibana, open **Stack Management → Data Views**.
2. Click **Create data view**.
3. Name: `probe-results`.
4. Index pattern: `probe-results*`.
5. Timestamp field: `timestamp`.
6. Save.

## Build the visualizations (Kibana Lens)

### Latency over time (line chart)

1. Go to **Analytics → Visualize Library → Create visualization → Lens**.
2. Select the `probe-results` data view.
3. Drag `timestamp` to the horizontal axis (Date histogram).
4. Drag `latencyMs` to the vertical axis and use the **Average** aggregation.
5. (Optional) Drag `targetHost` to the **Break down by** field to split lines per target.
6. Add a filter for `status : "UP"` to exclude failures from the latency line.
7. Save as `Latency Over Time`.

### Availability over time (percentage)

1. In Lens, create another visualization with the `probe-results` data view.
2. Drag `timestamp` to the horizontal axis (Date histogram).
3. For the metric, choose **Formula** and use:
   ```
   count(kql='status : "UP"') / count() * 100
   ```
4. Set the metric format to **Percent** (0–100). A bar, area, or line visualization works well.
5. (Optional) Drag `targetHost` to **Break down by** to see availability per host.
6. Save as `Availability (%) Over Time`.

## Assemble the dashboard

1. Go to **Analytics → Dashboard → Create dashboard**.
2. Click **Add from library** and add `Latency Over Time` and `Availability (%) Over Time`.
3. Set the time range (e.g., `Last 24 hours`), resize/rearrange as needed.
4. Save the dashboard (e.g., `Connectivity Overview`).

## Export as NDJSON

1. Open **Stack Management → Saved Objects**.
2. Select the items to export: the `probe-results` data view, both visualizations, and the dashboard.
3. Click **Export** and choose NDJSON. Kibana downloads a `.ndjson` file that can be imported into another instance.

## Provided saved objects (logs)

Import `docs/kibana/ict-logs.ndjson` to get:
- `ICT Logs – Recent`: saved search filtered by `targetId`, `status`, `method`, `reqId`, and `probeCycleId` columns.
- `ICT Logs – Error Rate`: Lens visualization showing error/warn share over time.
- `ICT Logs – Dashboard`: combines the saved search and error-rate chart.
