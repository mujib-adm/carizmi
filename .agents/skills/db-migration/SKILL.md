---
name: db-migration
description: Generate, inspect, and validate forward-only timestamped Flyway SQL migrations (VYYYYMMDDHHMMSS__*.sql) for Carizmi while preserving V1__DB_Schema_Baseline.sql immutability.
---

# Database Migration Skill (`db-migration`)

Use this skill whenever adding or modifying JPA `@Entity` tables, columns, indexes, foreign keys, or seed data in `backend/src/main/resources/db/migration/`.

## 1. Authoritative Reference Docs & Tooling
- **Migration Architecture & Workflow**: [`docs/DATABASE_MIGRATION.md`](../../../docs/DATABASE_MIGRATION.md) and [`ADR-0003`](../../../docs/adr/0003-hybrid-liquibase-diff-flyway-execution.md)
- **Diff Generator Tooling**:
  - [`MigrationDdlGenerator.java`](../../../backend/src/main/java/io/carizmi/infrastructure/bootstrap/MigrationDdlGenerator.java)
  - [`JpaLiquibaseConfig.java`](../../../backend/src/main/java/io/carizmi/infrastructure/bootstrap/JpaLiquibaseConfig.java)

---

## 2. Critical Safety & Immutability Rules
1. **Never Modify Existing Migrations**: `V1__DB_Schema_Baseline.sql` and all committed `V*.sql` files in `backend/src/main/resources/db/migration/` are strictly immutable (enforced by CI).
2. **Timestamped Naming**: Always create a new forward-only migration named `VYYYYMMDDHHMMSS__<Description>.sql`.
3. **Exact Physical Naming**: Hibernate uses `PhysicalNamingStrategyStandardImpl`. Java `@Table` and `@Column` names must match SQL identifiers character-for-character.
4. **Ask Before Running `MigrationDdlGenerator`**: `MigrationDdlGenerator` connects to a live MySQL instance (`localhost:3306/carizmi` configured in `JpaLiquibaseConfig`). Confirm with the maintainer that the local Docker database is running and safe to connect to before invoking it, or craft the forward-only `VYYYYMMDDHHMMSS__*.sql` migration directly and verify it via Testcontainers.
5. **Clean Up Temporary Artifacts**: Ensure `generated-changelog.xml` is removed and never staged.

---

## 3. Mandatory Verification Commands
```bash
# 1. Confirm V1__DB_Schema_Baseline.sql has zero modifications
git diff --exit-code backend/src/main/resources/db/migration/V1__DB_Schema_Baseline.sql

# 2. If Docker is running, verify Flyway migration + Hibernate ddl-auto:validate via Testcontainers
./mvnw test-compile failsafe:integration-test failsafe:verify -pl backend -Dit.test=AuthApiITSpec -DskipApiGen=true
```