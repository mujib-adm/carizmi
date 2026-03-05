package org.sofumar.portal.data.transformer

import org.sofumar.portal.core.vo.ExpenseVO
import org.sofumar.portal.data.dto.ExpenseDto
import org.sofumar.portal.testbase.BaseSpecification

import java.time.LocalDate

class ExpenseVOTransformerSpec extends BaseSpecification {

    ExpenseVOTransformer transformer = new ExpenseVOTransformer()

    def "test - transform: Should transform ExpenseDto to ExpenseVO"() {
        given: "TestData setup"
        ExpenseDto dto = ExpenseDto.builder()
                .expenseID(1)
                .dateOfExpense(LocalDate.now())
                .category("FOOD")
                .description("Lunch")
                .amount(25.50)
                .build()

        when: "The target method executed"
        ExpenseVO result = transformer.transform(dto)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result != null
        result.expenseID == dto.expenseID
        result.dateOfExpense == dto.dateOfExpense
        result.category == dto.category
        result.description == dto.description
        result.amount == dto.amount
        noExceptionThrown()
    }

    def "test - transform: Should return null when input is null"() {
        when: "The target method executed"
        ExpenseVO result = transformer.transform(null)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result == null
        noExceptionThrown()
    }

    def "test - transformForUpdate: Should update existing ExpenseVO from DTO"() {
        given: "TestData setup"
        ExpenseVO existing = new ExpenseVO(expenseID: 1, amount: 10.00, category: "OLD")
        ExpenseDto dto = ExpenseDto.builder()
                .expenseID(1)
                .dateOfExpense(LocalDate.of(2025, 1, 1))
                .category("NEW")
                .description("Updated")
                .amount(50.00)
                .build()

        when: "The target method executed"
        ExpenseVO result = transformer.transformForUpdate(dto, existing)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result != null
        result == existing
        result.dateOfExpense == dto.dateOfExpense
        result.category == dto.category
        result.description == dto.description
        result.amount == dto.amount
        noExceptionThrown()
    }

    def "test - transformForUpdate: Should return existing when DTO is null"() {
        given: "TestData setup"
        ExpenseVO existing = new ExpenseVO(expenseID: 1, amount: 10.00)

        when: "The target method executed"
        ExpenseVO result = transformer.transformForUpdate(null, existing)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result == existing
        noExceptionThrown()
    }

    def "test - transformForUpdate: Should return null when existing is null"() {
        given: "TestData setup"
        ExpenseDto dto = ExpenseDto.builder().amount(100.00).build()

        when: "The target method executed"
        ExpenseVO result = transformer.transformForUpdate(dto, null)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result == null
        noExceptionThrown()
    }

    def "test - transformList: Should transform list of ExpenseDtos"() {
        given: "TestData setup"
        ExpenseDto dto1 = ExpenseDto.builder().expenseID(1).amount(10.0).build()
        ExpenseDto dto2 = ExpenseDto.builder().expenseID(2).amount(20.0).build()
        List<ExpenseDto> list = [dto1, null, dto2]

        when: "The target method executed"
        List<ExpenseVO> result = transformer.transformList(list)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result.size() == 2
        result[0].expenseID == 1
        result[1].expenseID == 2
        noExceptionThrown()
    }

    def "test - transformList: Should return empty list when input is null"() {
        when: "The target method executed"
        List<ExpenseVO> result = transformer.transformList(null)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result != null
        result.isEmpty()
        noExceptionThrown()
    }
}