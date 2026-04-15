package io.carizmi.archunit

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
import io.carizmi.testbase.BaseSpecification
import com.tngtech.archunit.core.importer.ImportOption

/**
 * DATA ACCESS ARCHITECTURE CHECK
 * This Spec ensures that data access layers are properly encapsulated and accessed only by designated components.
 * It enforces that only Business Logic Implementations access Repositories,
 * The spec fails if any of the data access architecture violations are detected.
 */
class DataAccessArchUnitSpec extends BaseSpecification {

    def "Only Business Logic Implementation must access Repositories - General Exclusion"() {
        given:
        JavaClasses importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("io.carizmi")

        // Guard: ensure classes were actually imported
        assert importedClasses.size() > 0

        // Classes outside of domain service and repository layers must not depend on repositories
        ArchRule generalNoAccessRule = ArchRuleDefinition.noClasses()
                .that().resideOutsideOfPackages("..domain..service..", "..domain..repository..", "..domain..bootstrap..")
                .should().dependOnClassesThat().resideInAPackage("..domain..repository..")

        expect:
        generalNoAccessRule.check(importedClasses)
    }

    def "Domain Logic implementations only access their designated repository - Strict One-to-One Rule"() {
        given:
        JavaClasses importedClasses = new ClassFileImporter().importPackages("io.carizmi")

        // Guard: ensure classes were actually imported
        assert importedClasses.size() > 0

        // Core Business Logic implementations must ONLY access their own 1-to-1 repository
        ArchRule strictOneToOneRule = ArchRuleDefinition.classes()
                .that().resideInAPackage("..domain..service..")
                .should(new ArchCondition<JavaClass>("only access their corresponding repository") {
                    @Override
                    void check(JavaClass item, ConditionEvents events) {
                        String className = item.getSimpleName()

                        // Skip tests (Spock Specs or JUnit Tests)
                        if (className.endsWith("Spec") || className.endsWith("Test")) {
                            return
                        }

                        // Only check *Impl and *AbstractBL classes (BL implementations)
                        if (!className.endsWith("Impl") && !className.endsWith("AbstractBL")) {
                            return
                        }

                        String domainName = className.replace("Impl", "").replace("AbstractBL", "")
                        String expectedRepo = domainName + "Repository"

                        item.getDirectDependenciesFromSelf().forEach { dep ->
                            JavaClass target = dep.getTargetClass()
                            if (target.getPackageName().contains("repository") && !target.getPackageName().contains("spec")) {
                                if (!target.getSimpleName().equals(expectedRepo)) {
                                    String message = String.format("Class %s accesses %s, but should only access %s",
                                            className, target.getSimpleName(), expectedRepo)
                                    events.add(SimpleConditionEvent.violated(item, message))
                                }
                            }
                        }
                    }
                })

        expect:
        strictOneToOneRule.check(importedClasses)
    }
}