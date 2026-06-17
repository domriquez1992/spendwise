# Observability stack

Project 10 adds the three pillars of observability to Spendwise: **metrics**, **distributed
tracing**, and **structured logs** — all wired so they light up locally with one command.

## Run it

```bash
docker compose -f docker-compose.yml -f docker-compose.observability.yml up --build
```

Then exercise the API (register, log in, create a few expenses) and open:

| Tool       | URL                     | What you see                                            |
|------------|-------------------------|---------------------------------------------------------|
| Grafana    | http://localhost:3000   | The provisioned **Spendwise — Overview** dashboard      |
| Prometheus | http://localhost:9090   | Raw metrics + targets (the app should be `UP`)          |
| Tempo      | http://localhost:3200   | Trace store (queried through Grafana's Explore view)    |

Grafana opens with anonymous admin access; its Prometheus and Tempo data sources and the overview
dashboard are all provisioned from this folder, so nothing needs importing.

## The three pillars

**Metrics — Prometheus scrape.** The app exposes Micrometer metrics at `/actuator/prometheus`
(`micrometer-registry-prometheus`). Prometheus scrapes it every 15s. Alongside the built-in JVM,
HTTP, datasource, and cache metrics, a custom domain meter `spendwise_expenses_created_total`
(labelled by `category`) is incremented by an `@TransactionalEventListener(AFTER_COMMIT)` on
`ExpenseCreatedEvent`, so it counts only committed creations and needs no changes to the service.

**Tracing — OpenTelemetry → Tempo.** `spring-boot-starter-opentelemetry` bridges Micrometer
Observation to OpenTelemetry and exports spans over OTLP to Tempo (`http://tempo:4318/v1/traces`).
A request is traced across the HTTP → service → Kafka publish hop. Trace export is enabled only by
setting `management.opentelemetry.tracing.export.otlp.endpoint`, which this overlay does — so the
base stack and CI never reach for a collector.

**Logs — structured ECS JSON.** This overlay sets `LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs`, so the
app emits Elastic Common Schema JSON. Because Micrometer Tracing puts `traceId`/`spanId` into the
logging MDC, every log line is automatically trace-correlated — ready to ship to Loki or an ELK
stack (a documented next step; not included here to keep the stack lean).

## Notes

- The observability services are pinned, lightweight, and **local-only** — they are never started
  in CI. CI only validates that this overlay parses and merges (`docker compose ... config -q`).
- Metrics use a single pipeline (Prometheus scrape); the OTLP metrics registry that ships with the
  OpenTelemetry starter is disabled (`management.otlp.metrics.export.enabled: false`).
- `/actuator/prometheus`, `/actuator/health`, and `/actuator/info` are unauthenticated for scraping
  and probes. In production these management endpoints would be bound to a separate, network-
  restricted port rather than the public application port.
- Tempo runs with ephemeral storage (no volume mounted over `/var/tempo`, which the image owns as
  uid 10001), so traces live for the session — appropriate for a demo.
