package org.sofumar.portal.archunit

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.ArchRule
import org.sofumar.portal.framework.annotation.DomainLogicFor
import org.sofumar.portal.framework.bl.AbstractBusinessLogic
import spock.lang.Shared
import spock.lang.Specification

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes

/**
 * DOMAIN LOGIC ONE-TO-ONE ARCHITECTURE CHECK
 * This Spec ensures that each Value Object has exactly one associated Domain Logic implementation.
 * It also verifies that all Domain Logic implementations reside in the correct package.
 * The spec fails if any of the one-to-one mapping violations are detected.
 */
class DomainLogicOneToOneArchUnitSpec extends Specification {

    @Shared
    JavaClasses importedClasses

    void setupSpec() {
        importedClasses = new ClassFileImporter().importPackages("org.sofumar.portal")
    }

    // Test 1: structure/location
    def "AbstractBusinessLogic subclasses must reside in the core.businesslogic.impl package"() {
        given: "The rule that AbstractBusinessLogic can only be extended by classes in the core.businesslogic.impl package"
        ArchRule rule = classes()
                .that().areAssignableTo(AbstractBusinessLogic)
                .should().resideInAPackage("org.sofumar.portal.core.businesslogic.impl")
                .orShould().be(AbstractBusinessLogic)

        expect: "The rule is satisfied"
        rule.check(importedClasses)
    }

    // Test 2: 1-to-1 mapping rule
    def "Enforce One-to-One: Each Value Object must have exactly one associated Domain Logic implementation"() {
        when: "Grouping classes by the Value Object they handle"
        Map<String, List<String>> mapping = [:]
        importedClasses.each { javaClass ->
            javaClass.annotations.each { annotation ->
                if (annotation.rawType.name == DomainLogicFor.class.name) {
                    // In ArchUnit, the 'value' property of an annotation is accessed this way
                    String voClass = annotation.getProperties().get("value").toString()
                    // Strip class suffix if present (e.g., "class org.sofumar.portal.core.vo.MemberVO")
                    voClass = voClass.replace("class ", "")
                    mapping.computeIfAbsent(voClass, { k -> [] }).add(javaClass.name)
                }
            }
        }

        then: "The imported classes should not be empty"
        assert importedClasses.size() > 0: "No classes were imported by ArchUnit"

        and: "The mapping should not be empty"
        assert !mapping.isEmpty(): "No classes with @DomainLogicFor were found among ${importedClasses.size()} classes. Packages found: ${importedClasses.collect { it.packageName }.unique().sort()}"

        and: "Each Value Object should have exactly one implementation"
        mapping.each { vo, impls ->
            assert impls.size() == 1: "Expected exactly 1 Domain Logic implementation for ${vo}, but found ${impls.size()}: ${impls}"
        }
    }
}