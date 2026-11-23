# Pulse — API Monitoring & Incident Platform

A Java 17 / Spring Boot / PostgreSQL application for monitoring HTTP endpoints, recording health-check history, and tracking incidents from failure through recovery. The included responsive dashboard is served by Spring Boot and uses the same-origin REST API.

## Features

- Create, view, update, pause/enable, and delete monitored endpoints
- Configurable HTTP method, expected status, check interval, timeout, and optional latency threshold
- Scheduled checks for enabled endpoints, plus a manual **Run check** action
- Health-check history with server-side pagination
- Automatic incident creation on failure, failure-count increments while an incident remains open, and automatic resolution after a successful check
- Incident list, status filtering, and incident details
- Dashboard totals and overall recorded-check success rate
- Consistent JSON error responses for validation, missing resources, malformed requests, conflicts, and unexpected server errors
- Responsive frontend with endpoint search, loading/error feedback, and confirmation before destructive actions

## Requirements

- Java 17+
- PostgreSQL 14+
- Maven 3.9+ (or use the included Maven wrapper)
- Optional: Docker and Docker Compose

## Run locally

1. Create a PostgreSQL database and user (or use your existing local setup):

   ```sql
   CREATE USER hamsafarah WITH PASSWORD 'your-password';
   CREATE DATABASE api_monitoring OWNER hamsafarah;
   ```

2. Configure environment variables if your local credentials differ:

   ```bash
   export DB_URL='jdbc:postgresql://localhost:5432/api_monitoring'
   export DB_USERNAME='hamsafarah'
   export DB_PASSWORD='your-password'
   ```

3. From the project directory, run:

   ```bash
   ./mvnw spring-boot:run
   ```

   If the wrapper is not executable on macOS/Linux, run `bash mvnw spring-boot:run`.

4. Open **http://localhost:8080**.

The application uses `spring.jpa.hibernate.ddl-auto=update` for this development project. For production, replace this with versioned database migrations (Flyway or Liquibase).

## Run with Docker Compose

```bash
docker compose up --build
```

Then open **http://localhost:8080**. Compose starts PostgreSQL and the application. The development database credentials are defined in `docker-compose.yml`; change them before using this setup beyond local development.

Stop the services:

```bash
docker compose down
```

Remove the local database volume too (destructive):

```bash
docker compose down -v
```

## Run tests

```bash
./mvnw test
```

Tests use an in-memory H2 database and do not require a running PostgreSQL instance. Scheduled polling is disabled during tests.

The test suite includes:
- Spring application context and repository wiring
- Endpoint creation and retrieval
- REST API creation/listing and request validation
- Paginated health-check history
- Not-found and invalid incident-filter responses
- Incident opening, repeated failure counting, and resolution behavior

## REST API

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/api/endpoints` | List monitored endpoints |
| `POST` | `/api/endpoints` | Create an endpoint |
| `GET` | `/api/endpoints/{id}` | Get one endpoint |
| `PUT` | `/api/endpoints/{id}` | Update an endpoint |
| `PATCH` | `/api/endpoints/{id}/enabled` | Enable or pause an endpoint |
| `DELETE` | `/api/endpoints/{id}` | Delete endpoint and its history/incidents |
| `POST` | `/api/endpoints/{id}/check` | Trigger an immediate check |
| `GET` | `/api/endpoints/{id}/checks?page=0&size=20` | Paginated check history (maximum page size 100) |
| `GET` | `/api/endpoints/stats` | Dashboard statistics |
| `GET` | `/api/incidents` | List incidents |
| `GET` | `/api/incidents?status=OPEN` | Filter incidents by `OPEN` or `RESOLVED` |
| `GET` | `/api/incidents/{id}` | Get incident details |

### Create endpoint request

```json
{
  "name": "Example API",
  "url": "https://example.com",
  "method": "GET",
  "expectedStatus": 200,
  "intervalSeconds": 60,
  "timeoutMs": 5000,
  "latencyThresholdMs": 1000,
  "enabled": true
}
```

Allowed methods: `GET`, `HEAD`, `POST`, `OPTIONS`. URL must use HTTP or HTTPS. `latencyThresholdMs` is optional.

### Error response

```json
{
  "timestamp": "2026-11-23T12:00:00",
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Endpoint not found with id: 123"
}
```

## Configuration

| Environment variable | Default | Purpose |
|---|---|---|
| `SERVER_PORT` | `8080` | HTTP port |
| `DB_URL` | `jdbc:postgresql://localhost:5432/api_monitoring` | JDBC URL |
| `DB_USERNAME` | `hamsafarah` | PostgreSQL user |
| `DB_PASSWORD` | empty | PostgreSQL password |
| `SHOW_SQL` | `false` | Enable Hibernate SQL logging |
| `MONITORING_SCHEDULER_DELAY_MS` | `5000` | How often the scheduler scans for due checks |
| `MONITORING_SCHEDULER_ENABLED` | `true` | Enable/disable scheduled monitoring |

The scheduler scans enabled endpoints and runs a check when the configured interval has elapsed since the most recent check. Manual checks are available regardless of the scheduler setting.

## Current scope and production considerations

This is a complete MVP, not yet a hardened multi-tenant production monitoring service. Before production use, add authentication/authorization, rate limiting, outbound URL/SSRF protections, database migrations, structured operational logging/metrics, alert delivery, and concurrency-safe incident creation for multiple application instances. Do not expose the dashboard publicly without access controls.
