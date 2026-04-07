package org.sofumar.portal.controller

import org.sofumar.portal.core.businesslogic.Expense
import org.sofumar.portal.data.dto.ExpenseDto
import org.sofumar.portal.data.dto.request.ExpenseSearchRequestDto
import org.sofumar.portal.framework.data.response.GlobalResponse
import org.sofumar.portal.framework.data.response.PagedResult
import org.sofumar.portal.framework.data.response.PaginationMeta
import org.sofumar.portal.testbase.BaseSpecification
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Subject

class ExpenseControllerSpec extends BaseSpecification {

    Expense expenseService = Mock()

    @Subject
    ExpenseController expenseController = new ExpenseController(expenseService)

    def "test - addExpense: Should delegate to expense service and wrap result"() {
        given: "An expense request"
        ExpenseDto requestDto = new ExpenseDto(amount: 100.0)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<Integer>> result = expenseController.addExpense(requestDto)

        then: "The expected calls are made"
        1 * expenseService.addExpense(requestDto) >> 1
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.OK
        result.body.responseData == 1
        noExceptionThrown()
    }

    def "test - updateExpense: Should delegate to expense service and wrap result"() {
        given: "An expense update request"
        ExpenseDto requestDto = new ExpenseDto(expenseID: 1, amount: 200.0)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<Void>> result = expenseController.updateExpense(requestDto)

        then: "The expected calls are made"
        1 * expenseService.updateExpense(requestDto)
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.OK
        noExceptionThrown()
    }

    def "test - deleteExpense: Should delegate to expense service and wrap result"() {
        given: "An expense ID"
        Integer expenseID = 1

        when: "The target method executed"
        ResponseEntity<GlobalResponse<Void>> result = expenseController.deleteExpense(expenseID)

        then: "The expected calls are made"
        1 * expenseService.deleteExpense(expenseID)
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.OK
        noExceptionThrown()
    }

    def "test - getExpense: Should delegate to expense service and wrap result"() {
        given: "An expense ID"
        Integer expenseID = 1
        ExpenseDto dto = new ExpenseDto(expenseID: expenseID)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<ExpenseDto>> result = expenseController.getExpense(expenseID)

        then: "The expected calls are made"
        1 * expenseService.getExpense(expenseID) >> dto
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.OK
        result.body.responseData.expenseID == expenseID
        noExceptionThrown()
    }

    def "test - searchExpenses: Should delegate to expense service and wrap result"() {
        given: "A search request"
        ExpenseSearchRequestDto request = new ExpenseSearchRequestDto()
        List<ExpenseDto> dtoList = [new ExpenseDto(expenseID: 1)]
        PaginationMeta meta = PaginationMeta.of(0, 10, 1, 1)
        PagedResult<ExpenseDto> pagedResult = PagedResult.of(dtoList, meta)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<List<ExpenseDto>>> result = expenseController.searchExpenses(request)

        then: "The expected calls are made"
        1 * expenseService.searchExpenses(request) >> pagedResult
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.OK
        result.body.responseData.size() == 1
        result.body.responseData[0].expenseID == 1
        noExceptionThrown()
    }
}