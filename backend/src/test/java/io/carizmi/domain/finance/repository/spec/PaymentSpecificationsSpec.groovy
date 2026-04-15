package io.carizmi.domain.finance.repository.spec

import io.carizmi.shared.constants.FieldConstants
import io.carizmi.shared.constants.TableConstants
import io.carizmi.domain.finance.model.PaymentVO
import io.carizmi.testbase.BaseSpecification
import org.springframework.data.jpa.domain.Specification as JpaSpecification
import spock.lang.Unroll

import java.time.LocalDate

class PaymentSpecificationsSpec extends BaseSpecification {

    @Unroll
    def "test - hasMemberID: Should filter by member ID [memberID: #memberID]"() {
        given: "A member ID"
        Integer id = memberID

        when: "The specification is created and inspected"
        JpaSpecification<PaymentVO> spec = PaymentSpecifications.hasMemberID(id)
        Map<String, List> inspection = inspectSpecification(spec)

        then: "Strict interaction check"
        0 * _

        and: "The expected filters and values are captured"
        if (id) {
            inspection.filters.containsAll([TableConstants.MEMBER_TABLE, FieldConstants.MEMBER_ID])
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
    def "test - hasFeeType: Should filter by fee type [feeType: #feeType]"() {
        given: "A fee type"
        String type = feeType

        when: "The specification is created and inspected"
        JpaSpecification<PaymentVO> spec = PaymentSpecifications.hasFeeType(type)
        Map<String, List> inspection = inspectSpecification(spec)

        then: "Strict interaction check"
        0 * _

        and: "The expected filters and values are captured"
        if (type) {
            inspection.filters.contains(FieldConstants.FEE_TYPE)
            inspection.values.contains(type)
        } else {
            inspection.filters.isEmpty()
            inspection.values.isEmpty()
        }
        noExceptionThrown()

        where:
        feeType << ["Membership Fee", null]
    }

    @Unroll
    def "test - hasYear: Should filter by year [hasYearVal: #hasYearVal]"() {
        given: "A year using dynamic current year"
        int currentYear = LocalDate.now().year
        Integer yearInput = hasYearVal ? currentYear : null

        when: "The specification is created and inspected"
        JpaSpecification<PaymentVO> spec = PaymentSpecifications.hasYear(yearInput)
        Map<String, List> inspection = inspectSpecification(spec)

        then: "Strict interaction check"
        0 * _

        and: "The expected filters and values are captured"
        if (yearInput) {
            inspection.filters.contains(FieldConstants.YEAR)
            inspection.values.contains(yearInput)
        } else {
            inspection.filters.isEmpty()
            inspection.values.isEmpty()
        }
        noExceptionThrown()

        where:
        hasYearVal << [true, false]
    }

    @Unroll
    def "test - hasQuarter: Should filter by quarter [quarter: #quarter]"() {
        given: "A quarter"
        Integer testQuarter = quarter

        when: "The specification is created and inspected"
        JpaSpecification<PaymentVO> spec = PaymentSpecifications.hasQuarter(testQuarter)
        Map<String, List> inspection = inspectSpecification(spec)

        then: "Strict interaction check"
        0 * _

        and: "The expected filters and values are captured"
        if (testQuarter) {
            inspection.filters.contains(FieldConstants.QUARTER)
            inspection.values.contains(testQuarter)
        } else {
            inspection.filters.isEmpty()
            inspection.values.isEmpty()
        }
        noExceptionThrown()

        where:
        quarter << [1, null]
    }

    @Unroll
    def "test - dateReceivedBetween: Should filter by date range [useStart: #useStart, useEnd: #useEnd]"() {
        given: "A date range using dynamic current year"
        int year = LocalDate.now().year
        LocalDate startDate = LocalDate.of(year, 1, 1)
        LocalDate endDate = LocalDate.of(year, 3, 31)

        LocalDate from = useStart ? startDate : null
        LocalDate to = useEnd ? endDate : null

        when: "The specification is created and inspected"
        JpaSpecification<PaymentVO> spec = PaymentSpecifications.dateReceivedBetween(from, to)
        Map<String, List> inspection = inspectSpecification(spec)

        then: "Strict interaction check"
        0 * _

        and: "The expected filters and values are captured"
        if (from || to) {
            inspection.filters.contains(FieldConstants.DATE_RECEIVED)
            if (from) inspection.values.contains(from)
            if (to) inspection.values.contains(to)
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
}