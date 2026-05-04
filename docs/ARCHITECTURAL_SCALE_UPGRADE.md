# Architectural Scale Upgrade v1: Resilience & Enforcement

This document summarizes the first major architectural upgrade executed post-production deployment for the Carizmi Community Platform. The objective of this upgrade was to eliminate technical debt, enforce strict domain boundaries, and transition the application from a tightly coupled synchronous monolith to an Event-Driven Architecture (EDA) capable of enterprise scaling.

---

## 1. Context & Objectives

As the platform's initial scope expanded, structural bottlenecks emerged:
- **Synchronous Coupling:** Business operations (like processing a payment) were synchronously computing dashboard metrics and triggering side-effects, leading to high latency and the risk of cascading transaction failures.
- **Architectural Drift:** Without strict compile-time or runtime enforcement, the generic framework (`AbstractBusinessLogic`) risked logic fragmentation as new contributors joined.
- **Proxy Constraints:** Reliance on CGLIB proxies created initialization overhead and prevented the use of `final` modifiers on core business logic classes.

To resolve these, a comprehensive architectural scale upgrade was designed and implemented following enterprise design patterns.

---

## 2. Fully Implemented Upgrades (In-Use)

The following upgrades have been deployed to production and are actively securing and scaling the application.

### A. Strict Framework Enforcement (Sealed & Final Classes)
- **What Changed:** The core framework was refactored to leverage JDK 21 `sealed` classes and `final` modifiers. The `AbstractBusinessLogic` layer was enhanced with a Spring `afterPropertiesSet()` runtime validator that cross-checks the `@DomainLogicFor` annotation against generic type parameters.
- **Why It Changed:** To mathematically enforce a strict 1-to-1 ownership between a `ValueObject` (Data) and its `BusinessLogic` implementation (Behavior). This entirely eliminates logic fragmentation, ensuring that only one specific service can ever mutate a specific domain entity.

### B. JDK Dynamic Proxies over CGLIB
- **What Changed:** The application property `spring.aop.proxy-target-class` was explicitly set to `false`, instructing Spring AOP to utilize standard JDK Dynamic Proxies instead of CGLIB class-extension proxies. 
- **Why It Changed:** Because the Carizmi codebase already strictly adheres to the "Program to an Interface" paradigm for loose coupling, CGLIB was an unnatural and unnecessary constraint. Transitioning to JDK proxies allowed us to mark all domain service implementations as `final` (which CGLIB prohibits), securing the domain layer from unintended inheritance.

### C. Command Query Responsibility Segregation (CQRS)
- **What Changed:** The synchronous computation of dashboard metrics was ripped out of the core domain logic. A new `AbstractProjector` layer was created to pre-compute reads asynchronously into a `dashboard_snapshot` table.
- **Why It Changed:** To decouple write-heavy transactional operations from read-heavy dashboard queries. Dashboard reads are now O(1) database lookups, entirely insulated from transactional spikes.

### D. Resilient In-Process Event Streams
- **What Changed:** The platform transitioned to Spring's `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`.
- **Why It Changed:** Previously, secondary side-effects could fail and roll back the primary database transaction. Now, side-effects (like dashboard projection) only execute *after* the core ACID transaction successfully commits, drastically reducing failure rates and improving UX latency.

---

## 3. Infrastructure-Ready Upgrades (Future Scalability)

The following architectures were designed and merged to support future hyper-scaling, acting as a bridge to external infrastructure when required.

### A. The Transactional Outbox Pattern
- **What Changed:** An `outbox_event` table and an `OutboxEventRepository` were created. Domain events are now persisted to this outbox table within the exact same ACID transaction as the business entity mutation. 
- **Why It Changed:** To solve the "dual-write" problem. While currently consumed by an in-process relay scheduler, this guarantees **at-least-once delivery** of events during network partitions. It is infrastructure-ready: if the application scales to microservices or requires Kafka/RabbitMQ, an external Debezium connector or polling relay can seamlessly tail this outbox table without changing a single line of business logic.
