package org.sofumar.portal.data.transformer

import org.sofumar.portal.core.vo.MemberVO
import org.sofumar.portal.core.vo.PaymentVO
import org.sofumar.portal.data.dto.PaymentDto
import org.sofumar.portal.testsupport.BaseSpecification
import spock.lang.Unroll

import java.time.LocalDate

class PaymentDtoTransformerSpec extends BaseSpecification {

    PaymentDtoTransformer transformer = new PaymentDtoTransformer()

    @Unroll
    def "test - transform: Should transform PaymentVO to PaymentDto [hasMember: #hasMember]"() {
        given: "TestData setup"
        MemberVO member = hasMember ? new MemberVO(memberID: 100L, firstName: "John", lastName: "Doe") : null
        PaymentVO vo = new PaymentVO(
                paymentID: 1,
                member: member,
                feeType: "MEMBERSHIP_FEE",
                amount: 50.00,
                dateReceived: LocalDate.of(2025, 1, 1),
                methodOfPayment: "CASH",
                year: 2025,
                quarter: 1
        )

        when: "The target method executed"
        PaymentDto result = transformer.transform(vo)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result != null
        result.paymentID == vo.paymentID
        result.memberID == (hasMember ? 100L : null)
        result.memberFullName == (hasMember ? "John Doe" : "")
        result.feeType == vo.feeType
        result.amount == vo.amount
        result.dateReceived == vo.dateReceived
        result.methodOfPayment == vo.methodOfPayment
        result.year == vo.year
        result.quarter == vo.quarter
        noExceptionThrown()

        where:
        hasMember << [true, false]
    }

    def "test - transform: Should return null when input is null"() {
        when: "The target method executed"
        PaymentDto result = transformer.transform(null)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result == null
        noExceptionThrown()
    }

    def "test - transformList: Should transform list of PaymentVOs"() {
        given: "TestData setup"
        PaymentVO vo1 = new PaymentVO(paymentID: 1, amount: 100.0)
        PaymentVO vo2 = new PaymentVO(paymentID: 2, amount: 200.0)
        List<PaymentVO> list = [vo1, null, vo2]

        when: "The target method executed"
        List<PaymentDto> result = transformer.transformList(list)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result.size() == 2
        result[0].paymentID == 1
        result[1].paymentID == 2
        noExceptionThrown()
    }

    def "test - transformList: Should return empty list when input is null"() {
        when: "The target method executed"
        List<PaymentDto> result = transformer.transformList(null)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result != null
        result.isEmpty()
        noExceptionThrown()
    }
}