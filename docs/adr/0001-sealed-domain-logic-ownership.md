# ADR-0001: 1:1 Sealed Domain Logic Ownership & JDK Dynamic Proxies

- **Status:** Accepted
- **Date:** 2026-11-09
- **Enforcement:** `DomainLogicProcessor` (`javac`), `AbstractBusinessLogic.afterPropertiesSet()`, `application.yml` (`spring.aop.proxy-target-class: false`), `DomainLogicOwnershipRulesSpec`, `RepositoryOwnershipRulesSpec`

## Context
In standard Spring Boot applications, multiple `@Service` beans often inject the same `JpaRepository` and call `repository.save()` directly, scattering validation rules, audit updates, and domain event publishing. Furthermore, closing domain service hierarchies with `final` classes breaks Spring Boot's default CGLIB subclass proxying (`Cannot subclass final class`).

## Decision
Enforce **strict 1:1 entity and repository ownership** paired with **JDK interface-based dynamic proxies**:
1. **Sealed Abstract Business Logic (`*AbstractBL`)**: Each `*VO extends ValueObject` is bound to one `public abstract sealed class <Entity>AbstractBL extends AbstractBusinessLogic<<Entity>VO, <Entity>Repository> permits <Entity>Impl` annotated with `@DomainLogicFor(<Entity>VO.class)` and `@RepositoryOwnerFor(<Entity>Repository.class)`.
2. **Final Concrete Service (`*Impl`)**: Each `*AbstractBL` permits only one `@Service public final class <Entity>Impl extends <Entity>AbstractBL implements <Entity>`.
3. **JDK Dynamic Proxies (`spring.aop.proxy-target-class: false`)**: Because `*Impl` classes are `final`, Spring AOP is configured to use JDK interface proxies. All consumers (controllers, listeners, projectors, schedulers, orchestration services) **must inject the domain service interface**, never the concrete `*Impl` class.
4. **Orchestration Services (`..service.impl.*ServiceImpl`)**: Cross-domain orchestration services live in `domain/<context>/service/impl/`, do not extend `AbstractBusinessLogic`, and collaborate strictly via domain service interfaces—never by injecting a `JpaRepository`.

## Consequences
- **Positive:** Every entity write is guaranteed to pass through `AbstractBusinessLogic` hooks, `DomainValidator`, and `DomainEvent` publication; compile-time (`DomainLogicProcessor`), startup, and ArchUnit checks block any bypass.
- **Pitfall to Avoid:** Injecting a concrete `*Impl` class throws `BeanNotOfRequiredTypeException` at startup. Never change `proxy-target-class` to `true` or remove `final`/`sealed` modifiers.