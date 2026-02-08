package org.sofumar.portal.core.repo.jpaspec

import org.sofumar.portal.constants.Role
import org.sofumar.portal.constants.FieldConstants
import org.sofumar.portal.core.vo.UserVO
import org.sofumar.portal.testsupport.BaseSpecification
import org.springframework.data.jpa.domain.Specification as JpaSpecification
import spock.lang.Unroll

class UserSpecificationsSpec extends BaseSpecification {

    @Unroll
    def "test - hasUsername: Should filter by username [username: #username]"() {
        given: "A username"
        String testUsername = username

        when: "The specification is created and inspected"
        JpaSpecification<UserVO> spec = UserSpecifications.hasUsername(testUsername)
        Map<String, List> inspection = inspectSpecification(spec)

        then: "Strict interaction check"
        0 * _

        and: "The expected filters and values are captured"
        inspection.filters.contains(FieldConstants.USERNAME)
        inspection.values.contains(testUsername)
        noExceptionThrown()

        where:
        username << ["user1", null]
    }

    @Unroll
    def "test - hasRole: Should filter by role [role: #role]"() {
        given: "A role"
        Role testRole = role

        when: "The specification is created and inspected"
        JpaSpecification<UserVO> spec = UserSpecifications.hasRole(testRole)
        Map<String, List> inspection = inspectSpecification(spec)

        then: "Strict interaction check"
        0 * _

        and: "The expected filters and values are captured"
        inspection.filters.contains(FieldConstants.ROLE)
        inspection.values.contains(testRole)
        noExceptionThrown()

        where:
        role << [Role.ADMIN, null]
    }

    @Unroll
    def "test - isActive: Should filter by active status [active: #active]"() {
        given: "An active status"
        boolean testActive = active

        when: "The specification is created and inspected"
        JpaSpecification<UserVO> spec = UserSpecifications.isActive(testActive)
        Map<String, List> inspection = inspectSpecification(spec)

        then: "Strict interaction check"
        0 * _

        and: "The expected filters and values are captured"
        inspection.filters.contains(FieldConstants.ACTIVE)
        inspection.values.contains(testActive)
        noExceptionThrown()

        where:
        active << [true, false]
    }
}