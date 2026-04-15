package io.carizmi.domain.finance.repository.spec

import io.carizmi.shared.constants.FieldConstants
import io.carizmi.domain.finance.model.ExpenseVO
import io.carizmi.testbase.BaseSpecification
import org.springframework.data.jpa.domain.Specification as JpaSpecification
import spock.lang.Unroll

import java.time.LocalDate

class ExpenseSpecificationsSpec extends BaseSpecification {

    @Unroll
    def "test - hasCategory: Should filter by category [category: #category]"() {
        given: "A category"
        String testCategory = category

        when: "The specification is created and inspected"
        JpaSpecification<ExpenseVO> spec = ExpenseSpecifications.hasCategory(testCategory)
        Map<String, List> inspection = inspectSpecification(spec)

        then: "Strict interaction check"
        0 * _

        and: "The expected filters and values are captured"
        if (testCategory) {
            inspection.filters.contains(FieldConstants.CATEGORY)
            inspection.values.contains(testCategory)
        } else {
            inspection.filters.isEmpty()
            inspection.values.isEmpty()
        }
        noExceptionThrown()

        where:
        category << ["Utilities", null]
    }

    @Unroll
    def "test - dateOfExpenseBetween: Should filter by date range [from: #useStart, to: #useEnd]"() {
        given: "A date range using dynamic current year"
        int year = LocalDate.now().year
        LocalDate startDate = LocalDate.of(year, 1, 1)
        LocalDate endDate = LocalDate.of(year, 12, 31)

        LocalDate from = useStart ? startDate : null
        LocalDate to = useEnd ? endDate : null

        when: "The specification is created and inspected"
        JpaSpecification<ExpenseVO> spec = ExpenseSpecifications.dateOfExpenseBetween(from, to)
        Map<String, List> inspection = inspectSpecification(spec)

        then: "Strict interaction check"
        0 * _

        and: "The expected filters and values are captured"
        if (from || to) {
            inspection.filters.contains(FieldConstants.DATE_OF_EXPENSE)
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