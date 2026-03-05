package org.sofumar.portal.core.repo.jpaspec

import org.sofumar.portal.constants.Role
import org.sofumar.portal.constants.FieldConstants
import org.sofumar.portal.core.vo.UserVO
import org.sofumar.portal.testbase.BaseSpecification
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
        if (username != null) {
            inspection.filters.contains(FieldConstants.USERNAME)
            inspection.values.contains(expectedValue)
        } else {
            inspection.filters.isEmpty()
            inspection.values.isEmpty()
        }
        noExceptionThrown()

        where:
        username | expectedValue
        "user1"  | "user1"
        "USER1"  | "user1"
        null     | null
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

    @Unroll
    def "test - hasEmail: Should filter by email [email: #email]"() {
        given: "An email"
        String testEmail = email

        when: "The specification is created and inspected"
        JpaSpecification<UserVO> spec = UserSpecifications.hasEmail(testEmail)
        Map<String, List> inspection = inspectSpecification(spec)

        then: "Strict interaction check"
        0 * _

        and: "The expected filters and values are captured"
        if (email != null) {
            inspection.filters.contains(FieldConstants.EMAIL)
            inspection.values.contains(expectedValue)
        } else {
            inspection.filters.isEmpty()
            inspection.values.isEmpty()
        }
        noExceptionThrown()

        where:
        email          | expectedValue
        "test@e.com"   | "test@e.com"
        "TEST@E.COM"   | "test@e.com"
        null           | null
    }

    @Unroll
    def "test - notUserId: Should filter by not user id [userId: #userId]"() {
        given: "A user id"
        Integer testUserId = userId

        when: "The specification is created and inspected"
        JpaSpecification<UserVO> spec = UserSpecifications.notUserId(testUserId)
        Map<String, List> inspection = inspectSpecification(spec)

        then: "Strict interaction check"
        0 * _

        and: "The expected filters and values are captured"
        if (userId != null) {
            inspection.filters.contains(FieldConstants.USER_ID)
            inspection.values.contains(expectedValue)
        } else {
            inspection.filters.isEmpty()
            inspection.values.isEmpty()
        }
        noExceptionThrown()

        where:
        userId | expectedValue
        1      | 1
        100    | 100
        null   | null
    }
}