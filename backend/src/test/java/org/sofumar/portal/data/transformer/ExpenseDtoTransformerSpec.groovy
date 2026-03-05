package org.sofumar.portal.data.transformer

import org.sofumar.portal.core.vo.ExpenseVO
import org.sofumar.portal.data.dto.ExpenseDto
import org.sofumar.portal.testbase.BaseSpecification

import java.time.LocalDate

class ExpenseDtoTransformerSpec extends BaseSpecification {

    ExpenseDtoTransformer transformer = new ExpenseDtoTransformer()

    def "test - transform: Should transform ExpenseVO to ExpenseDto"() {
        given: "TestData setup"
        ExpenseVO vo = new ExpenseVO(
                expenseID: 1,
                dateOfExpense: LocalDate.now(),
                category: "TRAVEL",
                description: "Business trip",
                amount: 150.00
        )

        when: "The target method executed"
        ExpenseDto result = transformer.transform(vo)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result != null
        result.expenseID == vo.expenseID
        result.dateOfExpense == vo.dateOfExpense
        result.category == vo.category
        result.description == vo.description
        result.amount == vo.amount
        noExceptionThrown()
    }

    def "test - transform: Should return null when input is null"() {
        when: "The target method executed"
        ExpenseDto result = transformer.transform(null)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result == null
        noExceptionThrown()
    }

    def "test - transformList: Should transform list of ExpenseVOs"() {
        given: "TestData setup"
        ExpenseVO vo1 = new ExpenseVO(expenseID: 1, amount: 100.00)
        ExpenseVO vo2 = new ExpenseVO(expenseID: 2, amount: 200.00)
        List<ExpenseVO> list = [vo1, null, vo2]

        when: "The target method executed"
        List<ExpenseDto> result = transformer.transformList(list)

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
        List<ExpenseDto> result = transformer.transformList(null)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result != null
        result.isEmpty()
        noExceptionThrown()
    }
}