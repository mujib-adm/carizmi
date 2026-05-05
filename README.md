# Carizmi Community Platform

An open-source, full-stack community management platform built to deliver comprehensive solutions for **nonprofit organizations** and **task automation** for small to mid-size businesses.

## Overview

Carizmi Community Platform is designed to be a modular, extensible foundation that organizations can adopt and customize to manage their community operations, from membership records and financial tracking to automated workflows and reporting.

### Our Mission
This open-source initiative was born from a desire to support **non-profit organizations dedicated to community service**. Many of these organizations operate on tight budgets and lack the resources to afford complex, customized software development. Carizmi provides them with a premium, enterprise-grade platform completely free of charge.

While the currently implemented domains (Membership, Finance) are intentionally limited in scope, the primary focus of this initial release was to engineer a **rock-solid architectural foundation**. By establishing strict CQRS patterns, event-driven workflows, and scalable security, the platform is perfectly positioned for future expansion. The actual scope of the application will continue to evolve alongside our growing community of open-source contributors.

### Proven in Production
We believe that real-world deployment is the ultimate test of software quality. Carizmi is currently running in production, provided 100% free of charge to our first beneficiary: a Minnesota-based community organization.

- **Beneficiary:** Sofumar Community of Minnesota
- **Live Portal:** [portal.sofumarcommunityofmn.org](https://portal.sofumarcommunityofmn.org/)

*(Are you a non-profit using Carizmi? We would love to feature you here!)*

## Documentation

The repository contains comprehensive documentation covering the platform's architecture, scaling strategies, and operational guidelines.

| Document | Description |
|----------|-------------|
| **[Backend Architecture](docs/ARCHITECTURE.md)** | Overview of the core Java/Spring framework, including domain validation, event lifecycles, and structural enforcement patterns. |
| **[UI/UX Architecture](docs/UI_UX_ARCHITECTURE.md)** | Frontend design system, responsive theming engine, and React component architecture. |
| **[API Generation Pipeline](docs/API_GENERATION.md)** | Automated extraction of OpenAPI contracts and type-safe TypeScript client generation. |
| **[Architectural Scale Upgrade](docs/ARCHITECTURAL_SCALE_UPGRADE.md)** | **[Engineering Highlight]** Comprehensive breakdown of the platform's evolution into a resilient Event-Driven Architecture using CQRS and Outbox patterns. |
| **[Database Migration Strategy](docs/DATABASE_MIGRATION.md)** | Strategy for managing database schema evolution using immutable, timestamp-based migrations. |
| **[DevSecOps & CI/CD Pipeline](docs/DEVSECOPS_PIPELINE.md)** | Enterprise GitHub Actions workflow covering continuous integration, security audits, and automated GCP Cloud Run deployments. |
| **[Monitoring & Alerting](docs/MONITORING.md)** | Application observability via Spring Actuator, uptime monitoring, and system health checks. |
| **[Backup Strategy](docs/BACKUP.md)** | Automated database backup workflows and disaster recovery procedures. |

## Core Platform Capabilities

- **Membership Management** — End-to-end member lifecycle management, including registration workflows, secure profiles, status tracking, and directory lookup.
- **Financial Operations** — Comprehensive financial oversight featuring payment tracking, expense management, automated quarterly fee checklists, and collection summaries.
- **Identity & Access Control** — Secure, stateless JWT authentication coupled with granular role-based authorization and automated administrator provisioning.
- **API-First Integration** — Contract-driven development powered by auto-generated OpenAPI schemas and fully type-safe TypeScript API clients.
- **Event-Driven Resilience** — High-performance asynchronous metrics projection and transactional outbox patterns guaranteeing data consistency under load.

## Tech Stack

| Layer | Technologies |
|-------|-------------|
| **Backend** | Java 21, Spring Boot 3, Stateless JWT |
| **Frontend** | React 18, TypeScript, Vite, Ant Design, Nginx |
| **Database** | MySQL 8.0 |
| **Cache** | Redis |
| **Infrastructure** | Docker, Docker Compose, GitHub Actions (CI/CD) |
| **API Automation** | Springdoc OpenAPI, Orval, Axios |

## Getting Started

### Prerequisites
- **[Java 21](https://www.oracle.com/java/technologies/downloads/#java21)** (or any compatible OpenJDK distribution)
- **[Docker & Docker Compose](https://www.docker.com/products/docker-desktop/)** (Docker Desktop recommended for macOS/Windows)
- **Node.js 24+** (Auto-installed and managed via the frontend Maven plugin—no local installation required)

### Local Development (Full Stack via Docker)

To bring up the entire application locally in a Docker environment, simply execute the following commands after ensuring your `.env` file is configured:

```bash
# Compile and package the application
./mvnw clean install

# Build containers and bring up the full stack
docker compose build --no-cache && docker compose up
```

### Local Frontend Development (Standalone)

If you are only working on UI/UX and need rapid hot-reloading without rebuilding containers, you can run the frontend independently:

```bash
# Run frontend in a separate terminal
cd frontend && npm install && npm run dev
```

**Accessing the Platform:**
- **Full Stack Frontend (Docker):** http://localhost:8081
- **Full Stack Backend API (Docker):** http://localhost:8080/api
- **Standalone Frontend (npm run dev):** http://localhost:5173/

### Local Environment Setup

The repository includes a [`.env.example`](.env.example) template with all required configuration variables. Copy it to create your local `.env` file before running the platform:

```bash
# 1. Create your local environment file from the template
cp .env.example .env

# 2. Generate a secure JWT signing key and update .env
openssl rand -base64 64
# Copy the output and paste it as the JWT_SECRET value in .env

# 3. (Optional) Customize the remaining values as needed
```

> **⚠️ Important:** The `.env` file contains sensitive credentials and is excluded from version control via `.gitignore`. Never commit it to the repository. All secret values (database passwords, JWT keys) should be replaced with strong, unique values for any non-local environment.

### Environment Variables

The following environment variables configure the platform for local development via Docker Compose and production deployment.

#### 1. Application & Runtime
| Variable | Description | Default |
|----------|-------------|---------|
| `APP_PORT` | HTTP port the backend server listens on | `8080` |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile (`dev` or `prod`) | `dev` |
| `JAVA_OPTS` | JVM memory and tuning flags | `-Xms256m -Xmx512m` |

#### 2. Database
| Variable | Description | Default |
|----------|-------------|---------|
| `DB_NAME` | MySQL database schema name | `carizmi` |
| `DB_USER` | MySQL application username | `carizmi` |
| `DB_PASS` | MySQL application password | `carizmi_pwd` |
| `DB_ROOT_PASS` | MySQL root administrator password | `root_pwd_change_me` |
| `DB_URL` | Full JDBC connection string | `jdbc:mysql://db:3306/${DB_NAME}?useSSL=false...` |

#### 3. Redis (Session & Cache Store)
| Variable | Description | Default |
|----------|-------------|---------|
| `REDIS_HOST` | Redis server hostname | `redis` |
| `REDIS_PORT` | Redis server port | `6379` |

#### 4. Default Admin User (Auto-Provisioned on First Startup)
| Variable | Description | Default |
|----------|-------------|---------|
| `ADMIN_DEFAULT_FIRSTNAME` | First name of the initial admin user | `System` |
| `ADMIN_DEFAULT_LASTNAME` | Last name of the initial admin user | `Administrator` |
| `ADMIN_DEFAULT_EMAIL` | Email address of the initial admin user | `admin@mail.com` |
| `ADMIN_DEFAULT_USERNAME`| Login username for the initial admin | `admin` |

#### 5. Security
| Variable | Description | Default |
|----------|-------------|---------|
| `JWT_SECRET` | Secret key for HS512 JWT signing (min 64 bytes) | *(must be generated)* |

#### 6. Frontend & CORS
| Variable | Description | Default |
|----------|-------------|---------|
| `VITE_API_URL` | Base URL for the frontend API client | `http://localhost:8080/api` |
| `APP_CORS_ALLOWED_ORIGINS` | Comma-separated list of allowed CORS origins | `http://localhost:8081,http://localhost:5173` |

#### 7. Branding (Injected into Frontend at Container Startup)
| Variable | Description | Default |
|----------|-------------|---------|
| `APP_ORG_NAME` | Organization name used throughout the UI | `Your Organization Name` |
| `APP_HEADER_TITLE` | Main header title displayed in the portal | `YOUR ORG` |
| `APP_HEADER_SUBTITLE` | Header subtitle beneath the title | `COMMUNITY PORTAL` |
| `APP_LOGO_ALT` | Alt text for the organization logo | `Organization Logo` |
| `APP_COPYRIGHT` | Footer copyright notice | `© 2026 Your Organization Name.` |

## License

TBD — We are currently evaluating open-source licenses that best align with our mission to provide free software to non-profits while protecting the integrity of the platform. We will update this section once a decision has been made.
