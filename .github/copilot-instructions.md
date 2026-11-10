# Carizmi — GitHub Copilot Instructions

Canonical AI agent instructions live in [`AGENTS.md`](../AGENTS.md), [`backend/AGENTS.md`](../backend/AGENTS.md), and [`frontend/AGENTS.md`](../frontend/AGENTS.md) (keep this summary in sync with `AGENTS.md`). Follow these invariants at all times:

1. **Source of Truth for Stack & Versions**: Inspect [`pom.xml`](../pom.xml), [`backend/pom.xml`](../backend/pom.xml), and [`frontend/package.json`](../frontend/package.json) directly for exact dependency and tool versions.
2. **Never Call `repo.save()` Directly**: All entity persistence must go through `AbstractBusinessLogic` (`add`, `update`, `delete`).
3. **1:1 Ownership & Sealed Hierarchy**: Every `ValueObject` (`Integer` `@Id`) maps 1:1 to a `sealed *AbstractBL` (`@DomainLogicFor`, `@RepositoryOwnerFor`) and `final *Impl`. Orchestration services (`..service.impl.*ServiceImpl`) own no repository.
4. **JDK Dynamic Proxies (`proxy-target-class: false`)**: Always inject domain services by interface, never by concrete `*Impl` class.
5. **Jackson 3 vs. Jackson 2**: Import `tools.jackson.databind.*` and `tools.jackson.core.*` for serialization, and `com.fasterxml.jackson.annotation.*` for annotations. Never import `com.fasterxml.jackson.databind.*` or `com.fasterxml.jackson.core.*`.
6. **Layer Boundaries**: Services never import `ResponseEntity`, `GlobalResponse`, or `ResponseUtils`; Controllers never import `ValueObject` or `JpaRepository` and must declare `@IsAdmin`, `@IsAdminOrManager`, or `@IsAuthenticated`.
7. **OpenAPI & Orval**: Annotate all DTO-referenced enums with `@Schema(enumAsRef = true)`. Never hand-edit `frontend/src/api/generated/**`; regenerate `frontend/src/api/openapi.json` and run `npm --prefix frontend run generate:api`.
8. **Spock Tests in `src/test/java/`**: Backend unit (`*Spec.groovy`) and integration (`*ITSpec.groovy`) tests live in `src/test/java/` and `src/test-integration/java/`.
9. **Immutable Migrations & Safety**: Never modify `V1__DB_Schema_Baseline.sql` or committed `V*.sql` files; never read/commit `.env` or `secrets/`; ask the maintainer before modifying CI/Docker/prod configs, changing dependencies, or running destructive commands.