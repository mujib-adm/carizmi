package io.carizmi.domain.finance.controller

import io.carizmi.domain.finance.service.Payment
import io.carizmi.domain.finance.data.dto.PaymentDto
import io.carizmi.domain.finance.data.dto.request.PaymentSearchRequestDto
import io.carizmi.domain.finance.data.dto.response.LatestPaymentDto
import io.carizmi.framework.data.response.GlobalResponse
import io.carizmi.framework.data.response.PagedResult
import io.carizmi.framework.data.response.PaginationMeta
import io.carizmi.testbase.BaseSpecification
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Subject

class PaymentControllerSpec extends BaseSpecification {

    Payment paymentService = Mock()

    @Subject
    PaymentController paymentController = new PaymentController(paymentService)

    def "test - addPayment: Should delegate to payment service and wrap result"() {
        given: "A payment request"
        PaymentDto requestDto = new PaymentDto(amount: 50.0)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<Integer>> result = paymentController.addPayment(requestDto)

        then: "The expected calls are made"
        1 * paymentService.addPayment(requestDto) >> 1
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.OK
        result.body.responseData == 1
        noExceptionThrown()
    }

    def "test - updatePayment: Should delegate to payment service and wrap result"() {
        given: "A payment update request"
        PaymentDto requestDto = new PaymentDto(paymentID: 1, amount: 60.0)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<Void>> result = paymentController.updatePayment(requestDto)

        then: "The expected calls are made"
        1 * paymentService.updatePayment(requestDto)
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.OK
        noExceptionThrown()
    }

    def "test - deletePayment: Should delegate to payment service and wrap result"() {
        given: "A payment ID"
        Integer paymentID = 1

        when: "The target method executed"
        ResponseEntity<GlobalResponse<Void>> result = paymentController.deletePayment(paymentID)

        then: "The expected calls are made"
        1 * paymentService.deletePayment(paymentID)
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.OK
        noExceptionThrown()
    }

    def "test - getPayment: Should delegate to payment service and wrap result"() {
        given: "A payment ID"
        Integer paymentID = 1
        PaymentDto dto = new PaymentDto(paymentID: paymentID)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<PaymentDto>> result = paymentController.getPayment(paymentID)

        then: "The expected calls are made"
        1 * paymentService.getPayment(paymentID) >> dto
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.OK
        result.body.responseData.paymentID == paymentID
        noExceptionThrown()
    }

    def "test - searchPayments: Should delegate to payment service and wrap result"() {
        given: "A search request"
        PaymentSearchRequestDto request = new PaymentSearchRequestDto()
        List<PaymentDto> dtoList = [new PaymentDto(paymentID: 1)]
        PaginationMeta meta = PaginationMeta.of(0, 10, 1, 1)
        PagedResult<PaymentDto> pagedResult = PagedResult.of(dtoList, meta)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<List<PaymentDto>>> result = paymentController.searchPayments(request)

        then: "The expected calls are made"
        1 * paymentService.searchPayments(request) >> pagedResult
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.OK
        result.body.responseData.size() == 1
        result.body.responseData[0].paymentID == 1
        noExceptionThrown()
    }

    def "test - latestPayments: Should delegate to payment service and wrap result"() {
        given: "A limit"
        int limit = 10
        List<LatestPaymentDto> dtoList = [new LatestPaymentDto(paymentID: 1)]

        when: "The target method executed"
        ResponseEntity<GlobalResponse<List<LatestPaymentDto>>> result = paymentController.latestPayments(limit)

        then: "The expected calls are made"
        1 * paymentService.getLatestPayments(limit) >> dtoList
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.OK
        result.body.responseData.size() == 1
        result.body.responseData[0].paymentID == 1
        noExceptionThrown()
    }
}