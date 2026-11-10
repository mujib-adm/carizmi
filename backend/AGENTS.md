# Carizmi Backend & Framework — AI Agent Guide (`backend/**`, `framework/**`)

This file supplements root [`../AGENTS.md`](../AGENTS.md) with backend-specific rules for `backend/` and `framework/`. Inspect [`../pom.xml`](../pom.xml) and [`pom.xml`](pom.xml) for active Java, Spring, and dependency versions.

## 1. Package Discovery, Bounded Contexts & Layout (`io.carizmi.domain.<context>`)
Bounded contexts live under `backend/src/main/java/io/carizmi/domain/` (read class/method Javadocs and `ReferenceConstants` for specific business rules):
- `membership` — Community member directory (`MemberVO` is distinct from login `UserVO`), status lifecycle (`PENDING`/`ACTIVE`/`INACTIVE`), and active tenure episodes (`MemberMembershipPeriodVO`).
- `finance` — Dues/registration payments (`PaymentVO`), operational expenses (`ExpenseVO`), quarterly fee checklist matrix, and annual financial report calculation/snapshots.
- `identity` — Portal user accounts (`UserVO`), roles, JWT authentication, and Redis refresh-token rotation/blacklisting.
- `platform` — System settings (`SystemSettingsVO`), reference codes (`ReferenceVO`), fiscal baseline calculation (`YEARLY_BASELINE_{year}`), CQRS dashboard projections, and `YearEndScheduler`.

Each domain context follows this package layout:
```text
domain/<context>/
├── controller/       # @RestController; wraps results via ResponseUtils; never imports VOs or Repositories
├── data/
│   ├── dto/          # Request/Response DTOs (@Valid); all DTO-referenced enums MUST have @Schema(enumAsRef = true)
│   └── transformer/  # Stateless *DtoTransformer and *VOTransformer classes
├── listener/         # Sync domain listeners extending AbstractDomainEventListener
├── model/            # JPA @Entity extending ValueObject; primary keys are ALWAYS Integer (never Long/UUID)
├── repository/       # JpaRepository<*VO, Integer> & JpaSpecificationExecutor<*VO>; ends with *Repository
├── service/          # Service interfaces, sealed *AbstractBL, final *Impl, and AbstractProjector subclasses
│   └── impl/         # Cross-domain orchestration services (*ServiceImpl) that own NO repository
└── validation/       # @Component *Validator implements DomainValidator<*VO>
```

## 2. 4-Layer Entity & Repository Ownership Rules
Enforced by `DomainLogicProcessor` (`javac`), `AbstractBusinessLogic.afterPropertiesSet()`, `DomainLogicOwnershipRulesSpec`, `RepositoryOwnershipRulesSpec`, and `LayeredAndBoundaryRulesSpec`:
- **Entity (`*VO`)**: Extends `ValueObject`, uses `Integer` `@Id`, implements `getTableName()`, and uses `PhysicalNamingStrategyStandardImpl` (exact table/column names match SQL).
- **Sealed Abstract BL (`*AbstractBL`)**: Located in `..domain..service..`, declared `public abstract sealed class <Name>AbstractBL extends AbstractBusinessLogic<<Name>VO, <Name>Repository> permits <Name>Impl`, and annotated with `@DomainLogicFor(<Name>VO.class)` and `@RepositoryOwnerFor(<Name>Repository.class)`.
- **Final Service Implementation (`*Impl`)**: Located in `..domain..service..`, declared `@Service public final class <Name>Impl extends <Name>AbstractBL implements <Name>`.
- **Orchestration Services (`..service.impl.*ServiceImpl`)**: Services that coordinate multiple domains without owning an entity reside in `..service.impl..`, are named `*ServiceImpl`, do **not** extend `AbstractBusinessLogic`, and must **never** inject any `JpaRepository`.
- **Snapshot / Infrastructure Owners**: Read-model snapshot and outbox repositories are owned by dedicated `@Service` classes annotated with `@RepositoryOwnerFor` (discoverable via grep for `@RepositoryOwnerFor`).
- **Interface Injection Only**: `spring.aop.proxy-target-class: false` is active (JDK dynamic proxies). Always inject a domain service by its **interface** in `..service..` (or `..outbox..`), never by its concrete `*Impl` class.

## 3. Write Lifecycle & Validation (`AbstractBusinessLogic`)
- **All Writes Go Through `super.add(vo)` / `super.update(vo)` / `super.delete(vo)`**: `AbstractBusinessLogic` runs pre-persist hooks (`beforeAdd` / `beforeUpdate`), executes registered `DomainValidator<V>` beans, persists via `repo.save(vo)`, invokes post-persist hooks (`afterExecuteAdd` / `afterExecuteUpdate` / `afterExecuteDelete`), and optionally publishes a `DomainEvent`.
- **Validators Never Throw**: `DomainValidator<V>` implementations are pure message accumulators using `vo.addFieldMessage(field, msg)` and `vo.addGlobalMessage(msg)`. `AbstractBusinessLogic` automatically throws `ValidationException(vo)` when `vo.hasErrors()` is true.
- **Layer Boundaries (R1–R4)**:
  - Services return domain types (`Dto`, `VO`, `PagedResult<Dto>`, `SinglePagedResult<Dto>`, `void`) and **never** import `ResponseEntity`, `GlobalResponse`, or `ResponseUtils`.
  - Controllers never import `ValueObject` subclasses or `JpaRepository` interfaces and always wrap responses via `ResponseUtils`.
  - Every controller endpoint (except explicitly public endpoints such as `AuthController#refreshToken`) must declare `@IsAdmin`, `@IsAdminOrManager`, or `@IsAuthenticated` (meta-annotations backed by `@PreAuthorize`).

## 4. Event-Driven & CQRS Patterns
- Opt into domain event publishing in `*AbstractBL` by overriding `protected boolean publishesDomainEvents() { return true; }`.
- **Synchronous Same-TX Side Effects**: Extend `AbstractDomainEventListener` (`@EventListener`, executes synchronously inside the originating transaction; failure rolls back the write).
- **Async Read-Model Projections**: Extend `AbstractProjector` (`@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async`). Annotate `rebuildProjection()` with `@Transactional(propagation = Propagation.REQUIRES_NEW)`.

## 5. Jackson 3 vs. Jackson 2 & OpenAPI Rules
- **Jackson 3 (`tools.jackson`)**: Always import `tools.jackson.databind.ObjectMapper`, `tools.jackson.core.JacksonException`, and `tools.jackson.core.type.TypeReference`. **Never** import `com.fasterxml.jackson.databind.*` or `com.fasterxml.jackson.core.*`.
- **Jackson 2 Annotations**: Continue using `com.fasterxml.jackson.annotation.*` (`@JsonInclude`, `@JsonProperty`, `@JsonIgnore`, `@JsonCreator`).
- **DTO Enums**: Every enum in `..dto..` or referenced by any DTO field/accessor must be annotated with `@Schema(enumAsRef = true)`.

## 6. Testing Conventions (Spock)
- **Important Directory Convention**: All Spock `.groovy` test files live under `src/test/java/` and `src/test-integration/java/` (configured via `gmavenplus-plugin` in `backend/pom.xml`), **not** `src/test/groovy/`.
- **Unit Tests (`*Spec.groovy`)**: Extend `spock.lang.Specification` (or `BaseSpecification`), instantiate `*Impl` directly with Spock `Mock()` dependencies, and never boot a Spring context.
- **Integration Tests (`*ITSpec.groovy`)**: Located in `backend/src/test-integration/java/`, extend `BaseIntegrationSpecification` (`@SpringBootTest`, `@Import(TestContainersConfig)`).