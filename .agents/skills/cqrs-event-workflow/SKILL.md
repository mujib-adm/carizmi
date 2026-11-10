---
name: cqrs-event-workflow
description: Implement synchronous in-transaction domain event listeners (AbstractDomainEventListener) or asynchronous CQRS read-model projectors (AbstractProjector) in Carizmi.
---

# CQRS & Domain Event Workflow Skill (`cqrs-event-workflow`)

Use this skill when implementing cross-domain reactions to entity writes (`CREATED`, `UPDATED`, `DELETED`) or building/updating read-model projections (`*SnapshotVO`).

## 1. Choosing the Right Event Consumer Pattern

Consult [`ADR-0002`](../../../docs/adr/0002-hybrid-cqrs-and-outbox-events.md) and [`docs/ARCHITECTURE.md` (Sections 6–7)](../../../docs/ARCHITECTURE.md):

| Requirement | Base Class to Extend | Transaction & Thread Model | Reference Archetype |
| :--- | :--- | :--- | :--- |
| **Atomic cross-domain write** (if the reaction fails, the originating save must roll back) | [`AbstractDomainEventListener`](../../../framework/src/main/java/io/carizmi/framework/event/AbstractDomainEventListener.java) | Synchronous `@EventListener` in the **same transaction** and thread | [`MemberActivationListener.java`](../../../backend/src/main/java/io/carizmi/domain/membership/listener/MemberActivationListener.java) |
| **Read-side CQRS projection / snapshot** (eventual consistency; must never slow down or roll back the write) | [`AbstractProjector`](../../../framework/src/main/java/io/carizmi/framework/projection/AbstractProjector.java) | `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async` | [`DashboardProjector.java`](../../../backend/src/main/java/io/carizmi/domain/platform/service/DashboardProjector.java) |
| **External broker delivery** (dormant by default; enabled via `carizmi.outbox.relay.enabled=true`) | [`OutboxEventListener`](../../../backend/src/main/java/io/carizmi/infrastructure/outbox/OutboxEventListener.java) | `@TransactionalEventListener(phase = BEFORE_COMMIT)` persists `OutboxEventVO`; polled by `OutboxRelayScheduler` | [`infrastructure/outbox/`](../../../backend/src/main/java/io/carizmi/infrastructure/outbox/) |

---

## 2. Step-by-Step Implementation Rules

### Step 1: Verify Source Aggregate Publishes Events
Ensure the source entity's `*AbstractBL` overrides `publishesDomainEvents()` to return `true`.

### Step 2A: Implementing a Synchronous Listener (`AbstractDomainEventListener`)
1. Place in `io.carizmi.domain.<context>.listener.<Name>Listener`.
2. Annotate with `@Component` and extend `AbstractDomainEventListener`.
3. Return the target aggregate simple class names in `supportedAggregateTypes()` (e.g., `Set.of(PaymentVO.class.getSimpleName())`).
4. Implement `protected void onEvent(DomainEvent<?> event)`.
5. Inject target domain services by **interface** only—never inject another domain's `JpaRepository`.

### Step 2B: Implementing an Async Projector (`AbstractProjector`)
1. Place in `io.carizmi.domain.<context>.service.<Name>Projector`.
2. Annotate with `@Component` and extend `AbstractProjector`.
3. Return trigger aggregate names in `supportedAggregateTypes()`.
4. Annotate `rebuildProjection()` with `@Override @Transactional(propagation = Propagation.REQUIRES_NEW)` so the async thread opens a fresh transaction after the write transaction commits.
5. Use **Jackson 3 (`tools.jackson.databind.ObjectMapper`)** for any JSON snapshot serialization—never `com.fasterxml.jackson.databind.ObjectMapper`.
6. Persist the snapshot through its `@RepositoryOwnerFor`-annotated snapshot service interface.

---

## 3. Mandatory Verification
Write a Spock unit spec (`*Spec.groovy` in `backend/src/test/java/`) covering both matching and non-matching `supportedAggregateTypes()` and error handling, then run:
```bash
./mvnw test -pl framework,backend
```