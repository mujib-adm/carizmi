package org.sofumar.portal.controller

import org.sofumar.portal.core.businesslogic.Payment
import org.sofumar.portal.data.dto.PaymentDto
import org.sofumar.portal.data.dto.request.PaymentSearchRequestDto
import org.sofumar.portal.data.dto.response.LatestPaymentDto
import org.sofumar.portal.framework.data.response.GlobalResponse
import org.sofumar.portal.testsupport.BaseSpecification
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Subject

class PaymentControllerSpec extends BaseSpecification {

    Payment paymentService = Mock()

    @Subject
    PaymentController paymentController = new PaymentController(paymentService)

    def "test - addPayment: Should delegate to payment service"() {
        given: "A payment request"
        BigDecimal amount = 50.0
        PaymentDto requestDto = new PaymentDto(amount: amount)
        Integer paymentID = 1
        GlobalResponse<Integer> responseBody = GlobalResponse.withResponseData(paymentID)
        ResponseEntity<GlobalResponse<Integer>> expectedResponse = new ResponseEntity<>(responseBody, HttpStatus.OK)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<Integer>> result = paymentController.addPayment(requestDto)

        then: "The expected calls are made"
        1 * paymentService.addPayment(requestDto) >> expectedResponse
        0 * _

        and: "The expected result"
        result == expectedResponse
        result.body.responseData == paymentID
        noExceptionThrown()
    }

    def "test - updatePayment: Should delegate to payment service"() {
        given: "A payment update request"
        Integer paymentID = 1
        BigDecimal amount = 60.0
        PaymentDto requestDto = new PaymentDto(paymentID: paymentID, amount: amount)
        ResponseEntity<GlobalResponse<Void>> expectedResponse = new ResponseEntity<>(HttpStatus.OK)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<Void>> result = paymentController.updatePayment(requestDto)

        then: "The expected calls are made"
        1 * paymentService.updatePayment(requestDto) >> expectedResponse
        0 * _

        and: "The expected result"
        result == expectedResponse
        result.statusCode == HttpStatus.OK
        noExceptionThrown()
    }

    def "test - deletePayment: Should delegate to payment service"() {
        given: "A payment ID"
        Integer paymentID = 1
        ResponseEntity<GlobalResponse<Void>> expectedResponse = new ResponseEntity<>(HttpStatus.OK)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<Void>> result = paymentController.deletePayment(paymentID)

        then: "The expected calls are made"
        1 * paymentService.deletePayment(paymentID) >> expectedResponse
        0 * _

        and: "The expected result"
        result == expectedResponse
        result.statusCode == HttpStatus.OK
        noExceptionThrown()
    }

    def "test - getPayment: Should delegate to payment service"() {
        given: "A payment ID"
        Integer paymentID = 1
        PaymentDto dto = new PaymentDto(paymentID: paymentID)
        GlobalResponse<PaymentDto> responseBody = GlobalResponse.withResponseData(dto)
        ResponseEntity<GlobalResponse<PaymentDto>> expectedResponse = new ResponseEntity<>(responseBody, HttpStatus.OK)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<PaymentDto>> result = paymentController.getPayment(paymentID)

        then: "The expected calls are made"
        1 * paymentService.getPayment(paymentID) >> expectedResponse
        0 * _

        and: "The expected result"
        result == expectedResponse
        result.body.responseData.paymentID == paymentID
        noExceptionThrown()
    }

    def "test - searchPayments: Should delegate to payment service"() {
        given: "A search request"
        PaymentSearchRequestDto request = new PaymentSearchRequestDto()
        Integer paymentID = 1
        Integer expectedSize = 1
        List<PaymentDto> dtoList = [new PaymentDto(paymentID: paymentID)]
        GlobalResponse<List<PaymentDto>> responseBody = GlobalResponse.withResponseData(dtoList)
        ResponseEntity<GlobalResponse<List<PaymentDto>>> expectedResponse = new ResponseEntity<>(responseBody, HttpStatus.OK)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<List<PaymentDto>>> result = paymentController.searchPayments(request)

        then: "The expected calls are made"
        1 * paymentService.searchPayments(request) >> expectedResponse
        0 * _

        and: "The expected result"
        result == expectedResponse
        result.body.responseData.size() == expectedSize
        result.body.responseData[0].paymentID == paymentID
        noExceptionThrown()
    }

    def "test - getLatest: Should delegate to payment service"() {
        given: "A limit"
        int limit = 10
        Integer paymentID = 1
        Integer expectedSize = 1
        List<LatestPaymentDto> dtoList = [new LatestPaymentDto(paymentID: paymentID)]
        GlobalResponse<List<LatestPaymentDto>> responseBody = GlobalResponse.withResponseData(dtoList)
        ResponseEntity<GlobalResponse<List<LatestPaymentDto>>> expectedResponse = new ResponseEntity<>(responseBody, HttpStatus.OK)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<List<LatestPaymentDto>>> result = paymentController.getLatest(limit)

        then: "The expected calls are made"
        1 * paymentService.getLatestPayments(limit) >> expectedResponse
        0 * _

        and: "The expected result"
        result == expectedResponse
        result.body.responseData.size() == expectedSize
        result.body.responseData[0].paymentID == paymentID
        noExceptionThrown()
    }
}