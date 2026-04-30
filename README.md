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
   - Observability - https://swent0linux.asu.edu/grafana/d/ser516-observability/service-observability-metric?orgId=7&from=now-24h&to=now&timezone=browser&refresh=30s

---

> [!NOTE]
> There is a taiga-service folder and dashboard in grafana but are incomplete. 

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


## Jenkins
We have automation pipelines set to update Grafana, run available static analysis and unit tests whenever there is a push on any branch.

- Jenkins URL: https://swent0linux.asu.edu/jenkins/job/Group-3-Group-6-Group-7/job/SER516-Group3-6-7/

---
