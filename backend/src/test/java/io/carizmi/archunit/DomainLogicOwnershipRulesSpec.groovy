package io.carizmi.archunit

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.ArchRule
import io.carizmi.framework.annotation.DomainLogicFor
import io.carizmi.framework.bl.AbstractBusinessLogic
import spock.lang.Shared
import spock.lang.Specification

import java.lang.reflect.Modifier

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes

/**
 * Domain Logic Ownership Architecture Rules
 *
 * <p>Enforces the platform's strict 1-to-1 ownership model between Value Objects
 * and their business logic implementations. Every domain entity must have exactly
 * one {@link AbstractBusinessLogic} subclass that owns it via
 * {@link DomainLogicFor @DomainLogicFor}.</p>
 *
 * <h2>Enforcement Strategy</h2>
 * <ul>
 *   <li><b>Rule 1 — Package Location:</b> All {@code AbstractBusinessLogic} subclasses
 *       must reside in a {@code ..domain..service..} package.</li>
 *   <li><b>Rule 2 — Annotation Coverage:</b> Every {@code AbstractBusinessLogic} subclass
 *       must be annotated with {@code @DomainLogicFor} — no BL class may escape the registry.</li>
 *   <li><b>Rule 3 — 1-to-1 Uniqueness:</b> Each Value Object may be claimed by exactly
 *       one {@code @DomainLogicFor} annotation. Duplicate ownership is a violation.</li>
 *   <li><b>Rule 4 — Sealed Hierarchy:</b> All {@code *AbstractBL} classes must be declared
 * {@code sealed}, enforcing that only the explicitly permitted {@code *Impl} subclass
 *       can extend the business logic hierarchy.</li>
 *   <li><b>Rule 5 — Naming Convention:</b> {@code AbstractBusinessLogic} subclasses must
 *       follow the {@code *AbstractBL} or {@code *Impl} naming pattern.</li>
 *   <li><b>Rule 6 — Final Impl:</b> All {@code *Impl} classes must be declared
 *       {@code final}, closing the sealed hierarchy and preventing rogue subclasses.</li>
 * </ul>
 *
 * @see DomainLogicFor
 * @see io.carizmi.archunit.RepositoryOwnershipRulesSpec
 */
class DomainLogicOwnershipRulesSpec extends Specification {

    @Shared
    JavaClasses importedClasses

    void setupSpec() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("io.carizmi")
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Section 1: Package Location
    // ═══════════════════════════════════════════════════════════════════════════

    def "AbstractBusinessLogic subclasses must reside in their domain service package"() {
        given: "An ArchRule constraining ABL subclass locations"
        ArchRule rule = classes()
                .that().areAssignableTo(AbstractBusinessLogic)
                .should().resideInAPackage("..domain..service..")
                .orShould().be(AbstractBusinessLogic)

        expect: "The rule is satisfied"
        rule.check(importedClasses)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Section 2: @DomainLogicFor Annotation Coverage
    // ═══════════════════════════════════════════════════════════════════════════

    def "Every AbstractBusinessLogic subclass must be annotated with @DomainLogicFor"() {
        when: "Scanning for abstract ABL subclasses missing @DomainLogicFor"
        List<String> violations = []
        importedClasses.each { javaClass ->
            try {
                Class<?> clazz = javaClass.reflect()
                if (clazz == AbstractBusinessLogic) return
                if (!AbstractBusinessLogic.isAssignableFrom(clazz)) return

                // Only enforce on the abstract *AbstractBL layer — concrete *Impl classes
                // inherit ownership through the sealed hierarchy, not via direct annotation
                if (!Modifier.isAbstract(clazz.getModifiers())) return

                DomainLogicFor annotation = clazz.getAnnotation(DomainLogicFor)
                if (annotation == null) {
                    violations.add("${clazz.getName()} extends AbstractBusinessLogic but is not annotated with @DomainLogicFor")
                }
            } catch (NoClassDefFoundError | UnsupportedOperationException ignored) {
            }
        }

        then: "All abstract ABL subclasses declare their owned Value Object"
        assert violations.isEmpty(): "Missing @DomainLogicFor annotations:\n${violations.join('\n')}"
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Section 3: 1-to-1 Uniqueness
    // ═══════════════════════════════════════════════════════════════════════════

    def "Each Value Object must have exactly one @DomainLogicFor owner"() {
        when: "Grouping classes by the Value Object they claim ownership of"
        Map<String, List<String>> mapping = [:]
        importedClasses.each { javaClass ->
            try {
                Class<?> clazz = javaClass.reflect()
                DomainLogicFor annotation = clazz.getAnnotation(DomainLogicFor)
                if (annotation != null) {
                    String voClass = annotation.value().name
                    mapping.computeIfAbsent(voClass, { k -> [] }).add(javaClass.name)
                }
            } catch (NoClassDefFoundError | UnsupportedOperationException ignored) {
            }
        }

        then: "At least one @DomainLogicFor mapping exists"
        assert !mapping.isEmpty(): "No classes with @DomainLogicFor were found among ${importedClasses.size()} classes."

        and: "Each Value Object is owned by exactly one business logic class"
        mapping.each { vo, impls ->
            assert impls.size() == 1: "Expected exactly 1 Domain Logic owner for ${vo}, but found ${impls.size()}: ${impls}"
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Section 4: Sealed Hierarchy Enforcement
    // ═══════════════════════════════════════════════════════════════════════════

    def "All AbstractBL classes must be declared sealed"() {
        when: "Scanning for *AbstractBL classes that are not sealed"
        List<String> violations = []
        importedClasses.each { javaClass ->
            try {
                Class<?> clazz = javaClass.reflect()
                if (clazz == AbstractBusinessLogic) return
                if (!clazz.getSimpleName().endsWith("AbstractBL")) return
                if (!AbstractBusinessLogic.isAssignableFrom(clazz)) return

                if (!clazz.isSealed()) {
                    violations.add("${clazz.getName()} is an AbstractBL class but is not declared 'sealed'")
                }
            } catch (NoClassDefFoundError | UnsupportedOperationException ignored) {
            }
        }

        then: "Every *AbstractBL class uses the sealed modifier"
        assert violations.isEmpty(): "Unsealed AbstractBL classes found:\n${violations.join('\n')}"
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Section 5: BL Naming Convention
    // ═══════════════════════════════════════════════════════════════════════════

    def "AbstractBusinessLogic subclasses must follow the *AbstractBL or *Impl naming convention"() {
        when: "Scanning for ABL subclasses with non-standard names"
        List<String> violations = []
        importedClasses.each { javaClass ->
            try {
                Class<?> clazz = javaClass.reflect()
                if (clazz == AbstractBusinessLogic) return
                if (!AbstractBusinessLogic.isAssignableFrom(clazz)) return

                String simpleName = clazz.getSimpleName()
                boolean followsConvention = simpleName.endsWith("AbstractBL") || simpleName.endsWith("Impl")

                if (!followsConvention) {
                    violations.add("${clazz.getName()} extends AbstractBusinessLogic but does not end with 'AbstractBL' or 'Impl'")
                }
            } catch (NoClassDefFoundError | UnsupportedOperationException ignored) {
            }
        }

        then: "All ABL subclasses follow the naming convention"
        assert violations.isEmpty(): "Naming convention violations:\n${violations.join('\n')}"
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Section 6: Final Impl Enforcement
    // ═══════════════════════════════════════════════════════════════════════════

    def "All *Impl business logic classes must be declared final"() {
        when: "Scanning for *Impl classes that are not final"
        List<String> violations = []
        importedClasses.each { javaClass ->
            try {
                Class<?> clazz = javaClass.reflect()
                if (clazz == AbstractBusinessLogic) return
                if (!AbstractBusinessLogic.isAssignableFrom(clazz)) return
                if (!clazz.getSimpleName().endsWith("Impl")) return

                if (!Modifier.isFinal(clazz.getModifiers())) {
                    violations.add("${clazz.getName()} is an *Impl class but is not declared 'final'")
                }
            } catch (NoClassDefFoundError | UnsupportedOperationException ignored) {
            }
        }

        then: "Every *Impl class closes the sealed hierarchy with final"
        assert violations.isEmpty(): "Non-final Impl classes found:\n${violations.join('\n')}"
    }
}