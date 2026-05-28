# Monitoring & Observability

> **Production URL:** https://portal.sofumarcommunityofmn.org
> 
> **Infrastructure:** Google Cloud Run (Gen2) · us-central1

---

## Table of Contents

- [Observability Stack](#observability-stack)
- [Actuator Endpoint Reference](#actuator-endpoint-reference)
- [Health Check Architecture](#health-check-architecture)
- [Distributed Tracing](#distributed-tracing)
- [Structured Logging](#structured-logging)
- [Cloud Run Built-in Metrics](#cloud-run-built-in-metrics)
- [Prometheus Metrics (Local Development)](#prometheus-metrics-local-development)
- [Alerting Strategy](#alerting-strategy)
- [SLOs & SLIs](#slos--slis)
- [Dashboards](#dashboards)
- [Runbook References](#runbook-references)
- [Troubleshooting Quick Reference](#troubleshooting-quick-reference)

---

## Observability Stack

Carizmi's observability is built on the **three pillars** — metrics, traces, and logs — using Spring Boot's native integrations with Google Cloud's managed observability services.

| Pillar | Technology | Data Flow | Environment |
|--------|-----------|-----------|-------------|
| **Metrics** | Spring Boot Actuator + Micrometer | Actuator → Cloud Monitoring (auto) | Production |
| **Traces** | Micrometer Tracing → OpenTelemetry | OTLP → Google Cloud Trace | Production |
| **Logs** | Logstash JSON Encoder (Logback) | stdout → Google Cloud Logging (auto) | Production |
| **Health** | Spring Boot Actuator Health Probes | HTTP → Cloud Run Scheduler | All |
| **Resilience** | Resilience4j Circuit Breaker | Metrics → Micrometer, State → Actuator Health | All |

### Architecture Diagram

```
┌────────────────────────────────────────────────────────────────┐
│                    Carizmi Backend (Cloud Run)                 │
│                                                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │  Application │  │  Micrometer  │  │  Logstash Encoder    │  │
│  │    Code      │──│  Tracing     │  │  (logback-spring.xml)│  │
│  └──────────────┘  │  Bridge      │  └────────┬─────────────┘  │
│                    └──────┬───────┘           │                │
│                           │                   │  JSON to stdout│
│  ┌──────────────┐  ┌──────┴───────┐           │                │
│  │  Actuator    │  │ OpenTelemetry│           │                │
│  │  /health     │  │ OTLP Export  │           │                │
│  │  /metrics    │  └──────┬───────┘           │                │
│  │  /info       │         │                   │                │
│  └──────┬───────┘         │                   │                │
└─────────┼─────────────────┼───────────────────┼────────────────┘
          │                 │                   │
          ▼                 ▼                   ▼
  ┌───────────────┐  ┌─────────────┐   ┌────────────────┐
  │ Cloud Run     │  │ Google      │   │ Google Cloud   │
  │ Health Probes │  │ Cloud Trace │   │ Logging        │
  └───────────────┘  └─────────────┘   └────────────────┘
          │                  │                   │
          └──────────────────┼───────────────────┘
                             ▼
                    ┌────────────────┐
                    │ Google Cloud   │
                    │ Monitoring     │
                    │ (Dashboards &  │
                    │  Alerting)     │
                    └────────────────┘
```

---

## Actuator Endpoint Reference

Spring Boot Actuator exposes operational endpoints under the `/api/actuator/` path. Access control is enforced via Spring Security in [`SecurityConfig.java`](../backend/src/main/java/io/carizmi/infrastructure/security/SecurityConfig.java).

### Exposed Endpoints

| Endpoint | Path | Profiles | Access | Description |
|----------|------|----------|--------|-------------|
| **Health** | `/api/actuator/health` | All | 🔓 Public | Aggregate health status (details hidden from unauthenticated users) |
| **Liveness** | `/api/actuator/health/liveness` | All | 🔓 Public | Kubernetes-style liveness probe — is the JVM alive? |
| **Readiness** | `/api/actuator/health/readiness` | All | 🔓 Public | Kubernetes-style readiness probe — are dependencies healthy? |
| **Info** | `/api/actuator/info` | All | 🔓 Public | Application metadata |
| **Metrics** | `/api/actuator/metrics` | All | 🔒 Authenticated | Micrometer metrics index |
| **Metrics (named)** | `/api/actuator/metrics/{name}` | All | 🔒 Authenticated | Individual metric detail (e.g., `jvm.memory.used`) |
| **Prometheus** | `/api/actuator/prometheus` | `prod` | 🔒 Authenticated | Prometheus-format metrics scrape endpoint |

### Security Model

```
AUTHORIZED_PATHS (no JWT required):
  /auth/login, /auth/refresh, /actuator/health

All other actuator endpoints require a valid JWT token.
Endpoints not in the 'include' list are disabled entirely.
```

### Intentionally Disabled Endpoints

| Endpoint | Reason |
|----------|--------|
| `shutdown` | Explicitly disabled — security risk |
| `env` | Not exposed — leaks environment variables and secrets |
| `heapdump` | Not exposed — can leak credentials from JVM memory |
| `beans`, `configprops` | Not exposed — infrastructure information leak |

---

## Health Check Architecture

Carizmi uses Spring Boot Actuator's health probe groups to provide **separate liveness and readiness signals** to Cloud Run's container scheduler.

### Probe Groups

| Probe | Path | What It Checks | Failure Action |
|-------|------|---------------|----------------|
| **Liveness** | `/api/actuator/health/liveness` | JVM is alive (`livenessState`, `ping`) | Cloud Run **restarts** the container |
| **Readiness** | `/api/actuator/health/readiness` | Critical dependencies (`readinessState`, `db`, `redis`) | Cloud Run **stops routing traffic** (no restart) |
| **Aggregate** | `/api/actuator/health` | All indicators combined | Uptime monitoring / dashboards |

> **Design rationale:** Liveness probes must be minimal. If a liveness probe checks the database and the database is temporarily down, Cloud Run will restart the container — which won't fix a database outage and can cause cascading failures. Readiness probes safely include external dependencies because failure only stops traffic routing until the dependency recovers.

### Auto-Registered Health Indicators

These are automatically registered by Spring Boot based on detected dependencies:

| Indicator | Source | Included In |
|-----------|--------|-------------|
| `db` | `spring-boot-starter-data-jpa` (HikariCP → MySQL) | Readiness |
| `redis` | `spring-boot-starter-data-redis` | Readiness |
| `diskSpace` | Spring Boot default | Aggregate only |
| `ping` | Spring Boot default | Liveness |
| `circuitBreakers` | Resilience4j (`register-health-indicator: true`) | Aggregate only |

The `outboxRelay` circuit breaker is registered as a health indicator. When the circuit opens (failure rate > 50%), it reports `DOWN` in the aggregate health — useful for dashboards but intentionally excluded from liveness/readiness to avoid unnecessary restarts.

### Configuration

**Production** ([`application-prod.yml`](../backend/src/main/resources/application-prod.yml)):

```yaml
management:
  endpoint:
    health:
      show-details: when-authorized   # Full details only for authenticated users
      probes:
        enabled: true                 # Enables /health/liveness and /health/readiness
      group:
        liveness:
          include: livenessState, ping
        readiness:
          include: readinessState, db, redis
```

### Health Response Examples

**Public (unauthenticated):**
```json
{
  "status": "UP"
}
```

**Authenticated (with JWT):**
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP", "details": { "database": "MySQL", "validationQuery": "isValid()" } },
    "redis": { "status": "UP" },
    "circuitBreakers": { "status": "UP", "details": { "outboxRelay": "CLOSED" } },
    "diskSpace": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

---

## Distributed Tracing

Carizmi uses **OpenTelemetry** for distributed tracing in production, exported to **Google Cloud Trace** via the OTLP protocol.

### How It Works

```
Incoming Request
    │
    ▼
Spring Boot Auto-Instrumentation (Micrometer Tracing)
    │  Automatically creates spans for:
    │  • HTTP requests (server + client)
    │  • JDBC queries
    │  • Redis commands
    │  • @Async methods
    │
    ▼
Micrometer Tracing Bridge → OpenTelemetry SDK
    │
    ▼
OTLP Exporter → https://cloudtrace.googleapis.com
    │
    ▼
Google Cloud Trace (GCP Console)
```

### Trace Propagation

| Header | Standard | Behavior |
|--------|----------|----------|
| `traceparent` / `tracestate` | W3C Trace Context | Default — propagated automatically on outbound HTTP calls |
| `X-Cloud-Trace-Context` | Google Cloud | Automatically injected by Cloud Run on incoming requests |
| `X-Request-Id` | Custom | Set by [`RequestIdFilter`](../backend/src/main/java/io/carizmi/infrastructure/filter/RequestIdFilter.java) — accepts from client or generates UUID |

### Sampling Strategy

```yaml
management:
  tracing:
    sampling:
      probability: 0.05    # 5% of requests are traced
```

| Environment | Sampling Rate | Rationale |
|-------------|--------------|-----------|
| **Production** | 5% (`0.05`) | Cost-efficient for Cloud Trace billing; sufficient for pattern analysis |
| **Development** | 100% (`1.0`) | Full visibility during local debugging (if tracing is configured) |

### Viewing Traces

1. Open **Google Cloud Console** → **Trace** → **Trace Explorer**
2. Filter by service: `carizmi-backend`
3. Filter by latency to find slow requests
4. Click a trace to see the full span waterfall (HTTP → SQL → Redis)

### Log Correlation

Every trace automatically embeds `traceId` and `spanId` into structured logs via MDC (see [Structured Logging](#structured-logging)). In Cloud Trace, clicking a trace shows correlated log entries from Cloud Logging.

### Dependencies

| Dependency | Purpose |
|-----------|---------|
| `micrometer-tracing-bridge-otel` | Bridges Micrometer's tracing API to the OpenTelemetry SDK |
| `opentelemetry-exporter-otlp` | Exports trace spans via OTLP protocol to Cloud Trace |

---

## Structured Logging

Production uses **JSON-structured logging** via the Logstash Logback Encoder. Cloud Run automatically ingests stdout as structured log entries in Google Cloud Logging — no logging agent or sidecar required.

### Configuration

Logging is profile-based, configured in [`logback-spring.xml`](../backend/src/main/resources/logback-spring.xml):

| Profile | Format | Appender | Use Case |
|---------|--------|----------|----------|
| `!prod` (dev, test) | Human-readable console | Spring Boot default `CONSOLE` | Local development |
| `prod` | JSON (LogstashEncoder) | `JSON_CONSOLE` → stdout | Cloud Logging ingestion |

### Production JSON Log Fields

Every log entry in production contains:

| Field | Source | Example |
|-------|--------|---------|
| `@timestamp` | LogstashEncoder | `2026-05-27T04:15:32.123Z` |
| `level` | SLF4J | `INFO`, `WARN`, `ERROR` |
| `logger_name` | SLF4J (shortened to 36 chars) | `i.c.domain.identity.service.AuthSvc` |
| `message` | Application code | `User login successful` |
| `traceId` | MDC (Micrometer → OTel) | `a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4` |
| `spanId` | MDC (Micrometer → OTel) | `1a2b3c4d5e6f7a8b` |
| `requestId` | MDC (`RequestIdFilter`) | `550e8400-e29b-41d4-a716-446655440000` |

### Log Level Configuration (Production)

```yaml
logging:
  level:
    root: INFO
    org.springframework: INFO
    org.hibernate: WARN
    io.carizmi: INFO
    com.zaxxer.hikari: INFO
```

### Querying Logs in Cloud Console

1. Open **Google Cloud Console** → **Logging** → **Logs Explorer**
2. Select resource: **Cloud Run Revision** → `carizmi-backend`
3. Useful queries:

```
# All errors
severity="ERROR"

# Errors for a specific trace
jsonPayload.traceId="a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4"

# Slow request correlation (find logs for a requestId)
jsonPayload.requestId="550e8400-e29b-41d4-a716-446655440000"

# All logs from a specific domain
jsonPayload.logger_name=~"io.carizmi.domain.finance"

# Authentication failures
jsonPayload.message=~"login failed" OR jsonPayload.message=~"Authentication"
severity>="WARN"
```

### What NOT to Log

| Category | Examples | Why |
|----------|---------|-----|
| **PII** | Email addresses, full names, phone numbers | GDPR / privacy compliance |
| **Credentials** | Passwords, JWT tokens, API keys | Security — logs may be accessible by ops teams |
| **Request bodies** | Full JSON payloads | May contain PII; high volume |
| **Database connection strings** | JDBC URLs with credentials | Security |

---

## Cloud Run Built-in Metrics

Cloud Run automatically collects the following metrics with **zero configuration**. These are available in Google Cloud Monitoring without any code changes or exporters.

| Metric | GCP Identifier | Description | Suggested Alert Threshold |
|--------|---------------|-------------|---------------------------|
| **Request Latency** | `run.googleapis.com/request_latencies` | p50, p95, p99 response times | p95 > 500ms |
| **Request Count** | `run.googleapis.com/request_count` | Total requests by response code | Sudden drop to 0 |
| **Instance Count** | `run.googleapis.com/container/instance_count` | Active container instances | At max limit |
| **CPU Utilization** | `run.googleapis.com/container/cpu/utilizations` | Container CPU usage (0–1) | > 0.80 sustained |
| **Memory Utilization** | `run.googleapis.com/container/memory/utilizations` | Container memory usage (0–1) | > 0.85 sustained |
| **Startup Latency** | `run.googleapis.com/container/startup_latencies` | Cold start time | > 10s |
| **Billable Time** | `run.googleapis.com/container/billable_instance_time` | Billing-relevant runtime | Cost monitoring |

### Viewing Cloud Run Metrics

1. **Google Cloud Console** → **Cloud Run** → **carizmi-backend** → **Metrics** tab
2. Or: **Cloud Monitoring** → **Metrics Explorer** → filter resource type `cloud_run_revision`

---

## Prometheus Metrics (Local Development)

The `/actuator/prometheus` endpoint is available in the `prod` profile for local Prometheus scraping during development and testing. In production, Google Cloud Monitoring is the primary metrics backend (see [Cloud Run Built-in Metrics](#cloud-run-built-in-metrics)).

### Local Prometheus Setup

To run a local Prometheus instance for development:

**1. Create `prometheus.yml`:**

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'carizmi-backend'
    metrics_path: '/api/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8080']
```

**2. Run Prometheus via Docker:**

```bash
docker run -d --name prometheus \
  -p 9090:9090 \
  -v $(pwd)/prometheus.yml:/etc/prometheus/prometheus.yml \
  prom/prometheus
```

**3. Access Prometheus UI:** http://localhost:9090

### Key Metrics Available via Prometheus

| Metric | Type | Description |
|--------|------|-------------|
| `http_server_requests_seconds` | Histogram | HTTP request duration by URI, method, status |
| `jvm_memory_used_bytes` | Gauge | JVM memory usage by area (heap, non-heap) |
| `jvm_threads_live_threads` | Gauge | Current live thread count |
| `hikaricp_connections_active` | Gauge | Active database connections |
| `hikaricp_connections_idle` | Gauge | Idle database connections |
| `resilience4j_circuitbreaker_state` | Gauge | Circuit breaker state (0=closed, 1=open, 2=half-open) |
| `spring_data_repository_invocations_seconds` | Histogram | Spring Data JPA query durations |

### Optional: Grafana Dashboard

Import **Grafana Dashboard ID `4701`** (Spring Boot Statistics) for a pre-built visualization of JVM, HTTP, and HikariCP metrics.

---

## Alerting Strategy

### Severity Levels

| Severity | Description | Response Time | Notification |
|----------|-------------|---------------|--------------|
| **SEV-1** | Service unreachable, data loss risk | Immediate | Page / phone call |
| **SEV-2** | Core feature degraded, high error rate | 15 minutes | Urgent notification |
| **SEV-3** | Performance degradation, non-critical errors | Business hours | Email / Slack |
| **SEV-4** | Informational — capacity trends | Weekly review | Dashboard only |

### Recommended Alert Policies

These can be configured in **Google Cloud Monitoring** → **Alerting** → **Create Policy**:

| Alert | Metric | Condition | Severity | Runbook |
|-------|--------|-----------|----------|---------|
| **Service Down** | Request count | = 0 for > 3 min | SEV-1 | [Service Down](#runbook-service-down) |
| **High Error Rate** | 5xx response ratio | > 5% for > 5 min | SEV-2 | [High Error Rate](#runbook-high-error-rate) |
| **High Latency** | Request latency p95 | > 500ms for > 5 min | SEV-2 | [High Latency](#runbook-high-latency) |
| **High CPU** | CPU utilization | > 80% for > 10 min | SEV-3 | [High CPU](#runbook-high-cpu) |
| **High Memory** | Memory utilization | > 85% for > 10 min | SEV-3 | [High Memory](#runbook-high-memory) |
| **Cold Start Spike** | Startup latency | > 10s | SEV-3 | Review instance scaling |
| **Circuit Breaker Open** | Health check details | outboxRelay = OPEN | SEV-3 | [Circuit Breaker Open](#runbook-circuit-breaker-open) |

### Notification Channels

| Channel | Use Case |
|---------|----------|
| **Email** | All severities — audit trail |
| **Slack / Discord** | SEV-1 through SEV-3 — real-time awareness |
| **PagerDuty / Opsgenie** | SEV-1 only — on-call paging (when team scales) |

> **Best Practice:** Every alert must be actionable. If an alert fires and the response is "ignore it," the alert should be removed or downgraded to dashboard-only (SEV-4). Review and prune alerts quarterly.

---

## SLOs & SLIs

Service Level Objectives based on the [four golden signals](https://sre.google/sre-book/monitoring-distributed-systems/) (Google SRE).

### Service Level Indicators

| SLI | Definition | Measurement |
|-----|-----------|-------------|
| **Availability** | Proportion of successful requests | `non-5xx requests / total requests` |
| **Latency (p95)** | 95th percentile response time | `run.googleapis.com/request_latencies` |
| **Latency (p99)** | 99th percentile response time | `run.googleapis.com/request_latencies` |
| **Error Rate** | Proportion of server errors | `5xx responses / total responses` |

### Service Level Objectives

| SLI | SLO Target | Time Window | Error Budget |
|-----|-----------|-------------|--------------|
| Availability | ≥ 99.9% | 28-day rolling | ~43 min downtime/month |
| Latency (p95) | < 500ms | 28-day rolling | — |
| Latency (p99) | < 1000ms | 28-day rolling | — |
| Error Rate | < 0.1% | 28-day rolling | ~2,880 errors per 2.88M requests |

### Error Budget Policy

| Budget Consumed | Action |
|----------------|--------|
| < 50% | Normal development velocity |
| 50–75% | Prioritize reliability work in sprint planning |
| 75–100% | Shift focus to stability; reduce risky deployments |
| 100% (budget exhausted) | Freeze feature releases; focus entirely on reliability |

> **Note:** These SLOs are starting targets for the current deployment scale. Adjust as traffic patterns and user expectations evolve.

---

## Dashboards

### Google Cloud Console Dashboards

| Dashboard | URL Pattern | What It Shows |
|-----------|------------|---------------|
| **Cloud Run Service** | Console → Cloud Run → `carizmi-backend` → Metrics | Request count, latency, errors, instance count, CPU, memory |
| **Cloud Trace Explorer** | Console → Trace → Trace Explorer | Distributed trace waterfall, latency distribution |
| **Cloud Logging Explorer** | Console → Logging → Logs Explorer | Structured log search, error analysis |
| **Cloud Monitoring** | Console → Monitoring → Dashboards | Custom dashboards, alert history |

### Recommended Custom Dashboard Panels

Create a custom dashboard in Cloud Monitoring with these panels:

1. **Request Rate** — `run.googleapis.com/request_count` grouped by `response_code_class`
2. **Latency Distribution** — `run.googleapis.com/request_latencies` as heatmap (p50, p95, p99)
3. **Error Rate** — `5xx count / total count` as percentage over time
4. **Instance Count** — `run.googleapis.com/container/instance_count` (min, max, avg)
5. **CPU Utilization** — `run.googleapis.com/container/cpu/utilizations` as line chart
6. **Memory Utilization** — `run.googleapis.com/container/memory/utilizations` with 85% threshold line

### Uptime Monitoring

For external uptime monitoring, configure a probe targeting the public health endpoint:

| Provider | Monitor URL | Interval |
|----------|------------|----------|
| **Cloud Monitoring Uptime Check** (recommended) | `https://portal.sofumarcommunityofmn.org/api/actuator/health` | 1 minute |
| **UptimeRobot** (free alternative) | `https://portal.sofumarcommunityofmn.org/api/actuator/health` | 5 minutes |

---

## Runbook References

### Runbook: Service Down

**Trigger:** Zero requests for > 3 minutes.

**First 5 Minutes:**
1. Check Cloud Run console — is the service active?
2. Check Cloud Run logs for crash loops or OOM kills
3. Check `/api/actuator/health` manually: `curl https://portal.sofumarcommunityofmn.org/api/actuator/health`
4. Check for recent deployments that may have introduced a breaking change

**Investigation:**
- Cloud Run → Revisions → check if latest revision is healthy
- Cloud Logging → filter `severity="ERROR"` for the last 30 minutes
- Check GCP status page for regional outages: https://status.cloud.google.com

**Mitigation:**
- If bad deployment: roll back to previous revision via Cloud Run console
- If resource exhaustion: increase memory/CPU in deployment config
- If GCP outage: wait for resolution; update status page

---

### Runbook: High Error Rate

**Trigger:** 5xx error rate > 5% for > 5 minutes.

**First 5 Minutes:**
1. Check Cloud Logging for error stack traces: `severity="ERROR"`
2. Identify the failing endpoint(s): filter by `httpRequest.requestUrl`
3. Check if errors correlate with a recent deployment

**Investigation:**
- Is it one endpoint or all endpoints?
- Check database connectivity: `/api/actuator/health` → `db` component
- Check Redis connectivity: `/api/actuator/health` → `redis` component
- Check Cloud Trace for failing spans

**Mitigation:**
- If database related: check Cloud SQL instance health, connection pool (`hikaricp_connections_active`)
- If application bug: roll back deployment
- If dependency timeout: check Resilience4j circuit breaker state

---

### Runbook: High Latency

**Trigger:** p95 latency > 500ms for > 5 minutes.

**First 5 Minutes:**
1. Check Cloud Run → Metrics → Request Latency chart
2. Check instance count — are instances scaling up (cold starts)?
3. Check CPU utilization — is the container CPU-bound?

**Investigation:**
- Cloud Trace → sort by latency descending → inspect slow traces
- Look for slow database queries in trace spans
- Check HikariCP pool exhaustion: `hikaricp_connections_active` near `maximum-pool-size`

**Mitigation:**
- If cold starts: enable `--cpu-boost` (already enabled), consider minimum instances
- If slow queries: analyze and optimize SQL, add indexes
- If pool exhaustion: increase `maximum-pool-size` or optimize connection usage

---

### Runbook: High CPU

**Trigger:** CPU > 80% sustained for > 10 minutes.

**Investigation:**
1. Check if traffic volume has increased (request count spike)
2. Check if a specific endpoint is CPU-intensive
3. Check for thread pool exhaustion or deadlocks

**Mitigation:**
- Increase CPU allocation in Cloud Run deployment config
- Optimize hot-path code
- Review virtual thread usage for blocking operations

---

### Runbook: High Memory

**Trigger:** Memory > 85% sustained for > 10 minutes.

**Investigation:**
1. Check JVM heap metrics: `jvm_memory_used_bytes` (if Prometheus is available)
2. Check for memory leaks (gradual increase over time)
3. Check Cloud Run revision memory limit (currently 1536Mi)

**Mitigation:**
- Increase memory allocation in Cloud Run deployment config
- Analyze heap dump (dev environment only — never in production)
- Review large collections, caching strategies, or connection pool sizes

---

### Runbook: Circuit Breaker Open

**Trigger:** `outboxRelay` circuit breaker in OPEN state.

**Context:** The circuit breaker protects the outbox event relay mechanism. When open, domain events are still persisted to the database but are not relayed to external consumers.

**Investigation:**
1. Check `/api/actuator/health` — is `circuitBreakers.outboxRelay` reporting OPEN?
2. Check Cloud Logging for outbox relay errors
3. Check if external message broker (when enabled) is reachable

**Mitigation:**
- The circuit breaker will automatically transition to HALF-OPEN after 30 seconds
- If the underlying issue is resolved, it will close automatically
- If persistent: check the external dependency causing relay failures
- No data loss — events remain in `outbox_event` table until successfully relayed

---

## Troubleshooting Quick Reference

### Decision Tree

```
Problem Reported
    │
    ├── Service unreachable?
    │   ├── YES → Check Cloud Run console → Check for crash loops → See "Service Down" runbook
    │   └── NO ↓
    │
    ├── High error rate?
    │   ├── One endpoint → Check logs for that handler → Check DB/Redis → Fix or rollback
    │   ├── All endpoints → Check DB connectivity → Check Redis → See "High Error Rate" runbook
    │   └── NO ↓
    │
    ├── Slow responses?
    │   ├── Cold starts? → Check instance count → Consider minimum instances
    │   ├── CPU-bound? → Check CPU metrics → Scale up or optimize
    │   ├── DB-bound? → Check Cloud Trace for slow queries → Optimize SQL
    │   └── NO ↓
    │
    └── Health check failing?
        ├── Liveness failing → JVM issue → Check for OOM, crash loops
        ├── Readiness failing → Dependency issue → Check DB/Redis connectivity
        └── Aggregate DOWN → Check circuit breaker state → See "Circuit Breaker Open" runbook
```

### Common Diagnostic Commands

```bash
# Check aggregate health (public)
curl -s https://portal.sofumarcommunityofmn.org/api/actuator/health | jq .

# Check liveness probe
curl -s https://portal.sofumarcommunityofmn.org/api/actuator/health/liveness | jq .

# Check readiness probe
curl -s https://portal.sofumarcommunityofmn.org/api/actuator/health/readiness | jq .

# Check detailed health (requires JWT)
curl -s -H "Authorization: Bearer <TOKEN>" \
  https://portal.sofumarcommunityofmn.org/api/actuator/health | jq .

# View application info
curl -s https://portal.sofumarcommunityofmn.org/api/actuator/info | jq .

# Cloud Logging: recent errors (via gcloud CLI)
gcloud logging read \
  'resource.type="cloud_run_revision" AND resource.labels.service_name="carizmi-backend" AND severity="ERROR"' \
  --limit=20 --format=json --project=<PROJECT_ID>

# Cloud Run: check service status
gcloud run services describe carizmi-backend --region=us-central1 --format=yaml
```
