## SER 516 - Software Agility | Group 6 Project

## Team:
Arvind Tadasad (atadasad)
Dikshant Meshram (dmeshram)
Deepanshu Saxena (dsaxen14)
Prince Rwamatwara (irwamatw)
Ethan Freeman (ehfreema)

## Working Metric:
Fan-In | Fan-Out

## Project Overview

This project implements class-level Fan-In and Fan-Out coupling metrics for Java projects, exposed as a REST API and visualized through Grafana.
The tool:
- Parses Java source files
- Identifies class-to-class dependencies
- Computes:
    - Fan-Out → Number of distinct outbound dependencies from a class
    - Fan-In → Number of distinct inbound dependencies to a class
- Outputs structured results in:
    - JSON
    - CSV
    - Console output
- Runs via command line
- Integrates with Jenkins CI pipeline

## Tech Stack

- Java 17
- Maven
- JUnit
- GitHub Actions (CI)
- Jenkins (External CI)
- Taiga (Agile Project Tracking)

## Project Structure

SER516-Group6-Project/
├── src/
│   ├── main/java/edu/asu/ser516/metrics/
│   │   ├── MetricsApiServer.java           # REST API server (Javalin)
│   │   ├── FanOutComputer.java             # Fan-Out computation logic
│   │   ├── FanOutComputerMain.java         # CLI entry point
│   │   ├── CouplingAnalyzer.java           # Fan-In computation logic
│   │   ├── OutgoingReferenceExtractor.java # AST-based reference extraction
│   │   ├── ClassIndexBuilder.java          # Project class index builder
│   │   ├── SourceScanner.java              # Java file discovery
│   │   ├── ReferenceAdapters.java          # Converts reference maps to edges
│   │   ├── ClassReference.java             # Value object for class references
│   │   └── MetricWriters.java              # CSV/JSON output writers
│   └── test/java/edu/asu/ser516/metrics/
│       ├── MetricsApiServerTest.java       # REST API tests (TDD)
│       └── FanOutComputerTest.java         # Fan-Out unit tests
├── input/
│   └── Simple-Java-Calculator/             # Sample project for testing
├── Dockerfile                              # Multi-stage Docker build
├── .dockerignore
├── pom.xml                                 # Maven build + shade plugin
├── Jenkinsfile                             # Jenkins CI pipeline
├── .github/workflows/ci.yml               # GitHub Actions CI
└── README.md

## Usage

After building the project with `mvn clean package`, you can run the metrics tool using the generated JAR file in the `target/` directory.

### Command Line Interface

```bash
java -jar target/ser516-group6-metrics-1.0.0.jar <input-path> [options]
```

**Arguments:**
- `<input-path>`: Path to the root directory of the Java project to analyze.

**Options:**
- `-o, --output <file>`: Specify the output file path. If omitted, results are printed to stdout.
- `-f, --format <fmt>`: Specify the output format. Supported formats: `text` (default), `csv`, `json`.
- `-h, --help`: Show help message.

### Examples

**Analyze a project and print results to console:**
```bash
java -jar target/ser516-group6-metrics-1.0.0.jar /path/to/project
```

**Save results as CSV:**
```bash
java -jar target/ser516-group6-metrics-1.0.0.jar /path/to/project -o metrics.csv -f csv
```

**Save results as JSON:**
```bash
java -jar target/ser516-group6-metrics-1.0.0.jar /path/to/project -o metrics.json -f json
```
│
├── src/
│   ├── main/java/edu/asu/ser516/metrics/
│   │   ├── FanOutComputerMain.java
│   │   ├── SourceScanner.java
│   │   ├── ...
│   │
│   └── test/
│
├── pom.xml
├── Jenkinsfile
├── docker/
├── README.md
└── target/

## Prerequisites

### To run via Docker (recommended — no Java or Maven required)
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running

### To run locally without Docker
- Java 17+
- Maven 3.8+

---

## Option 1 — Run with Docker

This is the primary way to run the service. No Java or Maven installation required.

### Step 1 — Clone the repository

```bash
git clone https://github.com/dmeshram/SER516-Group6-Project.git
cd SER516-Group6-Project
```

### Step 2 — Build the Docker image

```bash
docker build -t ser516-metrics .
```

Expected output ends with:
```
Successfully tagged ser516-metrics:latest
```

The multi-stage build compiles the project inside a Maven container and produces a minimal runtime image using `eclipse-temurin:17-jre-jammy`. No local Java or Maven installation is needed.

### Step 3 — Configure environment (optional)

You can customize ports and host paths used by Docker Compose via the `.env` file in the project root:

```bash
METRICS_PORT=8086
GRAFANA_PORT=3006
INPUT_DIR=./input
GRAFANA_PROVISIONING_DIR=./grafana/provisioning
```

`docker-compose.yml` uses these variables so paths and ports are **not hardcoded**. You can override them either by editing `.env` or exporting environment variables before running Compose.

### Step 4 — Run the container stack with Docker Compose

From the project root:

```bash
docker compose up -d --build
```

This will build and start both the metrics service and Grafana using the configured environment values.

### Alternative — Run a single container manually

**Analyze the included sample project:**

```bash
docker run --rm \
  -v "$(pwd)/input:/input" \
  -p 8086:8080 \
  ser516-metrics
```

Expected startup output:
```
[main] INFO io.javalin.Javalin - Listening on http://localhost:8086/
Metrics API server started on port 8086
```

> **macOS/Linux note:** The quotes around `"$(pwd)/input:/input"` are required to handle uppercase letters or spaces in your directory path.

### Step 5 — Query the API

Open a second terminal while the container is running:

**Fan-Out for the sample project:**
```bash
curl "http://localhost:8086/metrics/fanout?path=/input/Simple-Java-Calculator/src"
```

Expected response:
```json
[
  {"class":"simplejavacalculator.UI","fanOut":13},
  {"class":"simplejavacalculator.BufferedImageCustom","fanOut":4},
  {"class":"simplejavacalculatorTest.CalculatorTest","fanOut":1},
  {"class":"simplejavacalculator.SimpleJavaCalculator","fanOut":1}
]
```

**Fan-In for the sample project:**
```bash
curl "http://localhost:8086/metrics/fanin?path=/input/Simple-Java-Calculator/src"
```

### Step 5 — Stop the container

Press `Ctrl+C` in the terminal where the container is running. Because the container was started with `--rm`, it removes itself automatically.

Or stop it from another terminal:
```bash
docker stop $(docker ps -q --filter ancestor=ser516-metrics)
```

---

## Analyzing Any Java Project with Docker

To analyze a Java project on your host machine, mount its source directory as a volume:

```bash
docker run --rm \
  -v "/absolute/path/to/your/project/src:/project" \
  -p 8086:8080 \
  ser516-metrics
```

Then query using the container-side path:
```bash
curl "http://localhost:8086/metrics/fanout?path=/project"
curl "http://localhost:8086/metrics/fanin?path=/project"
```

The rule is: whatever local path you mount to the **left** of the `:` in `-v`, use the **right-hand side** path in the `?path=` query parameter.

---

## Changing the Port

The server defaults to port `8080` (mapped to `8086` in Docker). To use a different port, pass the `PORT` environment variable:

```bash
docker run --rm \
  -v "$(pwd)/input:/input" \
  -p 9090:9090 \
  -e PORT=9090 \
  ser516-metrics
```

Then query on the new port:
```bash
curl "http://localhost:9090/metrics/fanout?path=/input/Simple-Java-Calculator/src"
```

---

## API Reference

### `GET /metrics/fanout`

Returns class-level Fan-Out for all classes in the given Java project, sorted descending by value.

**Query Parameters:**

| Parameter | Required | Description |
|---|---|---|
| `path` | Yes | Absolute path inside the container to the Java source directory |

**Success Response — HTTP 200:**
```json
[
  {"class": "simplejavacalculator.UI", "fanOut": 13},
  {"class": "simplejavacalculator.BufferedImageCustom", "fanOut": 4},
  {"class": "simplejavacalculatorTest.CalculatorTest", "fanOut": 1},
  {"class": "simplejavacalculator.SimpleJavaCalculator", "fanOut": 1}
]
```

**Error Response — HTTP 400:**
```json
{"error": "Path does not exist: /invalid/path"}
```

---

### `GET /metrics/fanin`

Returns class-level Fan-In for all classes in the given Java project, sorted descending by value. All project classes appear in the result, including those with a value of zero.

**Query Parameters:**

| Parameter | Required | Description |
|---|---|---|
| `path` | Yes | Absolute path inside the container to the Java source directory |

**Success Response — HTTP 200:**
```json
[
  {"class": "simplejavacalculator.Calculator", "fanIn": 2},
  {"class": "simplejavacalculator.UI", "fanIn": 1},
  {"class": "simplejavacalculator.BufferedImageCustom", "fanIn": 1},
  {"class": "simplejavacalculator.SimpleJavaCalculator", "fanIn": 0}
]
```

**Error Response — HTTP 400:**
```json
{"error": "Required query parameter 'path' is missing or empty."}
```

---

### Error Cases

| Scenario | HTTP Status |
|---|---|
| `path` parameter missing | 400 |
| `path` parameter is empty string | 400 |
| Path does not exist on the filesystem | 400 |
| Path exists but is not a directory | 400 |
| Unknown endpoint | 404 |

---

## Option 2 — Run Locally Without Docker

### Step 1 — Clone and build

```bash
git clone https://github.com/dmeshram/SER516-Group6-Project.git
cd SER516-Group6-Project
mvn clean package -DskipTests
```

This produces a fat JAR at `target/ser516-group6-metrics-1.0.0.jar` with all dependencies bundled via the Maven Shade plugin.

### Step 2 — Start the REST API server

```bash
java -jar target/ser516-group6-metrics-1.0.0.jar
```

Query it using your local filesystem path:
```bash
curl "http://localhost:8086/metrics/fanout?path=$(pwd)/input/Simple-Java-Calculator/src"
```

### Step 3 — Run via CLI (no server)

To compute Fan-Out and write results directly to files:

```bash
mvn exec:java \
  -Dexec.mainClass="edu.asu.ser516.metrics.FanOutComputerMain" \
  -Dexec.args="input/Simple-Java-Calculator/src both out"
```

Output is written to the `out/` directory:
- `out/fanout.json`
- `out/fanout.csv`

---

## Running Tests

```bash
mvn test
```

All tests must pass before merging into `develop` or `main`.

```
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
```

---

## CI/CD

### GitHub Actions

Triggers automatically on every push to every branch and on all pull requests.

Pipeline stages:
1. Checkout
2. Set up JDK 17 (Temurin distribution)
3. `mvn clean test`

Configuration: `.github/workflows/ci.yml`

### Jenkins

External CI pipeline with quality gates.

Pipeline stages:
1. Checkout
2. Build — `mvn clean compile`
3. Test — `mvn test`

Configuration: `Jenkinsfile`

To connect Jenkins:
1. Create a new Pipeline job
2. Set **Pipeline script from SCM**
3. Point to this repository
4. Set branch to `develop` or `main`

---

## How the Metrics Are Calculated

### Fan-Out

For each class, the tool counts the number of **distinct project-internal classes** it references through any of the following:

- Inheritance (`extends` / `implements`)
- Field type declarations
- Method parameter types
- Method return types
- Local variable declarations
- Object instantiation (`new ClassName()`)
- Static access (`ClassName.method()`)
- Type casting
- Generic type arguments (`List<ClassName>`)
- Annotations (if the annotation type is defined in the project)

**Exclusions:** `java.*`, `javax.*`, primitives (`int`, `boolean`, etc.), self-references, and third-party library classes. Only classes defined within the scanned source tree count.

**Counting:** Each referenced class counts once regardless of how many times it appears. If class `A` has two fields of type `B` and one method returning `B`, the Fan-Out contribution from `B` is **1**.

### Fan-In

Derived by inverting the Fan-Out relationships. For each class `B`, Fan-In equals the number of other project classes that have `B` in their Fan-Out set. All project classes appear in the Fan-In result — including entry-point classes and leaf classes with a value of zero.

---

## Metric Interpretation

| Fan-Out Value | Interpretation |
|---|---|
| 0 | Leaf class — no internal dependencies |
| 1–5 | Healthy — focused responsibilities |
| 6–10 | Elevated — consider splitting responsibilities |
| 10+ | High coupling — likely violates Single Responsibility Principle |

| Fan-In Value | Interpretation |
|---|---|
| 0 | Unused class or application entry point |
| 1–3 | Normal usage |
| 4+ | Core/utility class — changes here have wide impact across the project |
-Dexec.mainClass="edu.asu.ser516.metrics.FanOutComputerMain" \
-Dexec.args="input/Simple-Java-Calculator/src both out"

## Output Results
You can check the output, in "Out" folder in root directory.

## Grafana + PostgreSQL Dashboards

The project runs an automated Docker stack with Grafana, PostgreSQL, and dashboards for Fan-In and Fan-Out metrics.

### 1. Start the Stack (or Wipe & Restart)
```bash
docker compose down -v --remove-orphans
docker compose up -d
```

### 2. Generate the Metrics
Run this inside the container to generate files and snapshot metrics to PostgreSQL:
```bash
docker exec -e JDBC_URL="jdbc:postgresql://postgres-metrics:5432/metrics" -e JDBC_USER="grafana" -e JDBC_PASSWORD="grafana" ser516-group6-project-metrics-service-1 java -cp app.jar edu.asu.ser516.metrics.FanOutComputerMain /input both /out
```

### 3. View the Dashboards
Navigate to **http://localhost:3006** (Login: `admin` / `admin`). Data will automatically show in the provisioned dashboards.

## CI/CD - Jenkins Integration
This project includes:
- Jenkinsfile
- Automated pipeline stages:
- Checkout
- Build
- Test
- Generate metrics
- Archive artifacts
## To Run in Jenkins:
- Create a new Pipeline job
- Connect GitHub repository
- Set:
  - Pipeline script from SCM
  - Branch: develop or main
- Build the project

## How Fan-Out is Calculated

For each class:
1. Parse imports and type usages
2. Identify distinct referenced classes
3. Count unique outbound dependencies
4. Store in structured format

## Summary

This project delivers a fully automated, CI-integrated tool for computing class-level Fan-In and Fan-Out metrics for Java applications, aligned with Agile development practices and sprint-based tracking.
