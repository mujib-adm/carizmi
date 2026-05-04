package io.carizmi.archunit

import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition
import io.carizmi.framework.annotation.RepositoryOwnerFor
import spock.lang.Shared
import spock.lang.Specification

/**
 * Repository Ownership Architecture Rules
 *
 * <p>Enforces the platform's strict 1-to-1 repository ownership pattern:
 * each {@code *Repository} interface has exactly one owning service class,
 * and each owning class accesses only its declared repository.</p>
 *
 * <h2>Enforcement Strategy</h2>
 * <ul>
 *   <li><b>Rule 1 — Annotation-Based Ownership:</b> Classes annotated with
 * {@link RepositoryOwnerFor @RepositoryOwnerFor(FooRepository.class)} must only
 *       access {@code FooRepository}. Accessing any other repository is a violation.</li>
 *   <li><b>Rule 2 — Inbound Access Guard:</b> A repository may only be accessed by
 *       its designated owner ({@code FooImpl}, {@code FooAbstractBL}, or itself).
 *       Any other class importing it is a violation.</li>
 *   <li><b>Rule 3 — Outbound Access Guard:</b> An {@code *Impl} or {@code *AbstractBL}
 *       class may only access the repository whose name matches its own domain prefix.
 *       Accessing a mismatched repository is a violation.</li>
 *   <li><b>Rule 4 — Naming Convention:</b> All {@code JpaRepository} subinterfaces
 *       must be named with the {@code *Repository} suffix, ensuring they are
 *       automatically covered by the ownership rules above.</li>
 * </ul>
 *
 * @see RepositoryOwnerFor* @see io.carizmi.archunit.DomainLogicOwnershipRulesSpec
 */
class RepositoryOwnershipRulesSpec extends Specification {

    @Shared
    JavaClasses importedClasses

    void setupSpec() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("io.carizmi")
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Section 1: @RepositoryOwnerFor Annotation Enforcement
    // ═══════════════════════════════════════════════════════════════════════════

    def "Classes annotated with @RepositoryOwnerFor must only access the declared repository"() {
        given: "An ArchRule that checks @RepositoryOwnerFor-annotated classes"
        ArchRule rule = ArchRuleDefinition.classes()
                .should(new ArchCondition<JavaClass>("only access the repository declared in @RepositoryOwnerFor") {
                    @Override
                    void check(JavaClass item, ConditionEvents events) {
                        // Use Java reflection for cross-module annotation resolution
                        RepositoryOwnerFor annotation
                        try {
                            annotation = item.reflect().getAnnotation(RepositoryOwnerFor)
                        } catch (NoClassDefFoundError | UnsupportedOperationException ignored) {
                            return
                        }
                        if (annotation == null) return

                        String declaredRepo = annotation.value().getSimpleName()

                        item.getDirectDependenciesFromSelf().forEach { dep ->
                            JavaClass target = dep.getTargetClass()
                            String targetName = target.getSimpleName()

                            if (!targetName.endsWith("Repository")) return
                            if (!target.getPackageName().startsWith("io.carizmi")) return

                            if (targetName != declaredRepo) {
                                events.add(SimpleConditionEvent.violated(item, String.format(
                                        "%s is annotated with @RepositoryOwnerFor(%s) but accesses %s",
                                        item.getSimpleName(), declaredRepo, targetName)))
                            }
                        }
                    }
                })

        expect: "The rule is satisfied"
        rule.check(importedClasses)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Section 2: Structural Ownership Guards (Naming Convention Based)
    // ═══════════════════════════════════════════════════════════════════════════

    def "No class may access a repository unless it is the designated owner"() {
        given: "An ArchRule that enforces inbound access control on repositories"
        ArchRule rule = ArchRuleDefinition.classes()
                .should(new ArchCondition<JavaClass>("only access repositories they own (by naming convention)") {
                    @Override
                    void check(JavaClass item, ConditionEvents events) {
                        String className = item.getSimpleName()

                        item.getDirectDependenciesFromSelf().forEach { dep ->
                            JavaClass target = dep.getTargetClass()
                            String targetName = target.getSimpleName()

                            if (!targetName.endsWith("Repository")) return
                            if (!target.getPackageName().startsWith("io.carizmi")) return

                            // Derive the expected owner prefix: "FooRepository" → "Foo"
                            String ownerPrefix = targetName.replace("Repository", "")

                            boolean isDesignatedOwner =
                                    className == targetName                    // The repository interface itself
                                            || className == "${ownerPrefix}Impl"       // The BL implementation
                                            || className == "${ownerPrefix}AbstractBL" // The sealed abstract BL

                            if (!isDesignatedOwner) {
                                events.add(SimpleConditionEvent.violated(item, String.format(
                                        "%s accesses %s, but only %sImpl or %sAbstractBL may access it",
                                        className, targetName, ownerPrefix, ownerPrefix)))
                            }
                        }
                    }
                })

        expect: "The rule is satisfied"
        rule.check(importedClasses)
    }

    def "Each BL implementation must access only its own designated repository"() {
        given: "An ArchRule that enforces outbound access control on BL classes"
        ArchRule rule = ArchRuleDefinition.classes()
                .should(new ArchCondition<JavaClass>("only access their corresponding repository") {
                    @Override
                    void check(JavaClass item, ConditionEvents events) {
                        String className = item.getSimpleName()

                        // Only enforce on *Impl and *AbstractBL classes
                        if (!className.endsWith("Impl") && !className.endsWith("AbstractBL")) return
                        if (className.endsWith("Spec") || className.endsWith("Test")) return

                        // Derive expected repository from class name
                        String domainName = className.replace("Impl", "").replace("AbstractBL", "")
                        String expectedRepo = domainName + "Repository"

                        item.getDirectDependenciesFromSelf().forEach { dep ->
                            JavaClass target = dep.getTargetClass()
                            String targetName = target.getSimpleName()

                            if (!targetName.endsWith("Repository")) return
                            if (!target.getPackageName().startsWith("io.carizmi")) return

                            if (targetName != expectedRepo) {
                                events.add(SimpleConditionEvent.violated(item, String.format(
                                        "%s accesses %s, but should only access %s",
                                        className, targetName, expectedRepo)))
                            }
                        }
                    }
                })

        expect: "The rule is satisfied"
        rule.check(importedClasses)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Section 3: Repository Naming Convention
    // ═══════════════════════════════════════════════════════════════════════════

    def "All JpaRepository subinterfaces must be named with Repository suffix"() {
        when: "Scanning for all JpaRepository subinterfaces"
        List<String> violations = []
        importedClasses.each { javaClass ->
            try {
                Class<?> clazz = javaClass.reflect()
                if (clazz.isInterface() && org.springframework.data.jpa.repository.JpaRepository.isAssignableFrom(clazz)) {
                    if (!clazz.getSimpleName().endsWith("Repository")) {
                        violations.add("${clazz.getName()} extends JpaRepository but does not end with 'Repository'")
                    }
                }
            } catch (NoClassDefFoundError | UnsupportedOperationException ignored) {
                // Skip classes that can't be reflected
            }
        }

        then: "All JpaRepository subinterfaces follow the *Repository naming convention"
        assert violations.isEmpty(): "Naming violations found:\n${violations.join('\n')}"
    }
}