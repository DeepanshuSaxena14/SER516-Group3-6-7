# SER516-Group3-6-7
The code repository for Groups 3,6, and 7 to compute different metrics of a project.

# Project Structure

```text
SER516-Group3-6-7/
├── afferent-efferent/         # Group 3
├── defects-discovered/        # Group 7
├── fanin-fanout/              # Group 6
├── middleware/                # Group 3-6-7 shared middleware
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

> [!NOTE]
> The local `g6-grafana` container is disabled by default (migrated to hosted Grafana).
> Use the hosted Grafana at https://swenv0linux.asu.edu/grafana/ for dashboards.
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

#### Grafana (Hosted)
- URL: `https://swenv0linux.asu.edu/grafana/`
- Username: your ASU Grafana account
- Dashboards: Fan-Out Metrics Dashboard, Fan-In Metrics Dashboard

> [!IMPORTANT]
> The hosted Grafana connects to the PostgreSQL datasource via the machine running `docker compose up`.
> The datasource is configured with a direct IP address on the ASU network.
> **Before a demo session**, whoever runs `docker compose up` must:
> 1. Find their ASU Ethernet IP: run `ipconfig` and look for the adapter with `asu.edu` DNS suffix
> 2. Update the `grafana-postgresql-datasource` Host URL in hosted Grafana to `{YOUR_IP}:5433`
> 3. Hit **Save & test** to confirm the green connection status
> 4. Run the FIFO endpoints above to populate fresh data into the dashboards

#### Grafana (Local — optional, for development only)
- URL: `http://localhost:3000`
- Username: `admin`
- Password: `admin`
- Start with: `docker compose --profile local up`

#### PMD Backend
- URL: `http://localhost:4000`

#### PMD Endpoints:
  - #### Run pmd analysis
    - post `/api/github/clone`

take in a json body of this format:
```JSON
{ "github_link": "URL" }
```

#### Frontend
- URL: `http://localhost:80`

> [!NOTE]
> This frontend's only purpose is to allow the user to enter a GitHub repo URL to analyze instead of typing it out as a command. It is not the main dashboard used for any actual UI or statistics. The results are only returned in the api response and not shown in this frontend. The metrics will be integrated with grafana in this sprint or the next.


#### Mongo DB Server (For Focus Factor)
- URL: `http://localhost:4001`
  
#### Mongo Endpoints
  - #### Stats (Focus factor entry)
    - Get all stats   
      - get `/api/stat `

    - Create stat
      - post `/api/stat`

    - Update stat
      - put `/api/stat/:id`

    - Delete stat
      - delete `/api/stat/:id`

example json body for focus factor create and update stat:
```JSON
{
  "workCapacity": 80,
  "velocity": 45
}
```

## Jenkins
- We have automation pipelines set to update grafana, run any available static analysis and unit tests whenever there is a push on any branches
- To access Jenkins use this link: https://swenv0linux.asu.edu/jenkins/job/Group-3-Group-6-Group-7/job/SER516-Group3-6-7/

# More Info

> [!NOTE]
> #### Afferent/Efferent Service
> Group 3 is not exposed as a browser-based service. Run it manually with:
>```bash
>docker compose run --rm g3-ae-metrics
>```