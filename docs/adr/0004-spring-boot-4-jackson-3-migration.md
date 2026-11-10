# ADR-0004: Spring Boot 4 & Jackson 3 (`tools.jackson`) Migration

- **Status:** Accepted
- **Date:** 2026-11-09
- **Enforcement:** Root [`pom.xml`](../../pom.xml), `LayeredAndBoundaryRulesSpec` (ArchUnit)

## Context
Carizmi upgraded from Java 21 / Spring Boot 3.x to **Java 25** and **Spring Boot 4** (Spring Framework 7; see root [`pom.xml`](../../pom.xml) for exact versions). Spring Boot 4 / Spring Framework 7 migrates its core JSON databind engine from Jackson 2 (`com.fasterxml.jackson.databind.*`) to **Jackson 3 (`tools.jackson.databind.*`)**, which uses a new Maven coordinates/package namespace (`tools.jackson.core`, `tools.jackson.databind`) and unchecked `JacksonException` hierarchy while continuing to use **Jackson 2 annotations (`com.fasterxml.jackson.annotation.*`)**.

Because AI coding models are heavily pre-trained on Spring Boot 2.x/3.x codebases, they default to importing `com.fasterxml.jackson.databind.ObjectMapper` and catching `com.fasterxml.jackson.core.JsonProcessingException`, causing immediate compilation or bean injection failures.

## Decision
Standardize all JSON serialization and deserialization across `framework` and `backend` on **Jackson 3 (`tools.jackson`)**:
1. **Databind & Core Imports**: Always use `tools.jackson.databind.ObjectMapper`, `tools.jackson.core.JacksonException`, and `tools.jackson.core.type.TypeReference`.
2. **Annotation Imports**: Continue using `com.fasterxml.jackson.annotation.*` (`@JsonInclude`, `@JsonProperty`, `@JsonIgnore`, `@JsonCreator`), which Jackson 3 natively reads.
3. **ArchUnit Guardrail**: `LayeredAndBoundaryRulesSpec` forbids any class in `io.carizmi..` from depending on `com.fasterxml.jackson.databind..` or `com.fasterxml.jackson.core..`.

## Consequences
- **Positive:** Aligns cleanly with Spring Boot 4's auto-configured `tools.jackson.databind.ObjectMapper` bean and prevents mixed-Jackson runtime bugs.
- **Pitfall to Avoid:** Never import `com.fasterxml.jackson.databind.ObjectMapper` or `com.fasterxml.jackson.core.JsonProcessingException`.