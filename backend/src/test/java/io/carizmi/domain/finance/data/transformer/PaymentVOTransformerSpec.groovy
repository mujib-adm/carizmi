package io.carizmi.domain.finance.data.transformer

import io.carizmi.domain.finance.model.PaymentVO
import io.carizmi.domain.finance.data.dto.PaymentDto
import io.carizmi.domain.membership.model.MemberVO
import io.carizmi.testbase.BaseSpecification
import jakarta.persistence.EntityManager

import java.time.LocalDate

class PaymentVOTransformerSpec extends BaseSpecification {

    EntityManager entityManager = Mock()
    PaymentVOTransformer transformer = new PaymentVOTransformer(entityManager)

    def "test - transform: Should transform PaymentDto to PaymentVO"() {
        given: "TestData setup"
        Integer memberID = 100
        MemberVO memberRef = new MemberVO(memberID: memberID)
        PaymentDto dto = PaymentDto.builder()
                .memberID(memberID)
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
        1 * entityManager.getReference(MemberVO.class, 100) >> memberRef
        0 * _

        and: "The expected result"
        result != null
        result.member == memberRef
        result.feeType == dto.feeType
        result.amount == dto.amount
        result.dateReceived == dto.dateReceived
        result.methodOfPayment == dto.methodOfPayment
        result.year == dto.year
        result.quarter == dto.quarter
        noExceptionThrown()
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