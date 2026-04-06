# SER516-Group3-6-7
The code repository for Groups 3,6, and 7 to compute different metrics of a project.

# Project Structure

```text
SER516-Group3-6-7/
├── afferent-efferent/         # Group 3
├── defects-discovered/        # Group 7
├── fanin-fanout/              # Group 6
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
docker compose down -v
```

### Accessing Each Running Service

#### FanIn/Out Metrics
- URL: `http://localhost:8082`
  
> [!NOTE]
> As of the latest current version, the repo being analyzed needs to be already cloned and on the device using "Project Path". We will integrate the cloning api from the pmd module at a later release

#### FIFO Endpoints: 
  - get `http://localhost:8082/metrics/fanout?path={Project Path}`
  - get `http://localhost:8082/metrics/fanin?path={Project Path}`

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
> This frontend's only purpose is to allow the user to enter a GitHub repo URL to analyze instead of typing it out as a command. It is not the main dashboard used for any actual UI or statistics. The results are only returned in the api response and not shown in this frontend. The metrics will be integrated with grafana in this sprint or the next.


#### Mongo DB Server (For Focus Factor)
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

# Quick Start & Testing

Follow these steps to get the environment running and verify the services.

### 1. Start the Environment
Ensure Docker Desktop is running, then execute:
```bash
docker compose up -d --build
```
*Wait ~30 seconds for MongoDB and the services to initialize.*

### 2. Verify Endpoints with Curl

#### A. Trigger PMD Analysis (GitHub Clone)
Analyzes a Java project from GitHub and returns analysis.
```bash
curl "http://localhost:4000/api/pmd/analyze?github_link=https://github.com/octocat/Hello-World"
```

#### B. Trigger PMD Analysis (Local Path)
Analyzes a local project already present in the container.
```bash
curl -X POST http://localhost:4000/api/pmd/run-pmd \
  -H "Content-Type: application/json" \
  -d '{"repoPath": "."}'
```

#### B. Get Bug Count Summary (Mongo Service)
Retrieves the latest identified bug count stored in the database.
```bash
curl http://localhost:4001/api/defects/summary
```

#### C. Focus Factor Stats (Mongo Service)
View existing focus factor capacity/velocity records.
```bash
curl http://localhost:4001/api/stat
```

#### D. Fan-Out Metrics (FanIn/Out Service)
*Note: Replace `{Project Path}` with an absolute path to a Java project on your local machine.*
```bash
curl "http://localhost:8082/metrics/fanout?path=/input/Simple-Java-Calculator/src"
```

## Jenkins
- We have automation pipelines set to update grafana, run any available static analysis and unit tests whenever there is a push on any branches
- To access Jenkins use this link: https://swent0linux.asu.edu/jenkins/job/Group-3-Group-6-Group-7/job/SER516-Group3-6-7/

# More Info

> [!NOTE]
> #### Afferent/Efferent Service
> Group 3 is not exposed as a browser-based service. Run it manually with:
>```bash
>docker compose run --rm g3-ae-metrics
>```
