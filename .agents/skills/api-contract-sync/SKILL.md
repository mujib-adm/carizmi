---
name: api-contract-sync
description: Synchronize backend Spring MVC controllers and DTOs with frontend/src/api/openapi.json and regenerate Orval TypeScript API clients (src/api/generated/).
---

# API Contract Synchronization Skill (`api-contract-sync`)

Use this skill whenever adding, removing, or modifying a `@RestController` endpoint, request/response DTO, or enum exposed over the REST API.

## 1. Authoritative Reference Docs & Tooling
- **Pipeline Documentation**: [`docs/API_GENERATION.md`](../../../docs/API_GENERATION.md)
- **Backend Extractor**: [`OpenApiLocalGenerator.java`](../../../backend/src/test-integration/java/io/carizmi/integration/tooling/OpenApiLocalGenerator.java)
- **Frontend Generator & Post-Processor**: [`frontend/orval.config.ts`](../../../frontend/orval.config.ts) and [`frontend/clean-orval.mjs`](../../../frontend/clean-orval.mjs)

---

## 2. Step-by-Step Procedure

### Step 1: Verify ArchUnit DTO & Controller Rules
Ensure every enum referenced by a DTO has `@Schema(enumAsRef = true)` and controller boundaries are satisfied:
```bash
./mvnw test -pl framework,backend -Dtest=LayeredAndBoundaryRulesSpec -Dsurefire.failIfNoSpecifiedTests=false
```

### Step 2: Regenerate `frontend/src/api/openapi.json` (Requires Docker)
`OpenApiLocalGenerator` boots the backend against Testcontainers MySQL during Maven's `integration-test` phase and writes `frontend/src/api/openapi.json`:
```bash
./mvnw verify -pl framework,backend -DskipTests
```
> **Note:** `-DskipTests` skips Surefire/Failsafe tests for speed while still executing `exec-maven-plugin`'s `generate-openapi-json` goal. Do **not** pass `-DskipApiGen=true` here. If Docker is not running, **never hand-edit `openapi.json` or `frontend/src/api/generated/**`**—ask the maintainer to start Docker or run contract generation.

### Step 3: Regenerate Frontend TypeScript Clients via Orval
```bash
npm --prefix frontend run generate:api
```

### Step 4: Verify Frontend Type-Checking, Linting & Tests
```bash
npm --prefix frontend run type-check
npm --prefix frontend run lint
npm --prefix frontend run test
```