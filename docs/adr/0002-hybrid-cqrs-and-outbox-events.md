# ADR-0002: Hybrid CQRS, In-Process Domain Events, and Dormant Transactional Outbox

- **Status:** Accepted
- **Date:** 2026-11-09
- **Enforcement:** `AbstractBusinessLogic`, `AbstractDomainEventListener`, `AbstractProjector`, `OutboxEventListener`, `OutboxRelayScheduler`

## Context
Carizmi requires:
1. **Immediate transactional consistency** for critical cross-domain side effects (e.g., recording a registration fee payment must activate a `PENDING` member and open a membership period in the same database transaction).
2. **Eventual consistency** for read-side aggregations (e.g., rebuilding dashboard metrics snapshots after writes without blocking the HTTP thread).
3. **Optional external broker delivery** (Google Cloud Pub/Sub) without requiring an external message broker in standard single-instance nonprofit deployments.

## Decision
Use `DomainEventPublisher` (backed by Spring's `ApplicationEventPublisher`) inside `AbstractBusinessLogic` with three consumer models:
1. **Synchronous In-Transaction Listeners (`AbstractDomainEventListener`)**: Uses `@EventListener` (synchronous execution inside the active transaction). If the listener fails, the originating write transaction rolls back atomically.
2. **Asynchronous Read-Side Projectors (`AbstractProjector`)**: Uses `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` + `@Async` (with `@Transactional(propagation = Propagation.REQUIRES_NEW)` on `rebuildProjection()`).
3. **Dormant Transactional Outbox (`OutboxEventListener` + `OutboxRelayScheduler`)**: Guarded by `@ConditionalOnProperty(name = "carizmi.outbox.relay.enabled", havingValue = "true", matchIfMissing = false)`. Disabled by default (`false` in `application.yml`). Note: Carizmi does **not** use Apache Kafka; the outbox targets Google Cloud Pub/Sub when enabled.

## Consequences
- **Positive:** Zero external broker overhead in standard deployments while maintaining clean CQRS read/write separation and a production-ready outbox table (`outbox_event`).
- **Trade-off:** Contributors must choose the right base class (`AbstractDomainEventListener` vs. `AbstractProjector`) depending on whether same-transaction atomicity or async after-commit projection is required.