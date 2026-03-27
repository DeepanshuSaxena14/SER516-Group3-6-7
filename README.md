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

# Start integrated services
docker compose up --build

# Check running containers
docker compose ps

# Run Group 3 manually
docker compose run --rm g3-ae-metrics

# Stop everything
docker compose down

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