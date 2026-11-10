## Summary
<!-- Briefly describe what this PR changes and why. -->

## Architectural & Quality Checklist
Please verify that your changes satisfy Carizmi's engineering guardrails (see [`AGENTS.md`](../AGENTS.md) and [`docs/ARCHITECTURE.md`](../docs/ARCHITECTURE.md)):

### Backend & Framework (`backend/**`, `framework/**`)
- [ ] **Write Lifecycle**: All entity persistence flows through `AbstractBusinessLogic` (`add`, `update`, `delete`); no direct `repo.save()` calls.
- [ ] **1:1 Ownership & Proxies**: Services are injected by **interface**, never by concrete `*Impl` class or foreign `*Repository`.
- [ ] **Jackson 3 (`tools.jackson`)**: Uses `tools.jackson.databind.ObjectMapper` and `tools.jackson.core.JacksonException`; zero `com.fasterxml.jackson.databind.*` imports.
- [ ] **Endpoint Security**: Every new/modified controller endpoint declares `@IsAdmin`, `@IsAdminOrManager`, or `@IsAuthenticated`.
- [ ] **Database Migrations**: `V1__DB_Schema_Baseline.sql` and existing `V*.sql` files are untouched; any schema change uses a new `VYYYYMMDDHHMMSS__*.sql` migration.
- [ ] **Tests**: Spock unit specs (`*Spec.groovy` in `src/test/java/`) and ArchUnit specs (`DomainLogicOwnershipRulesSpec`, `RepositoryOwnershipRulesSpec`, `LayeredAndBoundaryRulesSpec`) pass (`./mvnw test -pl framework,backend`).

### API Contract & Frontend (`frontend/**`)
- [ ] **OpenAPI & Orval Sync**: If any controller, DTO, or DTO enum (`@Schema(enumAsRef = true)`) changed, `frontend/src/api/openapi.json` was regenerated and committed, and `npm --prefix frontend run generate:api` was verified.
- [ ] **Frontend Verification**: `npm --prefix frontend run type-check`, `lint`, and `test` pass with zero errors.
- [ ] **Theme & Responsiveness**: Uses CSS custom properties (`var(--...)`) for Light/Dark mode parity and supports `<576px` mobile viewports.

### Documentation & AI Readiness
- [ ] Updated [`docs/ARCHITECTURE.md`](../docs/ARCHITECTURE.md), [`docs/adr/`](../docs/adr/README.md), or [`AGENTS.md`](../AGENTS.md) if foundational architectural patterns changed.