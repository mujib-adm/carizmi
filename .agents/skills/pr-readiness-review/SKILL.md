---
name: pr-readiness-review
description: Run Carizmi's pre-PR architectural, security, contract-drift, and test verification checklist before completing a task or opening a pull request.
---

# PR Readiness Review Skill (`pr-readiness-review`)

Use this skill before completing any multi-file implementation task or when asked to review changes for architectural and CI/CD readiness.

## 1. Git Hygiene & Immutability Checks
```bash
# 1. Inspect working tree status
git status --short

# 2. Verify V1 baseline migration was not modified
git diff --exit-code backend/src/main/resources/db/migration/V1__DB_Schema_Baseline.sql
```
- Confirm no secrets (`.env`, `secrets/`), temporary debug files, or `generated-changelog.xml` are staged.
- Confirm no committed `V*.sql` migration files in `backend/src/main/resources/db/migration/` were modified.

## 2. Architectural & Security Verification (Backend)
Check modified Java files against Carizmi's non-negotiable invariants (enforced by `DomainLogicOwnershipRulesSpec`, `RepositoryOwnershipRulesSpec`, and `LayeredAndBoundaryRulesSpec`):
- [ ] **No Direct `repo.save()`**: All entity writes go through `AbstractBusinessLogic` (`add`, `update`, `delete`).
- [ ] **Interface Injection Only**: All injected domain services use interfaces, never concrete `*Impl` classes.
- [ ] **Jackson 3 Only**: Serialization imports use `tools.jackson.databind.*` and `tools.jackson.core.*`, never `com.fasterxml.jackson.databind.*` or `com.fasterxml.jackson.core.*`.
- [ ] **DTO Enum Annotations**: Every enum referenced by a DTO has `@Schema(enumAsRef = true)`.
- [ ] **Endpoint Security**: Every non-public controller endpoint has `@IsAdmin`, `@IsAdminOrManager`, or `@IsAuthenticated`.

Run the full backend unit and ArchUnit suite:
```bash
./mvnw test -pl framework,backend
```

## 3. Frontend & API Contract Verification
If any `@RestController`, DTO, or frontend file was modified:
- [ ] If backend endpoints/DTOs changed, `frontend/src/api/openapi.json` has been regenerated (`./mvnw verify -pl framework,backend -DskipTests`) and `npm --prefix frontend run generate:api` was executed.
- [ ] No manual edits were made directly inside `frontend/src/api/generated/**`.

Run the frontend verification suite:
```bash
npm --prefix frontend run type-check
npm --prefix frontend run lint
npm --prefix frontend run test
```
*(If formatting modified frontend files, run Prettier only on the specific files you changed: `npx --prefix frontend prettier --check <changed-files>`—never run `npm run format` across the entire repo without asking.)*