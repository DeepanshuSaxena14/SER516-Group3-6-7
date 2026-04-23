# SER516-Group3-6-7
The code repository for Groups 3, 6, and 7 to compute different metrics of a project.

# Project Structure

```text
SER516-Group3-6-7/
├── afferent-efferent/         # Group 3 — Afferent/Efferent coupling metrics
├── defects-discovered/        # Group 7 — PMD defect analysis + frontend
├── fanin-fanout/              # Group 6 — Fan-In/Fan-Out coupling metrics
├── middleware/                # Shared middleware — orchestrates all services
├── prometheus/
│   ├── prometheus.yml         # Prometheus scrape config (all 3 services)
│   └── metrics-writer/        # Bridge service: Prometheus → Supabase
│       ├── index.js
│       ├── package.json
│       ├── Dockerfile
│       └── supabase-migration.sql
├── docker-compose.yml         # Root Docker Compose file
└── Jenkinsfile                # Root Jenkins automations file
```

## Prerequisites
Make sure the following are installed on your machine:

- Docker Desktop
- Docker Compose

Verify installation:

```bash
docker --version
docker compose version
```

## Environment Setup

# Start integrated services
```bash
docker compose up --build
```

> [!NOTE]
> The local `g6-grafana` container is disabled by default (migrated to hosted Grafana).
> Use the hosted Grafana at https://swent0linux.asu.edu/grafana/ for dashboards.
> To start the local Grafana container for development, run:


## How It Works

1. Access frontend using http://localhost:8081
2. Enters a public GitHub repository URL in the frontend and click **Analyse**
3. The frontend sends a `POST /analyze` request to the middleware
4. The middleware fans out **in parallel** to:
   - **g7-pmd** — clones the repo and runs defect analysis and computes and stores relevant metrics in the mongoDB module
   - **g6-metrics** — clones the repo, computes Fan-In and Fan-Out metrics, persists results to Supabase
5. The frontend displays a **"View Dashboards on Grafana →"** link on completion for redirect
6. The hosted Grafana dashboards automatically reflect the latest results
7. To access the dashboard, make sure you're in the group 6 area you will find each service's dashboard
   - Afferent - https://swent0linux.asu.edu/grafana/d/afferent-dashboard/afferent-dashboard?orgId=7&from=now-7d&to=now&timezone=browser&var-min_afferent=0&var-class_filter=
   - Efferent - https://swent0linux.asu.edu/grafana/d/efferent-dashboard/efferent-dashboard?orgId=7&from=now-7d&to=now&timezone=browser&var-min_efferent=0&var-class_filter=
   - Defects - https://swent0linux.asu.edu/grafana/d/defect-analysis-dashboard/defect-analysis-dashboard?orgId=7&from=now-7d&to=now&timezone=browser&var-min_priority=5&var-file_filter=
   - FanIn - https://swent0linux.asu.edu/grafana/d/fan-in-metrics/fan-in-metrics-dashboard?orgId=7&from=now-7d&to=now&timezone=browser&var-min_fanin=0&var-class_filter=
   - FanOut - https://swent0linux.asu.edu/grafana/d/fan-out-metrics/fan-out-metrics-dashboard?orgId=7&from=now-7d&to=now&timezone=browser&var-min_fanout=0&var-class_filter=

> [!NOTE]
> #### Afferent/Efferent Service (Group 3)
> The AE service is not exposed as a browser-based service. It runs interactively via Docker:
> ```bash
> docker compose run --rm g3-ae-metrics
> ```
> Select option `2` to analyze a GitHub repository. Results are persisted to the `afferent_efferent_result` table in Supabase.

> [!NOTE]
> GitHub repository URLs **must end with `.git`** for cloning to work correctly with JGit.
> Example: `https://github.com/junit-team/junit4.git`

---

## Prometheus Observability

All three services are instrumented with Prometheus and expose a `/prometheus` scrape endpoint:

| Service | Technology | Endpoint |
|---|---|---|
| `g6-metrics` | Java / Javalin + Micrometer | `http://localhost:8082/prometheus` |
| `middlewares` | Node.js / Express + prom-client | `http://localhost:4002/prometheus` |
| `g7-pmd` | Node.js / Express + prom-client | `http://localhost:4000/prometheus` |

The `prometheus` container (port `9090`) scrapes all three every 15 seconds. Verify targets at **http://localhost:9090/targets**.

### Metrics collected per service

|            Metric              | `g6-metrics` | `middleware` | `g7-pmd` |
|--------------------------------|--------------|--------------|----------|
| HTTP request rate              | (Micrometer) |       —      |    —     |
| HTTP error rate (4xx/5xx)      | (Micrometer) |       —      |    —     |
| `/analyze` latency p50/p95/p99 | (Micrometer) |       —      |    —     |
| JVM heap used / max            |     Yes      |       —      |    —     |
| CPU usage rate                 |     Yes      |      Yes     |    Yes   |
| Node.js heap used              |      —       |      Yes     |    Yes   |
| Event loop lag p50/p95/p99     |      —       |      Yes     |    Yes   |

### Metrics Writer — Prometheus → Supabase bridge

Because the hosted Grafana on `swent0linux.asu.edu` cannot directly reach a local Prometheus instance, a `metrics-writer` Docker service reads from the Prometheus HTTP API every 15 seconds and writes snapshots into the `observability_metrics` table in Supabase. The hosted Grafana then queries this table through its existing PostgreSQL datasource — no new datasource or server access required.

```
Local Docker                              Cloud
services → Prometheus (:9090)
                  ↓
           metrics-writer ──────────►  Supabase (observability_metrics)
                                                ↑
                                     hosted Grafana reads via PostgreSQL
```

#### One-time Supabase setup

Run the migration **once** in the Supabase SQL Editor (Dashboard → SQL Editor):

```sql
-- full script: prometheus/metrics-writer/supabase-migration.sql
CREATE TABLE IF NOT EXISTS observability_metrics (
  id         BIGSERIAL        PRIMARY KEY,
  service    TEXT             NOT NULL,
  metric     TEXT             NOT NULL,
  value      DOUBLE PRECISION NOT NULL,
  scraped_at TIMESTAMPTZ      NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_obs_service_metric_time
  ON observability_metrics (service, metric, scraped_at DESC);
```

#### Observability Dashboard

Import `fanin-fanout/grafana/dashboards/observability.json` into the hosted Grafana
(**Dashboards → Import → Upload JSON file**). The dashboard uses the existing
`postgres-metrics` datasource (Supabase) and shows:

- **Event loop lag** (p50 / p95 / p99) — middleware & g7-pmd
- **Node.js heap used** — middleware & g7-pmd
- **CPU usage rate** — all three services
- **JVM heap used vs max + utilisation gauge** — g6-metrics
- **HTTP request rate & error rate** — g6-metrics *(populates after first request)*
- **/analyze latency** (p50 / p95 / p99) — g6-metrics *(populates after first request)*

> [!NOTE]
> The HTTP request rate and latency panels show "No data" until at least one request
> has been made to g6-metrics. Send a test request to populate them:
> ```bash
> curl "http://localhost:8082/metrics/fanout?path=/input/Simple-Java-Calculator/src"
> ```
> Wait ~30 seconds (one Prometheus scrape + one writer cycle), then refresh the dashboard.

---

## Jenkins
We have automation pipelines set to update Grafana, run available static analysis and unit tests whenever there is a push on any branch.

- Jenkins URL: https://swent0linux.asu.edu/jenkins/job/Group-3-Group-6-Group-7/job/SER516-Group3-6-7/

---
