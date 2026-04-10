# SER516-Group3-6-7
The code repository for Groups 3, 6, and 7 to compute different metrics of a project.

# Project Structure

```text
SER516-Group3-6-7/
├── afferent-efferent/         # Group 3 — Afferent/Efferent coupling metrics
├── defects-discovered/        # Group 7 — PMD defect analysis + frontend
├── fanin-fanout/              # Group 6 — Fan-In/Fan-Out coupling metrics
├── middleware/                # Shared middleware — orchestrates all services
└── docker-compose.yml         # Root Docker Compose file
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

Copy the example environment file and fill in the required values:

```bash
cp .env.example .env
```

Set the following variable in your `.env` file:

```
SUPABASE_PASSWORD=your_supabase_password_here
```

> [!NOTE]
> Contact a team member for the actual Supabase password. Never commit the `.env` file to the repository.

# Start integrated services
```bash
docker compose up --build
```

> [!NOTE]
> The local `g6-grafana` container is disabled by default (migrated to hosted Grafana).
> Use the hosted Grafana at https://swent0linux.asu.edu/grafana/ for dashboards.
> To start the local Grafana container for development, run:
> ```bash
> docker compose --profile local up --build
> ```

# Check running containers
```bash
docker compose ps
```

# Run Group 3 manually
```bash
docker compose run --rm g3-ae-metrics
```

# Stop everything
```bash
docker compose down -v
```

---

## How It Works

When a user enters a public GitHub repository URL in the frontend and clicks **Analyse**, the following happens automatically:

1. The frontend sends a `POST /analyze` request to the middleware
2. The middleware fans out **in parallel** to:
   - **g7-pmd** — clones the repo and runs PMD static analysis
   - **g6-metrics** — clones the repo, computes Fan-In and Fan-Out metrics, persists results to Supabase
3. Both services write their results to Supabase cloud PostgreSQL
4. The frontend displays a **"View Dashboards on Grafana →"** link on completion
5. The hosted Grafana dashboards automatically reflect the latest results

> [!NOTE]
> GitHub repository URLs **must end with `.git`** for cloning to work correctly with JGit.
> Example: `https://github.com/junit-team/junit4.git`

---

## Accessing Each Running Service

### Frontend (Unified Analysis Entry Point)
- URL: `http://localhost:8081`

The main entry point for end-to-end analysis. Enter a public GitHub repository URL and click **Analyse**. Results are persisted to Supabase and viewable on the hosted Grafana dashboards.

---

### Middleware (Orchestration Layer)
- URL: `http://localhost:4002`

The middleware routes requests to backend services. It exposes both a fan-out orchestration endpoint and proxied routes to individual services.

#### Analyze endpoint — triggers all metric services in parallel
- `POST /analyze`

Request body:
```json
{ "github_link": "https://github.com/owner/repo.git" }
```

Response:
```json
{
  "github_link": "https://github.com/owner/repo.git",
  "pmd": { ... },
  "metrics": {
    "status": "ok",
    "javaFilesAnalyzed": 471,
    "classesWithFanOut": 398,
    "classesWithFanIn": 450
  },
  "errors": []
}
```

> [!NOTE]
> The `errors` array will contain partial failures if one service is unavailable. The other service's results are still returned.

---

### Fan-In/Fan-Out Metrics (g6-metrics)
- URL: `http://localhost:8082`

#### Endpoints

Analyze a **local path** (repo already cloned on disk):
- `GET /metrics/fanout?path={absolute/path/to/java/project}`
- `GET /metrics/fanin?path={absolute/path/to/java/project}`

Analyze a **GitHub repository directly** by URL (clones automatically):
- `GET /metrics/analyze?github_link={GitHub URL}`

Example:
```bash
curl "http://localhost:8082/metrics/analyze?github_link=https://github.com/junit-team/junit4.git"
```

Response:
```json
{
  "status": "ok",
  "repo": "https://github.com/junit-team/junit4.git",
  "javaFilesAnalyzed": 471,
  "classesWithFanOut": 398,
  "classesWithFanIn": 450
}
```

> [!NOTE]
> The `/metrics/analyze` endpoint clones the repository, runs both Fan-In and Fan-Out analysis,
> and automatically persists results to Supabase for Grafana. GitHub URLs must end with `.git`.

---

### PMD Backend (g7-pmd)
- URL: `http://localhost:4000`

#### Run PMD analysis
- `POST /api/github/clone`

Request body:
```json
{ "github_link": "https://github.com/owner/repo.git" }
```

---

### PostgreSQL (Local — g6-postgres container)
- Host: `localhost`
- Port: `5433`
- Database: `metrics`
- Username: `grafana`
- Password: `grafana`

### PostgreSQL (Cloud — Supabase, used by Grafana and all metric services)
- Host: `aws-1-us-east-1.pooler.supabase.com`
- Port: `5432`
- Database: `postgres`
- Username: `postgres.ouogetytffnkwjdiqjxv`
- SSL: required

---

#### PMD Endpoints:
- **Run PMD analysis (Local)**
  - `POST /api/pmd/run-pmd`
  - Body: `{ "repoPath": "/app/work/project" }`
  - Returns `report` results.

- **Clone and Analyze (GitHub)**
  - `GET /api/pmd/analyze?github_link=URL`
  - Returns `report` results.

#### Frontend
- URL: `http://localhost:8081`

> [!NOTE]
> The hosted Grafana connects to the Supabase cloud PostgreSQL instance.
> Dashboards update automatically after each `/analyze` call — no manual refresh required.

### Grafana (Local — optional, for development only)
- URL: `http://localhost:3000`
- Username: `admin`
- Password: `admin`
- Start with: `docker compose --profile local up`

---

### Mongo DB Server (For Focus Factor)
- URL: `http://localhost:4001`
  
#### Mongo Endpoints
- **Defects Summary (Bug Count)**
  - `GET /api/defects/summary`
  - Returns the latest identified bug count.

- **Stats (Focus factor entry)**
  - Get all stats: `GET /api/stat`
  - Create stat: `POST /api/stat`
  - Update stat: `PUT /api/stat/:id`
  - Delete stat: `DELETE /api/stat/:id`

Example JSON body for Focus Factor create/update:
```JSON
{
  "workCapacity": 80,
  "velocity": 45
}
```

---

## Jenkins
We have automation pipelines set to update Grafana, run available static analysis and unit tests whenever there is a push on any branch.

- Jenkins URL: https://swent0linux.asu.edu/jenkins/job/Group-3-Group-6-Group-7/job/SER516-Group3-6-7/

---

## More Info

> [!NOTE]
> #### Afferent/Efferent Service (Group 3)
> The AE service is not exposed as a browser-based service. It runs interactively via Docker:
> ```bash
> docker compose run --rm g3-ae-metrics
> ```
> Select option `2` to analyze a GitHub repository. Results are persisted to the `afferent_efferent_result` table in Supabase.
>
> When running with Supabase integration, pass the JDBC connection details explicitly:
> ```bash
> docker compose --profile manual run --rm \
>   -e "JDBC_URL=jdbc:postgresql://aws-1-us-east-1.pooler.supabase.com:5432/postgres?sslmode=require" \
>   -e "JDBC_USER=postgres.ouogetytffnkwjdiqjxv" \
>   -e "JDBC_PASSWORD=<password>" \
>   g3-ae-metrics
> ```