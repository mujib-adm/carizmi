# Architecture Decision Records (ADRs)

This directory contains lightweight Architecture Decision Records following the [Michael Nygard ADR standard](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions).

## Governance & Lifecycle Rules
1. **Scope (Architecture Only, Never Features):** ADRs record cross-cutting, non-standard, or foundational architectural decisions (e.g., why `proxy-target-class: false` is enforced, or why Liquibase and Flyway coexist). **Never** create ADRs for domain features, endpoints, or business use cases—document those in class/method Javadocs.
2. **Immutability (Append-Only):** Once an ADR is `Accepted`, treat it as a point-in-time historical record. If an architectural decision changes in the future, do not rewrite the original ADR; change its status to `Superseded by ADR-XXXX` and add a new ADR.
3. **On-Demand Context:** AI agents and contributors should follow [`../../AGENTS.md`](../../AGENTS.md) for active rules and consult ADRs on demand when investigating *why* a structural guardrail exists.

## Index

| ADR | Title | Status |
| :--- | :--- | :--- |
| [ADR-0001](0001-sealed-domain-logic-ownership.md) | 1:1 Sealed Domain Logic Ownership & JDK Dynamic Proxies | Accepted |
| [ADR-0002](0002-hybrid-cqrs-and-outbox-events.md) | Hybrid CQRS, In-Process Domain Events, and Dormant Transactional Outbox | Accepted |
| [ADR-0003](0003-hybrid-liquibase-diff-flyway-execution.md) | Hybrid Liquibase Diff Generation + Flyway Runtime Execution | Accepted |
| [ADR-0004](0004-spring-boot-4-jackson-3-migration.md) | Spring Boot 4 & Jackson 3 (`tools.jackson`) Migration | Accepted |