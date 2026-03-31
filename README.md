# SER516-Group3-6-7
The code repository for Groups 3,6, and 7 to compute different metrics of a project.

# Project Structure

```text
SER516-Group3-6-7/
├── afferent-efferent/         # Group 3
├── defects-discovered/        # Group 7
├── fanin-fanout/              # Group 6
└── docker-compose.yml         # Root Docker Compose file
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

# Start integrated services
```bash
docker compose up --build
```

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
docker compose down
```

### Accessing Each Running Service

#### Metrics API
- Base URL: `http://localhost:8082`
- Example endpoint: `http://localhost:8082/metrics/fanout?path=/input/Simple-Java-Calculator/src`

#### PostgreSQL
- Host: `localhost`
- Port: `5433`
- Database: `metrics`
- Username: `grafana`
- Password: `grafana`

#### Grafana
- URL: `http://localhost:3000`
- Username: `admin`
- Password: `admin`

#### PMD Backend
- URL: `http://localhost:4000`

#### Frontend
- URL: `http://localhost:8081`

#### Afferent/Efferent Service
Group 3 is not exposed as a browser-based service. Run it manually with:

```bash
docker compose run --rm g3-ae-metrics
```

## Quick Start — Full Stack with Grafana
 
Run all commands from the **repository root** (`SER516-Group3-6-7/`). 
 
### Step 1 — Start the stack
 
```bash
docker compose up --build -d
```
 
Wait about 30 seconds for all services to become healthy. You can watch progress with:
 
```bash
docker compose logs -f g6-metrics g6-postgres
```
 
You should see:
```
g6-metrics-1  | Metrics API server started on port 8080
g6-metrics-1  | Wrote 4 fan-out rows to database.
g6-postgres-1 | database system is ready to accept connections
```
 
### Step 2 — Trigger Fan-In computation
 
The healthcheck auto-triggers Fan-Out on startup, but Fan-In must be called manually:
 
```bash
curl "http://localhost:8082/metrics/fanin?path=/input/Simple-Java-Calculator/src"
```
 
Expected response (truncated):
```json
{
  "classLevel": [
    {"class":"simplejavacalculator.Calculator","fanIn":2},
    {"class":"simplejavacalculator.UI","fanIn":1}
  ],
  "methodLevel": [
    {"method":"simplejavacalculator.Calculator.calculateMono(MonoOperatorModes, Double)","fanIn":10},
    {"method":"simplejavacalculator.Calculator.calculateBi(BiOperatorModes, Double)","fanIn":9}
  ]
}
```
 
### Step 3 — Verify data landed in PostgreSQL
 
```bash
docker exec -it ser516-group3-6-7-g6-postgres-1 psql -U grafana -d metrics \
  -c "SELECT scope, COUNT(*), MAX(recorded_at) FROM fan_in_metrics GROUP BY scope;"
```
 
Expected:
```
  scope  | count |          max
---------+-------+-------------------------------
 class   |     5 | 2026-03-31 21:52:10.123+00
 method  |    28 | 2026-03-31 21:52:10.123+00
```
 
```bash
docker exec -it ser516-group3-6-7-g6-postgres-1 psql -U grafana -d metrics \
  -c "SELECT scope, COUNT(*) FROM fan_out_metrics GROUP BY scope;"
```
 
Expected:
```
 scope | count
-------+-------
 class |     4
```
 
### Step 4 — Open Grafana
 
Go to **http://localhost:3000** and log in with `admin` / `admin`.
 
Navigate to **Dashboards** and open:
- **Fan-Out Metrics Dashboard** — shows Fan-Out per class, bar chart, trend over time
- **Fan-In Metrics Dashboard** — shows class-level and method-level Fan-In panels
 
### Step 5 — Stop the stack
 
```bash
docker compose down
```
 
---
 
## API Reference
 
All endpoints are available at `http://localhost:8082`.
 
### `GET /metrics/fanout?path=<path>`
 
Returns class-level Fan-Out sorted descending.
 
```bash
curl "http://localhost:8082/metrics/fanout?path=/input/Simple-Java-Calculator/src"
```
 
Response:
```json
[
  {"class":"simplejavacalculator.UI","fanOut":13},
  {"class":"simplejavacalculator.BufferedImageCustom","fanOut":4},
  {"class":"simplejavacalculatorTest.CalculatorTest","fanOut":1},
  {"class":"simplejavacalculator.SimpleJavaCalculator","fanOut":1}
]
```
 
### `GET /metrics/fanin?path=<path>`
 
Returns class-level and method-level Fan-In in a unified response.
 
```bash
curl "http://localhost:8082/metrics/fanin?path=/input/Simple-Java-Calculator/src"
```
 
Response shape:
```json
{
  "classLevel": [
    {"class":"<FQCN>","fanIn":<int>}
  ],
  "methodLevel": [
    {"method":"<pkg.Class.method(ParamTypes)>","fanIn":<int>}
  ]
}
```
 
### Error responses
 
| Scenario | HTTP Status |
|---|---|
| `path` param missing | 400 |
| Path does not exist | 400 |
| Path is not a directory | 400 |
 
---
 
## Running Tests
 
From inside the `fanin-fanout/` directory:
 
```bash
cd fanin-fanout
mvn test
```
 
Expected:
```
Tests run: 91, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```
 
---
 
## Analyzing Your Own Java Project
 
Mount your project as a volume and query against it:
 
```bash
docker run --rm \
  -v "/absolute/path/to/your/project/src:/myproject" \
  -p 8082:8080 \
  ser516-group3-6-7-g6-metrics
 
curl "http://localhost:8082/metrics/fanout?path=/myproject"
curl "http://localhost:8082/metrics/fanin?path=/myproject"
```
 
---
 
## Troubleshooting
 
### Problem: `fan_in_metrics` relation does not exist
 
**Cause:** The PostgreSQL container reused an old volume that was created before the `fan_in_metrics` table was added to `init-fanout.sql`. The init SQL only runs on a brand new database.
 
**Fix:**
```bash
docker compose down
docker volume rm ser516-group3-6-7_g6-postgres-data
docker compose up --build -d
```
 
Then re-trigger Fan-In:
```bash
curl "http://localhost:8082/metrics/fanin?path=/input/Simple-Java-Calculator/src"
```
 
---
 
### Problem: Grafana Fan-In panels show "No data"
 
**Cause 1 — Fan-In endpoint has not been called yet.**
 
The healthcheck only triggers Fan-Out. Run:
```bash
curl "http://localhost:8082/metrics/fanin?path=/input/Simple-Java-Calculator/src"
```
Then refresh Grafana.
 
**Cause 2 — Wrong time range in Grafana.**
 
The dashboard defaults to "Last 7 days". If you just ran the stack, change the time picker to **Last 1 hour** and hit Refresh.
 
**Cause 3 — Grafana datasource hostname mismatch.**
 
Check the datasource config:
```bash
grep "url:" fanin-fanout/grafana/provisioning/datasources/datasource.yml
```
 
If it shows `postgres-metrics:5432`, fix it:
```bash
sed -i '' 's/url: postgres-metrics:5432/url: g6-postgres:5432/' \
  fanin-fanout/grafana/provisioning/datasources/datasource.yml
 
docker compose restart g6-grafana
```
 
---