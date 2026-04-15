# Carizmi Community Platform

An open-source, full-stack community management platform built to deliver comprehensive solutions for **nonprofit organizations** and **task automation** for small to mid-size businesses.

## Overview

Carizmi Community Platform is designed to be a modular, extensible foundation that organizations can adopt and customize to manage their community operations — from membership records and financial tracking to automated workflows and reporting.

### Key Capabilities

- **Membership Management** — Member registration, profiles, status tracking, and lookup
- **Financial Operations** — Payment processing, expense tracking, quarterly fee checklists, and collection summaries
- **Dashboard & Analytics** — Real-time metrics and aggregated views across all domains
- **Identity & Access Control** — JWT-based stateless authentication, role-based authorization, and admin user provisioning
- **Platform Administration** — System settings, reference data management, and configurable branding
- **API-First Architecture** — Auto-generated OpenAPI contracts with type-safe TypeScript clients

### Contributing

This is a public, open-source project — contributions from IT professionals and developers are welcome. The full scope of the platform is evolving, and there are opportunities to contribute across the backend, frontend, infrastructure, and documentation.

## Tech Stack

| Layer | Technologies |
|-------|-------------|
| **Backend** | Java 21, Spring Boot 3.5.6 (Stateless, JWT) |
| **Frontend** | React 19, TypeScript, Vite, Ant Design, Nginx |
| **Database** | MySQL 8.0 |
| **Cache** | Redis |
| **Infrastructure** | Docker, Docker Compose, GitHub Actions (CI/CD) |
| **API Automation** | Springdoc OpenAPI, Orval, Axios |

## Documentation

| Document | Description |
|----------|-------------|
| **[Architecture](docs/ARCHITECTURE.md)** | Business logic engine, domain validation, lifecycle hooks, and VO ↔ BL one-to-one enforcement |
| **[API Generation Pipeline](docs/API_GENERATION.md)** | OpenAPI contract extraction and TypeScript code generation automation |
| **[Database Migration Strategy](docs/DATABASE_MIGRATION.md)** | Hybrid Liquibase + Flyway schema management with timestamp-based versioning |
| **[Backup Strategy](docs/BACKUP.md)** | Automated database backups and restore procedures |
| **[Monitoring & Alerting](docs/MONITORING.md)** | Actuator endpoints, uptime monitoring, and observability roadmap |

## Getting Started

### Prerequisites
- Java 21
- Docker & Docker Compose
- Node.js 24+ (auto-installed by Maven for frontend builds)

### Local Development

```bash
# Start infrastructure (database + cache)
docker-compose up -d db redis

# Run backend
cd backend && ../mvnw spring-boot:run

# Run frontend (separate terminal)
cd frontend && npm install && npm run dev
```

### Running with Docker Compose (Full Stack)

```bash
# Ensure .env is configured (see below)
docker-compose up -d --build
```

The application will be available at:
- **Frontend**: http://localhost:8081
- **Backend API**: http://localhost:8080/api

### Environment Variables

For production and CI/CD, the following secrets must be provided. **Do not** commit these to version control.

| Variable | Description | Example |
|----------|-------------|---------|
| `DB_URL` | JDBC URL for MySQL | `jdbc:mysql://prod-db:3306/carizmi` |
| `DB_USER` | MySQL username | `carizmi` |
| `DB_PASS` | MySQL password | `secure_password` |
| `JWT_SECRET` | Secret key for JWT signing (min 64 bytes) | `openssl rand -base64 64` |
| `VITE_API_URL` | API base URL for frontend | `https://api.yourdomain.com/api` |

## CI/CD Pipeline

The `.github/workflows/ci-cd.yml` pipeline handles:
- **CI** — Builds and tests backend & frontend on every pull request
- **CD** — Builds Docker images and pushes to GitHub Container Registry (GHCR) on merge to `main`

## License

This project is open source. See the repository for license details.
