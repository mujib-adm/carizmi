package io.carizmi.archunit

import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.ArchRule
import io.carizmi.domain.finance.constants.QuarterCellStatus
import io.carizmi.domain.identity.controller.AuthController
import io.carizmi.framework.data.response.GlobalResponse
import io.carizmi.framework.message.MessageType
import io.carizmi.framework.util.ResponseUtils
import io.carizmi.framework.vo.ValueObject
import io.carizmi.infrastructure.security.annotation.IsAdmin
import io.carizmi.infrastructure.security.annotation.IsAdminOrManager
import io.carizmi.infrastructure.security.annotation.IsAuthenticated
import io.carizmi.shared.constants.QuarterStatus
import io.carizmi.shared.constants.Role
import io.carizmi.shared.data.dto.SortOrder
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import spock.lang.Shared
import spock.lang.Specification

import java.lang.annotation.Annotation
import java.lang.reflect.Method
import java.lang.reflect.Modifier

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses

/**
 * Layered Boundary, Jackson 3, OpenAPI Enum, and Security Architecture Rules.
 *
 * <p>Enforces Carizmi's core architectural guardrails across all backend and framework packages:</p>
 * <ul>
 *   <li><b>Rule 1 (R1/R4 — HTTP-Agnostic Services):</b> Service layer classes ({@code ..domain..service..})
 *       must never depend on {@link ResponseEntity}, {@link GlobalResponse}, or {@link ResponseUtils}.</li>
 *   <li><b>Rule 2 (R2 — Controller Isolation):</b> Web controllers ({@code ..controller..}) must never
 *       depend on {@link ValueObject} subclasses or {@link JpaRepository} interfaces.</li>
 *   <li><b>Rule 3 (Jackson 3 Enforcement):</b> No class in {@code io.carizmi..} may depend on legacy
 *       Jackson 2 databind/core packages ({@code com.fasterxml.jackson.databind..} or
 *       {@code com.fasterxml.jackson.core..}); all serialization must use {@code tools.jackson..}.</li>
 *   <li><b>Rule 4 (OpenAPI Enum $ref Extraction):</b> Any enum residing in {@code ..dto..} or referenced
 *       by any DTO class must declare {@code @Schema(enumAsRef = true)}.</li>
 *   <li><b>Rule 5 (Controller Endpoint Security):</b> Every mapped endpoint in {@code ..controller..}
 *       (except {@code AuthController#refreshToken}) must be secured via {@code @IsAdmin},
 *       {@code @IsAdminOrManager}, or {@code @IsAuthenticated} at the method or class level.</li>
 * </ul>
 */
class LayeredAndBoundaryRulesSpec extends Specification {

    private static final Set<String> PUBLIC_CONTROLLER_ENDPOINTS = Set.of(
            "io.carizmi.domain.identity.controller.AuthController#refreshToken"
    )

    @Shared
    JavaClasses importedClasses

    void setupSpec() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("io.carizmi")
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Rule 1: R1 / R4 — Business Logic & Services Must Be HTTP-Agnostic
    // ═══════════════════════════════════════════════════════════════════════════

    def "Service classes must not depend on ResponseEntity, GlobalResponse, or ResponseUtils (Rules R1 & R4)"() {
        given: "An ArchRule forbidding HTTP envelope dependencies in domain services"
        ArchRule rule = buildServiceHttpBoundaryRule()

        expect: "No service class imports HTTP response types"
        rule.check(importedClasses)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Rule 2: R2 — Controllers Must Not Access ValueObjects or JpaRepositories
    // ═══════════════════════════════════════════════════════════════════════════

    def "Controllers must not depend on ValueObject entities or JpaRepository interfaces (Rule R2)"() {
        given: "An ArchRule forbidding controllers from bypassing the service/DTO boundary"
        ArchRule rule = buildControllerPersistenceBoundaryRule()

        expect: "Controllers remain isolated from persistence types"
        rule.check(importedClasses)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Rule 3: Spring Boot 4 / Jackson 3 Databind Enforcement (ADR-0004)
    // ═══════════════════════════════════════════════════════════════════════════

    def "Application classes must use Jackson 3 (tools.jackson) and never legacy com.fasterxml.jackson.databind or core"() {
        given: "An ArchRule forbidding legacy Jackson 2 databind and core packages"
        ArchRule rule = buildJackson3Rule()

        expect: "Zero legacy Jackson 2 databind/core imports exist"
        rule.check(importedClasses)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Rule 4: OpenAPI Enum @Schema(enumAsRef = true) Enforcement
    // ═══════════════════════════════════════════════════════════════════════════

    def "All DTO-referenced enums must be annotated with @Schema(enumAsRef = true)"() {
        when: "Dynamically discovering all enums in ..dto.. or referenced by any ..dto.. class"
        Set<JavaClass> dtoReferencedEnums = findDtoReferencedEnums(importedClasses)
        List<String> violations = findEnumSchemaViolations(dtoReferencedEnums)

        then: "All DTO-referenced enums are discovered (excluding non-DTO enums) and declare @Schema(enumAsRef = true)"
        Set<String> discoveredNames = dtoReferencedEnums.collect { it.name }.toSet()
        discoveredNames.containsAll([
                SortOrder.name,
                QuarterStatus.name,
                QuarterCellStatus.name
        ])
        !discoveredNames.contains(Role.name)
        !discoveredNames.contains(MessageType.name)
        assert violations.isEmpty() : "DTO enums missing @Schema(enumAsRef = true):\n${violations.join('\n')}"
    }

    def "Enum schema validator reports a clean diagnostic message (with literal \$ref) when an unannotated enum is referenced"() {
        given: "A synthetic enum class without @Schema(enumAsRef = true)"
        JavaClass unannotatedEnum = new ClassFileImporter().importClass(Role)

        when: "Evaluating the enum against the Rule 4 validator"
        List<String> violations = findEnumSchemaViolations(Set.of(unannotatedEnum))

        then: "Violation message is formatted cleanly without GString evaluation errors"
        violations.size() == 1
        violations[0].contains(Role.name)
        violations[0].contains('missing @Schema(enumAsRef = true) (required for Orval $ref generation)')
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Rule 5: Controller Endpoint Security Annotation Coverage
    // ═══════════════════════════════════════════════════════════════════════════

    def "Every RestController endpoint must declare @IsAdmin, @IsAdminOrManager, or @IsAuthenticated unless explicitly public"() {
        when: "Scanning all @RestController classes and their HTTP mapping methods"
        List<String> violations = findUnsecuredControllerEndpoints(importedClasses, PUBLIC_CONTROLLER_ENDPOINTS)

        then: "Every non-public controller endpoint has an explicit authorization annotation"
        assert violations.isEmpty() : "Unsecured controller endpoints found:\n${violations.join('\n')}"
    }

    def "Controller security validator detects an endpoint when it is not covered by security annotations or the public allowlist"() {
        when: "Running the security scanner with an empty public endpoint allowlist"
        List<String> violations = findUnsecuredControllerEndpoints(importedClasses, Collections.emptySet())

        then: "The unannotated AuthController#refreshToken endpoint is flagged immediately"
        violations == ["${AuthController.name}#refreshToken is an HTTP endpoint without @IsAdmin, @IsAdminOrManager, or @IsAuthenticated".toString()]
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Rule Helpers
    // ═══════════════════════════════════════════════════════════════════════════

    private static ArchRule buildServiceHttpBoundaryRule() {
        return noClasses()
                .that().resideInAPackage("..domain..service..")
                .should().dependOnClassesThat().areAssignableTo(ResponseEntity)
                .orShould().dependOnClassesThat().areAssignableTo(GlobalResponse)
                .orShould().dependOnClassesThat().areAssignableTo(ResponseUtils)
                .because("Domain services must return domain/DTO types and never construct HTTP responses (see docs/ARCHITECTURE.md Rules R1 & R4)")
    }

    private static ArchRule buildControllerPersistenceBoundaryRule() {
        return noClasses()
                .that().resideInAPackage("..controller..")
                .should().dependOnClassesThat().areAssignableTo(ValueObject)
                .orShould().dependOnClassesThat().areAssignableTo(JpaRepository)
                .because("Controllers must interact exclusively with domain service interfaces and DTOs (see docs/ARCHITECTURE.md Rule R2)")
    }

    private static ArchRule buildJackson3Rule() {
        return noClasses()
                .that().resideInAPackage("io.carizmi..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.fasterxml.jackson.databind..",
                        "com.fasterxml.jackson.core.."
                )
                .because("Carizmi runs on Spring Boot 4 / Jackson 3; use tools.jackson.databind.ObjectMapper and tools.jackson.core.JacksonException (ADR-0004)")
    }

    private static Set<JavaClass> findDtoReferencedEnums(JavaClasses classes) {
        Set<JavaClass> dtoReferencedEnums = new LinkedHashSet<>()
        classes.each { javaClass ->
            if (!javaClass.packageName.contains(".dto")) return

            if (javaClass.isEnum()) {
                dtoReferencedEnums.add(javaClass)
            }

            javaClass.directDependenciesFromSelf.each { dep ->
                JavaClass target = dep.targetClass
                if (target.packageName.startsWith("io.carizmi") && target.isEnum()) {
                    dtoReferencedEnums.add(target)
                }
            }
        }
        return dtoReferencedEnums
    }

    private static List<String> findEnumSchemaViolations(Collection<JavaClass> enumClasses) {
        List<String> violations = []
        enumClasses.each { enumJavaClass ->
            try {
                Class<?> clazz = enumJavaClass.reflect()
                Schema schemaAnnotation = clazz.getAnnotation(Schema)
                if (schemaAnnotation == null || !schemaAnnotation.enumAsRef()) {
                    violations.add("${clazz.getName()} is exposed in a DTO but is missing @Schema(enumAsRef = true) (required for Orval \$ref generation)")
                }
            } catch (NoClassDefFoundError | UnsupportedOperationException ignored) {
            }
        }
        return violations
    }

    private static List<String> findUnsecuredControllerEndpoints(JavaClasses classes, Set<String> publicAllowlist) {
        List<String> violations = []
        classes.each { javaClass ->
            try {
                Class<?> clazz = javaClass.reflect()
                if (!clazz.isAnnotationPresent(RestController)) return

                boolean classSecured = hasSecurityAnnotation(clazz.getAnnotations())

                for (Method method : clazz.getDeclaredMethods()) {
                    if (!Modifier.isPublic(method.getModifiers())) continue
                    if (!isHttpEndpointMethod(method)) continue

                    String endpointId = "${clazz.getName()}#${method.getName()}"
                    if (publicAllowlist.contains(endpointId)) continue

                    boolean methodSecured = hasSecurityAnnotation(method.getAnnotations())
                    if (!classSecured && !methodSecured) {
                        violations.add("${endpointId} is an HTTP endpoint without @IsAdmin, @IsAdminOrManager, or @IsAuthenticated")
                    }
                }
            } catch (NoClassDefFoundError | UnsupportedOperationException ignored) {
            }
        }
        return violations
    }

    private static boolean isHttpEndpointMethod(Method method) {
        return method.isAnnotationPresent(GetMapping) ||
                method.isAnnotationPresent(PostMapping) ||
                method.isAnnotationPresent(PutMapping) ||
                method.isAnnotationPresent(DeleteMapping) ||
                method.isAnnotationPresent(PatchMapping) ||
                method.isAnnotationPresent(RequestMapping)
    }

    private static boolean hasSecurityAnnotation(Annotation[] annotations) {
        return annotations.any { ann ->
            Class<?> type = ann.annotationType()
            type == IsAdmin || type == IsAdminOrManager || type == IsAuthenticated
        }
    }
}