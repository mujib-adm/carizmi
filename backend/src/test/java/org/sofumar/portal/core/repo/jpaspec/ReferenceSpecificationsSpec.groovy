package org.sofumar.portal.core.repo.jpaspec

import org.sofumar.portal.constants.FieldConstants
import org.sofumar.portal.core.vo.ReferenceVO
import org.sofumar.portal.testsupport.BaseSpecification
import org.springframework.data.jpa.domain.Specification as JpaSpecification
import spock.lang.Unroll

class ReferenceSpecificationsSpec extends BaseSpecification {

    @Unroll
    def "test - hasReferenceName: Should filter by reference name [name: #name]"() {
        given: "A reference name"
        String testName = name

        when: "The specification is created and inspected"
        JpaSpecification<ReferenceVO> spec = ReferenceSpecifications.hasReferenceName(testName)
        Map<String, List> inspection = inspectSpecification(spec)

        then: "Strict interaction check"
        0 * _

        and: "The expected filters and values are captured"
        inspection.filters.contains(FieldConstants.REFERENCE_NAME)
        inspection.values.contains(testName)
        noExceptionThrown()

        where:
        name << ["status", null]
    }

    @Unroll
    def "test - hasReferenceCode: Should filter by reference code [code: #code]"() {
        given: "A reference code"
        String testCode = code

        when: "The specification is created and inspected"
        JpaSpecification<ReferenceVO> spec = ReferenceSpecifications.hasReferenceCode(testCode)
        Map<String, List> inspection = inspectSpecification(spec)

        then: "Strict interaction check"
        0 * _

        and: "The expected filters and values are captured"
        inspection.filters.contains(FieldConstants.REFERENCE_CODE)
        inspection.values.contains(testCode)
        noExceptionThrown()

        where:
        code << ["ACTIVE", null]
    }

    @Unroll
    def "test - isActive: Should filter by active status [active: #active]"() {
        given: "An active status"
        Boolean testActive = active

        when: "The specification is created and inspected"
        JpaSpecification<ReferenceVO> spec = ReferenceSpecifications.isActive(testActive)
        Map<String, List> inspection = inspectSpecification(spec)

        then: "Strict interaction check"
        0 * _

        and: "The expected filters and values are captured"
        inspection.filters.contains(FieldConstants.ACTIVE)
        inspection.values.contains(testActive)
        noExceptionThrown()

        where:
        active << [true, false, null]
    }
}