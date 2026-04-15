package io.carizmi.domain.finance.data.transformer

import io.carizmi.domain.finance.model.PaymentVO
import io.carizmi.domain.finance.data.dto.PaymentDto
import io.carizmi.testbase.BaseSpecification
import spock.lang.Unroll

import java.time.LocalDate

class PaymentVOTransformerSpec extends BaseSpecification {

    PaymentVOTransformer transformer = new PaymentVOTransformer()

    @Unroll
    def "test - transform: Should transform PaymentDto to PaymentVO [hasMemberID: #hasMemberID]"() {
        given: "TestData setup"
        PaymentDto dto = PaymentDto.builder()
                .memberID(hasMemberID ? 100 : null)
                .feeType("DONATION")
                .amount(100.00)
                .dateReceived(LocalDate.now())
                .methodOfPayment("TRANSFER")
                .year(2025)
                .quarter(2)
                .build()

        when: "The target method executed"
        PaymentVO result = transformer.transform(dto)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result != null
        result.feeType == dto.feeType
        result.amount == dto.amount
        result.dateReceived == dto.dateReceived
        result.methodOfPayment == dto.methodOfPayment
        result.year == dto.year
        result.quarter == dto.quarter
        if (hasMemberID) {
            result.member != null
            result.member.memberID == 100L
        } else {
            result.member == null
        }
        noExceptionThrown()

        where:
        hasMemberID << [true, false]
    }

    def "test - transform: Should return null when input is null"() {
        when: "The target method executed"
        PaymentVO result = transformer.transform(null)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result == null
        noExceptionThrown()
    }

    def "test - transformForUpdate: Should update existing PaymentVO from DTO"() {
        given: "TestData setup"
        PaymentVO existing = new PaymentVO(paymentID: 1, amount: 20.00)
        PaymentDto dto = PaymentDto.builder()
                .amount(30.00)
                .feeType("MISC")
                .build()

        when: "The target method executed"
        PaymentVO result = transformer.transformForUpdate(dto, existing)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result != null
        result == existing
        result.amount == dto.amount
        result.feeType == dto.feeType
        noExceptionThrown()
    }

    def "test - transformForUpdate: Should return existing when DTO is null"() {
        given: "TestData setup"
        PaymentVO existing = new PaymentVO(paymentID: 1, amount: 20.00)

        when: "The target method executed"
        PaymentVO result = transformer.transformForUpdate(null, existing)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result == existing
        noExceptionThrown()
    }

    def "test - transformForUpdate: Should return null when existing is null"() {
        given: "TestData setup"
        PaymentDto dto = PaymentDto.builder().amount(100.00).build()

        when: "The target method executed"
        PaymentVO result = transformer.transformForUpdate(dto, null)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result == null
        noExceptionThrown()
    }

    def "test - transformList: Should transform list of PaymentDtos"() {
        given: "TestData setup"
        PaymentDto dto1 = PaymentDto.builder().paymentID(1).amount(10.0).build()
        PaymentDto dto2 = PaymentDto.builder().paymentID(2).amount(20.0).build()
        List<PaymentDto> list = [dto1, null, dto2]

        when: "The target method executed"
        List<PaymentVO> result = transformer.transformList(list)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result.size() == 2
        result[0].amount == 10.0
        result[1].amount == 20.0
        noExceptionThrown()
    }

    def "test - transformList: Should return empty list when input is null"() {
        when: "The target method executed"
        List<PaymentVO> result = transformer.transformList(null)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result != null
        result.isEmpty()
        noExceptionThrown()
    }
}