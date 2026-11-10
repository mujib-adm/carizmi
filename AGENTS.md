# Carizmi — AI Agent Engineering Guide

Carizmi is a production-grade open-source community management platform for nonprofit organizations. It runs a modular monolith backend (**Java / Spring Boot / MySQL / Redis**) paired with a **React / TypeScript / Vite / Ant Design** SPA.

> **Source of Truth for Versions & Dependencies:** Never assume or hardcode library/plugin versions. Inspect [`pom.xml`](pom.xml), [`framework/pom.xml`](framework/pom.xml), [`backend/pom.xml`](backend/pom.xml), and [`frontend/package.json`](frontend/package.json) directly.

## Scoped Instructions & Reference Docs
- **Backend & Framework (`backend/**`, `framework/**`)**: Read [`backend/AGENTS.md`](backend/AGENTS.md) before modifying Java or Spock files.
- **Frontend (`frontend/**`)**: Read [`frontend/AGENTS.md`](frontend/AGENTS.md) before modifying TypeScript, React, or CSS files.
- **Architecture & Lifecycle**: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) and ADRs in [`docs/adr/README.md`](docs/adr/README.md)
- **API Contract Pipeline**: [`docs/API_GENERATION.md`](docs/API_GENERATION.md)
- **Database Migrations**: [`docs/DATABASE_MIGRATION.md`](docs/DATABASE_MIGRATION.md)

## Build Order & Module Topology
Maven builds three modules in strict order (defined in root [`pom.xml`](pom.xml)):
1. `framework` — Core write/read lifecycle (`AbstractBusinessLogic`, `ValueObject`, `DomainValidator`, `DomainLogicProcessor`, `DomainEvent`, `AbstractDomainEventListener`, `AbstractProjector`, `GlobalResponse`, `GlobalExceptionHandler`).
2. `backend` — Domain bounded contexts (`backend/src/main/java/io/carizmi/domain/*`), infrastructure (`backend/src/main/java/io/carizmi/infrastructure/*`), Spock unit & integration specs, and `OpenApiLocalGenerator`.
3. `frontend` — React SPA consuming `frontend/src/api/openapi.json` via Orval (`frontend/src/api/generated/`).

## Verified Build & Verification Commands

> **macOS Non-Interactive Shell Note:** If `java` is not on the default non-interactive `PATH`, resolve the active JDK configured in `pom.xml` via `export JAVA_HOME=$(/usr/libexec/java_home)` (or pass `JAVA_HOME=...`).

### Fast Backend Loop (No Docker Required)
```bash
# Compile framework + backend without running tests or OpenAPI generation
./mvnw test-compile -pl framework,backend

# Run all Spock unit & ArchUnit specs (no Docker needed)
./mvnw test -pl framework,backend

# Run a single Spock spec across the reactor
./mvnw test -pl framework,backend -Dtest=LayeredAndBoundaryRulesSpec -Dsurefire.failIfNoSpecifiedTests=false

# Package/install without triggering Testcontainers OpenApiLocalGenerator
./mvnw install -DskipTests -DskipApiGen=true
```
> **Important:** `-DskipTests` skips Surefire/Failsafe but **does not** skip `OpenApiLocalGenerator` (`exec-maven-plugin` in `integration-test`). Always pass `-DskipApiGen=true` when skipping tests or running without Docker.

### Full Integration & API Contract Loop (Requires Docker)
```bash
# Run backend unit tests + Testcontainers IT specs + regenerate frontend/src/api/openapi.json
./mvnw verify -pl framework,backend

# Run a single integration test via Testcontainers
./mvnw test-compile failsafe:integration-test failsafe:verify -pl backend -Dit.test=AuthApiITSpec -DskipApiGen=true
```

### Fast Frontend Loop (`frontend/`)
```bash
npm --prefix frontend run generate:api   # Regenerate src/api/generated/ from openapi.json (Orval + clean-orval.mjs)
npm --prefix frontend run type-check     # tsc --noEmit
npm --prefix frontend run lint           # ESLint
npm --prefix frontend run test           # Vitest (single run)
```

## Non-Negotiable Architectural Invariants
1. **Never Call `repo.save()` Directly**: All entity writes (`add`, `update`, `delete`) must flow through `AbstractBusinessLogic` (`super.add(vo)`, `super.update(vo)`).
2. **1:1 Entity & Repository Ownership**: Every `ValueObject` (`Integer` `@Id`) is owned by exactly one `sealed *AbstractBL` (`@DomainLogicFor`, `@RepositoryOwnerFor`) and one `final *Impl` service. Never inject another domain's `JpaRepository`; inject its service interface instead.
3. **JDK Dynamic Proxies (`spring.aop.proxy-target-class: false`)**: Always inject domain services by **interface**, never by concrete `*Impl` class.
4. **Jackson 3 (`tools.jackson`) vs. Jackson 2 Annotations**: Use **Jackson 3 (`tools.jackson.databind.ObjectMapper`, `tools.jackson.core.*`)** for serialization alongside **Jackson 2 annotations (`com.fasterxml.jackson.annotation.*`)**. Never import `com.fasterxml.jackson.databind.*` or `com.fasterxml.jackson.core.*`.
5. **API Contract Sync**: Any change to a `@RestController`, DTO, or DTO-exposed enum (`@Schema(enumAsRef = true)`) requires regenerating and committing `frontend/src/api/openapi.json` (requires Docker) and running `npm --prefix frontend run generate:api`. Never hand-edit `frontend/src/api/generated/**`.
6. **Immutable Database Migrations**: Never modify `V1__DB_Schema_Baseline.sql` or any committed `V*.sql` file in `backend/src/main/resources/db/migration/` (enforced by CI). Always create a new `VYYYYMMDDHHMMSS__<description>.sql` migration.

## Security Redlines & "Ask Before" Boundaries
- **Secrets & Env**: Never read, print, or commit `.env` or `secrets/**`.
- **Endpoint Authorization**: Every controller endpoint (except explicitly public endpoints such as `/auth/refresh`) must declare `@IsAdmin`, `@IsAdminOrManager`, or `@IsAuthenticated`.
- **SQL & Schema**: Never use raw SQL concatenation or `ddl-auto: update` in runtime profiles (`validate` is enforced in `application.yml`).
- **Ask the Maintainer Before**:
  - Committing, pushing, rebasing, force-pushing, or opening/merging PRs.
  - Modifying `.github/workflows/**`, `Dockerfile*`, `docker-compose*.yml`, cloud deployment configs, or `application-prod.yml`.
  - Modifying `backend/src/main/java/io/carizmi/infrastructure/security/**` or security, CORS, JWT, rate-limit, or cookie settings in `application*.yml`.
  - Adding, removing, or upgrading dependencies in `pom.xml` or `package.json`.
  - Deleting tracked files, dropping database schemas/volumes (`docker compose down -v`), or running `MigrationDdlGenerator` against any database.
  - Running repo-wide code formatters (`npm run format`) or codemods across untouched files.