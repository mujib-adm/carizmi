package org.sofumar.portal.controller

import org.sofumar.portal.core.businesslogic.Expense
import org.sofumar.portal.data.dto.ExpenseDto
import org.sofumar.portal.data.dto.request.ExpenseSearchRequestDto
import org.sofumar.portal.framework.data.response.GlobalResponse
import org.sofumar.portal.testsupport.BaseSpecification
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Subject

class ExpenseControllerSpec extends BaseSpecification {

    Expense expenseService = Mock()

    @Subject
    ExpenseController expenseController = new ExpenseController(expenseService)

    def "test - addExpense: Should delegate to expense service"() {
        given: "An expense request"
        BigDecimal amount = 100.0
        ExpenseDto requestDto = new ExpenseDto(amount: amount)
        Integer expenseID = 1
        GlobalResponse<Integer> responseBody = GlobalResponse.withResponseData(expenseID)
        ResponseEntity<GlobalResponse<Integer>> expectedResponse = new ResponseEntity<>(responseBody, HttpStatus.OK)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<Integer>> result = expenseController.addExpense(requestDto)

        then: "The expected calls are made"
        1 * expenseService.addExpense(requestDto) >> expectedResponse
        0 * _

        and: "The expected result"
        result == expectedResponse
        result.body.responseData == expenseID
        noExceptionThrown()
    }

    def "test - updateExpense: Should delegate to expense service"() {
        given: "An expense update request"
        Integer expenseID = 1
        BigDecimal amount = 200.0
        ExpenseDto requestDto = new ExpenseDto(expenseID: expenseID, amount: amount)
        ResponseEntity<GlobalResponse<Void>> expectedResponse = new ResponseEntity<>(HttpStatus.OK)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<Void>> result = expenseController.updateExpense(requestDto)

        then: "The expected calls are made"
        1 * expenseService.updateExpense(requestDto) >> expectedResponse
        0 * _

        and: "The expected result"
        result == expectedResponse
        result.statusCode == HttpStatus.OK
        noExceptionThrown()
    }

    def "test - deleteExpense: Should delegate to expense service"() {
        given: "An expense ID"
        Integer expenseID = 1
        ResponseEntity<GlobalResponse<Void>> expectedResponse = new ResponseEntity<>(HttpStatus.OK)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<Void>> result = expenseController.deleteExpense(expenseID)

        then: "The expected calls are made"
        1 * expenseService.deleteExpense(expenseID) >> expectedResponse
        0 * _

        and: "The expected result"
        result == expectedResponse
        result.statusCode == HttpStatus.OK
        noExceptionThrown()
    }

    def "test - getExpense: Should delegate to expense service"() {
        given: "An expense ID"
        Integer expenseID = 1
        ExpenseDto dto = new ExpenseDto(expenseID: expenseID)
        GlobalResponse<ExpenseDto> responseBody = GlobalResponse.withResponseData(dto)
        ResponseEntity<GlobalResponse<ExpenseDto>> expectedResponse = new ResponseEntity<>(responseBody, HttpStatus.OK)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<ExpenseDto>> result = expenseController.getExpense(expenseID)

        then: "The expected calls are made"
        1 * expenseService.getExpense(expenseID) >> expectedResponse
        0 * _

        and: "The expected result"
        result == expectedResponse
        result.body.responseData.expenseID == expenseID
        noExceptionThrown()
    }

    def "test - searchExpenses: Should delegate to expense service"() {
        given: "A search request"
        ExpenseSearchRequestDto request = new ExpenseSearchRequestDto()
        Integer firstExpenseID = 1
        Integer expectedSize = 1
        List<ExpenseDto> dtoList = [new ExpenseDto(expenseID: firstExpenseID)]
        GlobalResponse<List<ExpenseDto>> responseBody = GlobalResponse.withResponseData(dtoList)
        ResponseEntity<GlobalResponse<List<ExpenseDto>>> expectedResponse = new ResponseEntity<>(responseBody, HttpStatus.OK)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<List<ExpenseDto>>> result = expenseController.searchExpenses(request)

        then: "The expected calls are made"
        1 * expenseService.searchExpenses(request) >> expectedResponse
        0 * _

        and: "The expected result"
        result == expectedResponse
        result.body.responseData.size() == expectedSize
        result.body.responseData[0].expenseID == firstExpenseID
        noExceptionThrown()
    }
}