package io.carizmi.domain.membership.repository.spec

import io.carizmi.shared.constants.FieldConstants
import io.carizmi.shared.constants.ReferenceConstants
import io.carizmi.domain.membership.model.MemberVO
import io.carizmi.testbase.BaseSpecification
import org.springframework.data.jpa.domain.Specification as JpaSpecification
import spock.lang.Unroll

import java.time.LocalDate

class MemberSpecificationsSpec extends BaseSpecification {

    @Unroll
    def "test - hasStatus: Should filter by status [status: #status]"() {
        given: "A status"
        String testStatus = status

        when: "The specification is created and inspected"
        JpaSpecification<MemberVO> spec = MemberSpecifications.hasStatus(testStatus)
        Map<String, List> inspection = inspectSpecification(spec)

        then: "Strict interaction check"
        0 * _

        and: "The expected filters and values are captured"
        if (testStatus) {
            inspection.filters.contains(FieldConstants.STATUS)
            inspection.values.contains(testStatus)
        } else {
            inspection.filters.isEmpty()
            inspection.values.isEmpty()
        }
        noExceptionThrown()

        where:
        status << [ReferenceConstants.MEMBER_STATUS.ACTIVE, null]
    }

    @Unroll
    def "test - hasMemberID: Should filter by member ID [memberID: #memberID]"() {
        given: "A member ID"
        Integer id = memberID

        when: "The specification is created and inspected"
        JpaSpecification<MemberVO> spec = MemberSpecifications.hasMemberID(id)
        Map<String, List> inspection = inspectSpecification(spec)

        then: "Strict interaction check"
        0 * _

        and: "The expected filters and values are captured"
        if (id) {
            inspection.filters.contains(FieldConstants.MEMBER_ID)
            inspection.values.contains(id)
        } else {
            inspection.filters.isEmpty()
            inspection.values.isEmpty()
        }
        noExceptionThrown()

        where:
        memberID << [1, null]
    }

    @Unroll
    def "test - hasFirstName: Should filter by first name [firstName: #firstName]"() {
        given: "A first name"
        String name = firstName

        when: "The specification is created and inspected"
        JpaSpecification<MemberVO> spec = MemberSpecifications.hasFirstName(name)
        Map<String, List> inspection = inspectSpecification(spec)

        then: "Strict interaction check"
        0 * _

        and: "The expected filters and values are captured"
        if (name) {
            inspection.filters.contains(FieldConstants.FIRST_NAME)
            inspection.values.contains("%" + name.toLowerCase() + "%")
        } else {
            inspection.filters.isEmpty()
            inspection.values.isEmpty()
        }
        noExceptionThrown()

        where:
        firstName << ["John", null]
    }

    @Unroll
    def "test - hasLastName: Should filter by last name [lastName: #lastName]"() {
        given: "A last name"
        String name = lastName

        when: "The specification is created and inspected"
        JpaSpecification<MemberVO> spec = MemberSpecifications.hasLastName(name)
        Map<String, List> inspection = inspectSpecification(spec)

        then: "Strict interaction check"
        0 * _

        and: "The expected filters and values are captured"
        if (name) {
            inspection.filters.contains(FieldConstants.LAST_NAME)
            inspection.values.contains("%" + name.toLowerCase() + "%")
        } else {
            inspection.filters.isEmpty()
            inspection.values.isEmpty()
        }
        noExceptionThrown()

        where:
        lastName << ["Doe", null]
    }

    @Unroll
    def "test - hasPhone: Should filter by phone [phone: #phone]"() {
        given: "A phone number"
        String testPhone = phone

        when: "The specification is created and inspected"
        JpaSpecification<MemberVO> spec = MemberSpecifications.hasPhone(testPhone)
        Map<String, List> inspection = inspectSpecification(spec)

        then: "Strict interaction check"
        0 * _

        and: "The expected filters and values are captured"
        if (testPhone) {
            inspection.filters.contains(FieldConstants.PHONE)
            inspection.values.contains(testPhone)
        } else {
            inspection.filters.isEmpty()
            inspection.values.isEmpty()
        }
        noExceptionThrown()

        where:
        phone << ["123-456-7890", null]
    }

    @Unroll
    def "test - hasEmail: Should filter by email [email: #email]"() {
        given: "An email"
        String testEmail = email

        when: "The specification is created and inspected"
        JpaSpecification<MemberVO> spec = MemberSpecifications.hasEmail(testEmail)
        Map<String, List> inspection = inspectSpecification(spec)

        then: "Strict interaction check"
        0 * _

        and: "The expected filters and values are captured"
        if (testEmail) {
            inspection.filters.contains(FieldConstants.EMAIL)
            inspection.values.contains("%" + testEmail.toLowerCase() + "%")
        } else {
            inspection.filters.isEmpty()
            inspection.values.isEmpty()
        }
        noExceptionThrown()

        where:
        email << ["test@test.com", null]
    }

    @Unroll
    def "test - hasState: Should filter by state [state: #state]"() {
        given: "A state"
        String testState = state

        when: "The specification is created and inspected"
        JpaSpecification<MemberVO> spec = MemberSpecifications.hasState(testState)
        Map<String, List> inspection = inspectSpecification(spec)

        then: "Strict interaction check"
        0 * _

        and: "The expected filters and values are captured"
        if (testState) {
            inspection.filters.contains(FieldConstants.STATE)
            inspection.values.contains(testState)
        } else {
            inspection.filters.isEmpty()
            inspection.values.isEmpty()
        }
        noExceptionThrown()

        where:
        state << ["MN", null]
    }

    @Unroll
    def "test - joinDateAfter: Should filter by join date after [hasDate: #hasDate]"() {
        given: "A date using dynamic current year"
        int year = LocalDate.now().year
        LocalDate testDateVal = LocalDate.of(year, 1, 1)
        LocalDate date = hasDate ? testDateVal : null

        when: "The specification is created and inspected"
        JpaSpecification<MemberVO> spec = MemberSpecifications.joinDateAfter(date)
        Map<String, List> inspection = inspectSpecification(spec)

        then: "Strict interaction check"
        0 * _

        and: "The expected filters and values are captured"
        if (date) {
            inspection.filters.contains(FieldConstants.JOIN_DATE)
            inspection.values.contains(date)
        } else {
            inspection.filters.isEmpty()
            inspection.values.isEmpty()
        }
        noExceptionThrown()

        where:
        hasDate << [true, false]
    }

    @Unroll
    def "test - joinDateBefore: Should filter by join date before [hasDate: #hasDate]"() {
        given: "A date using dynamic current year"
        int year = LocalDate.now().year
        LocalDate testDateVal = LocalDate.of(year, 12, 31)
        LocalDate date = hasDate ? testDateVal : null

        when: "The specification is created and inspected"
        JpaSpecification<MemberVO> spec = MemberSpecifications.joinDateBefore(date)
        Map<String, List> inspection = inspectSpecification(spec)

        then: "Strict interaction check"
        0 * _

        and: "The expected filters and values are captured"
        if (date) {
            inspection.filters.contains(FieldConstants.JOIN_DATE)
            inspection.values.contains(date)
        } else {
            inspection.filters.isEmpty()
            inspection.values.isEmpty()
        }
        noExceptionThrown()

        where:
        hasDate << [true, false]
    }

    @Unroll
    def "test - joinDateBetween: Should filter by join date range [useStart: #useStart, useEnd: #useEnd]"() {
        given: "A date range using dynamic current year"
        int year = LocalDate.now().year
        LocalDate startDate = LocalDate.of(year, 1, 1)
        LocalDate endDate = LocalDate.of(year, 12, 31)

        LocalDate start = useStart ? startDate : null
        LocalDate end = useEnd ? endDate : null

        when: "The specification is created and inspected"
        JpaSpecification<MemberVO> spec = MemberSpecifications.joinDateBetween(start, end)
        Map<String, List> inspection = inspectSpecification(spec)

        then: "Strict interaction check"
        0 * _

        and: "The expected filters and values are captured"
        if (start || end) {
            inspection.filters.contains(FieldConstants.JOIN_DATE)
            if (start) inspection.values.contains(start)
            if (end) inspection.values.contains(end)
        } else {
            inspection.filters.isEmpty()
            inspection.values.isEmpty()
        }
        noExceptionThrown()

        where:
        useStart | useEnd
        true     | true
        true     | false
        false    | true
        false    | false
    }

    @Unroll
    def "test - lookup: Should perform fuzzy search [query: #query, expectedFilters: #expectedFilters, expectedValues: #expectedValues]"() {
        given: "A search query"
        String testQuery = query

        when: "The specification is created and inspected"
        JpaSpecification<MemberVO> spec = MemberSpecifications.lookup(testQuery)
        Map<String, List> inspection = inspectSpecification(spec)

        then: "Strict interaction check"
        0 * _

        and: "The expected filters and values are captured"
        if (testQuery && !testQuery.trim().isEmpty()) {
            inspection.filters.contains(FieldConstants.STATUS)
            inspection.values.contains(ReferenceConstants.MEMBER_STATUS.ACTIVE)
            inspection.filters.containsAll(expectedFilters)
            inspection.values.containsAll(expectedValues)
        } else {
            inspection.filters.isEmpty()
            inspection.values.isEmpty()
        }
        noExceptionThrown()

        where:
        query                  | expectedFilters                                       | expectedValues
        "123"                  | [FieldConstants.MEMBER_ID]                            | [123L]
        "10000000000000000000" | [FieldConstants.MEMBER_ID]                            | ["%10000000000000000000%"]
        "John"                 | [FieldConstants.FIRST_NAME, FieldConstants.LAST_NAME] | ["%john%", "%john%"]
        "John Smith"           | [FieldConstants.FIRST_NAME, FieldConstants.LAST_NAME] | ["%john%", "%smith%"]
        "  "                   | []                                                    | []
        null                   | []                                                    | []
    }
}