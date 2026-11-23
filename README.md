# Pulse — API Monitoring & Incident Platform

A Java 17, Spring Boot, and PostgreSQL application that monitors HTTP endpoints, tracks service health, and manages incidents automatically.

### Features
- Monitor HTTP endpoints automatically or manually
- Configure check intervals, timeouts, and expected status codes
- Track health-check history
- Automatically create and resolve incidents
- View service health through a responsive dashboard

### Tech Stack
Java 17 · Spring Boot · PostgreSQL · Maven · Docker · JavaScript

### Run Locally
```bash
./mvnw spring-boot:run
```

Requires Java 17+ and PostgreSQL. Configure `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` before starting.

### Run with Docker
```bash
export POSTGRES_PASSWORD='your-password'
docker compose up --build
```

### Run Tests
```bash
./mvnw test
```

Tests use an in-memory H2 database.
