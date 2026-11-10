---
name: new-domain-module
description: Scaffold or extend a backend domain entity (ValueObject, JpaRepository, sealed AbstractBL, final Impl, DomainValidator, DTOs/Transformers, Controller, and Spock unit specs) in compliance with Carizmi's 4-layer ownership and ArchUnit rules.
---

# New Domain Module Skill (`new-domain-module`)

Use this skill whenever creating or extending a persistent entity (`*VO`), repository, or domain service in `backend/src/main/java/io/carizmi/domain/<context>/`.

## 1. Authoritative Reference Docs (Read First)
- **Backend Rules**: [`backend/AGENTS.md`](../../../backend/AGENTS.md)
- **Step-by-Step Code Templates**: [`docs/ARCHITECTURE.md` (Section 10 — How to Create a New Domain Module)](../../../docs/ARCHITECTURE.md#10-how-to-create-a-new-domain-module)
- **ArchUnit Enforcement**:
  - [`DomainLogicOwnershipRulesSpec.groovy`](../../../backend/src/test/java/io/carizmi/archunit/DomainLogicOwnershipRulesSpec.groovy)
  - [`RepositoryOwnershipRulesSpec.groovy`](../../../backend/src/test/java/io/carizmi/archunit/RepositoryOwnershipRulesSpec.groovy)
  - [`LayeredAndBoundaryRulesSpec.groovy`](../../../backend/src/test/java/io/carizmi/archunit/LayeredAndBoundaryRulesSpec.groovy)

---

## 2. Implementation Checklist
1. **Determine Service Type**:
   - **Entity-Owning Domain Service (`domain/<context>/service/*Impl`)**: Owns a single `*VO` and `*Repository` 1:1 via `sealed *AbstractBL`. Follow Steps 1–9 in [`docs/ARCHITECTURE.md` §10](../../../docs/ARCHITECTURE.md#10-how-to-create-a-new-domain-module).
   - **Cross-Domain Orchestration Service (`domain/<context>/service/impl/*ServiceImpl`)**: Coordinates multiple domain service interfaces and owns **no** `JpaRepository` or `*VO`.
2. **Verify Key Invariants**:
   - [ ] `<Entity>VO` extends `ValueObject`, uses `Integer` `@Id`, and implements `getTableName()`.
   - [ ] `<Entity>Repository` ends with `Repository` and is accessed **only** by `<Entity>AbstractBL` / `<Entity>Impl`.
   - [ ] `<Entity>AbstractBL` is `public abstract sealed`, annotated with `@DomainLogicFor(<Entity>VO.class)` and `@RepositoryOwnerFor(<Entity>Repository.class)`, and `permits <Entity>Impl`.
   - [ ] `<Entity>Impl` is `@Service public final class`, implements the `<Entity>` interface, and routes all writes through `add(vo)` / `update(vo)` / `delete(vo)`.
   - [ ] `<Entity>Validator` implements `DomainValidator<<Entity>VO>` and accumulates messages on `vo` without throwing.
   - [ ] All injected domain collaborations use **service interfaces** (never concrete `*Impl` classes).
   - [ ] Any enum referenced by a DTO has `@Schema(enumAsRef = true)`.
   - [ ] Controller endpoints declare `@IsAdmin`, `@IsAdminOrManager`, or `@IsAuthenticated` and wrap responses via `ResponseUtils`.
   - [ ] Spock unit specs (`*Spec.groovy`) are created under `backend/src/test/java/io/carizmi/domain/<context>/`.

---

## 3. Mandatory Verification Commands
```bash
./mvnw test -pl framework,backend -Dtest=DomainLogicOwnershipRulesSpec,RepositoryOwnershipRulesSpec,LayeredAndBoundaryRulesSpec -Dsurefire.failIfNoSpecifiedTests=false
```