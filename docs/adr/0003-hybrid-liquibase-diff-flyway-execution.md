# ADR-0003: Hybrid Liquibase Diff Generation + Flyway Runtime Execution

- **Status:** Accepted
- **Date:** 2026-11-09
- **Enforcement:** `backend/src/main/resources/application.yml`, `JpaLiquibaseConfig.java`, `MigrationDdlGenerator.java`, `.github/workflows/ci-cd.yml`

## Context
Both `liquibase-core` (`compile` scope) and `flyway-core` + `flyway-mysql` (`runtime` scope) exist in `backend/pom.xml`. Without context, contributors or AI agents may assume one is a leftover dependency and attempt to remove it or enable Liquibase at runtime.

## Decision
Carizmi intentionally uses a **hybrid two-tool database migration workflow** (documented in [`docs/DATABASE_MIGRATION.md`](../DATABASE_MIGRATION.md)):
1. **Flyway (Runtime Execution & Schema Verification)**: Enabled at application startup (`spring.flyway.enabled: true`) alongside `spring.jpa.hibernate.ddl-auto: validate`. Flyway executes immutable, versioned SQL scripts in `backend/src/main/resources/db/migration/` (`V1__DB_Schema_Baseline.sql` and `VYYYYMMDDHHMMSS__*.sql`).
2. **Liquibase (Build-Time DDL Diff Tooling Only)**: Disabled at runtime (`spring.liquibase.enabled: false` in `application.yml`). Liquibase is used programmatically under `@Profile("liquibase-diff")` by [`JpaLiquibaseConfig`](../../backend/src/main/java/io/carizmi/infrastructure/bootstrap/JpaLiquibaseConfig.java) and [`MigrationDdlGenerator`](../../backend/src/main/java/io/carizmi/infrastructure/bootstrap/MigrationDdlGenerator.java) to diff the Spring/Hibernate JPA entity model (`hibernate:spring:io.carizmi.domain`) against a local MySQL schema and output a timestamped `VYYYYMMDDHHMMSS__*.sql` file for Flyway.
3. **Baseline Immutability**: CI (`ci-cd.yml`) fails the build if `V1__DB_Schema_Baseline.sql` is modified on any branch or PR.

## Consequences
- **Positive:** Combines Hibernate-to-DB diff automation with deterministic, forward-only Flyway SQL execution in production.
- **Pitfall to Avoid:** Never remove `liquibase-core` (breaks `MigrationDdlGenerator`), never enable Liquibase at runtime, and never edit existing `V*.sql` files in `backend/src/main/resources/db/migration/`.