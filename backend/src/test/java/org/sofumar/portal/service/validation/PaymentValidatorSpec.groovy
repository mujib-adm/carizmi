package org.sofumar.portal.service.validation

import org.sofumar.portal.constants.FieldConstants
import org.sofumar.portal.constants.ReferenceCodeConstants
import org.sofumar.portal.core.vo.MemberVO
import org.sofumar.portal.core.vo.PaymentVO
import org.sofumar.portal.framework.exception.ValidationException
import org.sofumar.portal.testsupport.BaseSpecification
import spock.lang.Subject
import spock.lang.Unroll

import java.time.LocalDate

class PaymentValidatorSpec extends BaseSpecification {

    ReferenceValidator referenceValidator = Mock()

    @Subject
    PaymentValidator paymentValidator = new PaymentValidator(referenceValidator)

    def "test - validate: Should pass for valid Non-Membership Payment"() {
        given: "A valid PaymentVO (e.g., REGISTRATION_FEE)"
        MemberVO member = new MemberVO(memberID: 1)
        String feeType = ReferenceCodeConstants.FEE_TYPE.REGISTRATION_FEE
        BigDecimal amount = 50.0
        LocalDate date = LocalDate.now()
        String method = ReferenceCodeConstants.PAYMENT_METHOD.CASH
        PaymentVO vo = new PaymentVO(member: member, feeType: feeType, amount: amount, dateReceived: date, methodOfPayment: method)

        String feeTypeField = FieldConstants.FEE_TYPE
        String feeTypeName = ReferenceCodeConstants.FEE_TYPE.NAME

        String methodField = FieldConstants.METHOD_OF_PAYMENT
        String methodName = ReferenceCodeConstants.PAYMENT_METHOD.NAME

        when: "The target method executed"
        paymentValidator.validate(vo)

        then: "The expected calls are made"
        1 * referenceValidator.validate(vo, feeTypeField, feeTypeName, feeType)
        1 * referenceValidator.validate(vo, methodField, methodName, method)
        0 * _

        and: "The expected result"
        !vo.hasErrors()
        noExceptionThrown()
    }

    def "test - validate: Should pass for valid Membership Payment"() {
        given: "A valid Membership PaymentVO"
        MemberVO member = new MemberVO(memberID: 1)
        String feeType = ReferenceCodeConstants.FEE_TYPE.MEMBERSHIP_FEE
        BigDecimal amount = 100.0
        LocalDate date = LocalDate.now()
        String method = ReferenceCodeConstants.PAYMENT_METHOD.CHECK
        Integer year = 2024
        Integer quarter = 1
        PaymentVO vo = new PaymentVO(member: member, feeType: feeType, amount: amount, dateReceived: date, methodOfPayment: method, year: year, quarter: quarter)

        String feeTypeField = FieldConstants.FEE_TYPE
        String feeTypeName = ReferenceCodeConstants.FEE_TYPE.NAME

        String methodField = FieldConstants.METHOD_OF_PAYMENT
        String methodName = ReferenceCodeConstants.PAYMENT_METHOD.NAME

        when: "The target method executed"
        paymentValidator.validate(vo)

        then: "The expected calls are made"
        1 * referenceValidator.validate(vo, feeTypeField, feeTypeName, feeType)
        1 * referenceValidator.validate(vo, methodField, methodName, method)
        0 * _

        and: "The expected result"
        !vo.hasErrors()
        noExceptionThrown()
    }

    @Unroll
    def "test - validate: Handling field validations [field: #field, value: #value]"() {
        given: "A PaymentVO with a specific field variation"
        String defaultFeeType = ReferenceCodeConstants.FEE_TYPE.REGISTRATION_FEE
        String defaultMethod = ReferenceCodeConstants.PAYMENT_METHOD.CASH

        MemberVO member = field == FieldConstants.MEMBER_ID && value == null ? null : new MemberVO(memberID: 1)
        PaymentVO vo = new PaymentVO(
                member: member,
                feeType: field == FieldConstants.FEE_TYPE ? value : defaultFeeType,
                amount: (BigDecimal) (field == FieldConstants.AMOUNT ? value : 50.0G),
                dateReceived: (LocalDate) (field == FieldConstants.DATE_RECEIVED ? value : LocalDate.now()),
                methodOfPayment: field == FieldConstants.METHOD_OF_PAYMENT ? value : defaultMethod
        )
        if (field == FieldConstants.MEMBER_ID && value != null) {
            vo.member = new MemberVO(memberID: null) // case where member exists but ID is null
        }

        String feeTypeField = FieldConstants.FEE_TYPE
        String feeTypeName = ReferenceCodeConstants.FEE_TYPE.NAME

        String paymentMethodField = FieldConstants.METHOD_OF_PAYMENT
        String methodName = ReferenceCodeConstants.PAYMENT_METHOD.NAME

        when: "The target method executed"
        try {
            paymentValidator.validate(vo)
        } catch (ValidationException e) {
            // expected
        }

        then: "The expected calls are made"
        if (!(field == feeTypeField && (value == null || value == ""))) {
            1 * referenceValidator.validate(vo, feeTypeField, feeTypeName, field == feeTypeField ? value : defaultFeeType)
        }

        if (!(field == paymentMethodField && (value == null || value == ""))) {
            1 * referenceValidator.validate(vo, paymentMethodField, methodName, field == paymentMethodField ? value : defaultMethod)
        }
        0 * _

        and: "The expected result"
        vo.hasErrors()
        vo.getFieldMessages().containsKey(field)
        noExceptionThrown()

        where:
        field                            | value
        FieldConstants.MEMBER_ID         | null
        FieldConstants.FEE_TYPE          | null
        FieldConstants.FEE_TYPE          | ""
        FieldConstants.AMOUNT            | null
        FieldConstants.DATE_RECEIVED     | null
        FieldConstants.METHOD_OF_PAYMENT | null
        FieldConstants.METHOD_OF_PAYMENT | ""
    }

    @Unroll
    def "test - validate: Handling period logic for Membership [year: #year, quarter: #quarter]"() {
        given: "A Membership PaymentVO with partial period"
        MemberVO member = new MemberVO(memberID: 1)
        String feeType = ReferenceCodeConstants.FEE_TYPE.MEMBERSHIP_FEE
        String method = ReferenceCodeConstants.PAYMENT_METHOD.CASH
        PaymentVO vo = new PaymentVO(member: member, feeType: feeType, amount: 100.0, dateReceived: LocalDate.now(), methodOfPayment: method, year: year, quarter: quarter)

        String feeTypeField = FieldConstants.FEE_TYPE
        String feeTypeName = ReferenceCodeConstants.FEE_TYPE.NAME

        String methodField = FieldConstants.METHOD_OF_PAYMENT
        String methodName = ReferenceCodeConstants.PAYMENT_METHOD.NAME

        when: "The target method executed"
        try {
            paymentValidator.validate(vo)
        } catch (ValidationException e) {
            // expected
        }

        then: "The expected calls are made"
        1 * referenceValidator.validate(vo, feeTypeField, feeTypeName, feeType)
        1 * referenceValidator.validate(vo, methodField, methodName, method)
        0 * _

        and: "The expected result"
        vo.hasErrors()
        if (year == null) vo.getFieldMessages().containsKey(FieldConstants.YEAR)
        if (quarter == null) vo.getFieldMessages().containsKey(FieldConstants.QUARTER)
        noExceptionThrown()

        where:
        year | quarter
        null | 1
        2024 | null
        null | null
    }

    def "test - validateForUpdate: Should validate paymentID and call validate"() {
        given: "A PaymentVO for update"
        Integer paymentID = 1
        MemberVO member = new MemberVO(memberID: 1)
        String feeType = ReferenceCodeConstants.FEE_TYPE.REGISTRATION_FEE
        String method = ReferenceCodeConstants.PAYMENT_METHOD.CASH
        PaymentVO vo = new PaymentVO(paymentID: paymentID, member: member, feeType: feeType, amount: 50.0, dateReceived: LocalDate.now(), methodOfPayment: method)

        String feeTypeField = FieldConstants.FEE_TYPE
        String feeTypeName = ReferenceCodeConstants.FEE_TYPE.NAME

        String methodField = FieldConstants.METHOD_OF_PAYMENT
        String methodName = ReferenceCodeConstants.PAYMENT_METHOD.NAME

        when: "The target method executed"
        paymentValidator.validateForUpdate(vo)

        then: "The expected calls are made"
        1 * referenceValidator.validate(vo, feeTypeField, feeTypeName, feeType)
        1 * referenceValidator.validate(vo, methodField, methodName, method)
        0 * _

        and: "The expected result"
        !vo.hasErrors()
        noExceptionThrown()
    }

    def "test - validateForUpdate: Should catch missing paymentID"() {
        given: "A PaymentVO for update without ID"
        MemberVO member = new MemberVO(memberID: 1)
        String feeType = ReferenceCodeConstants.FEE_TYPE.REGISTRATION_FEE
        String method = ReferenceCodeConstants.PAYMENT_METHOD.CASH
        PaymentVO vo = new PaymentVO(member: member, feeType: feeType, amount: 50.0, dateReceived: LocalDate.now(), methodOfPayment: method)

        String feeTypeField = FieldConstants.FEE_TYPE
        String feeTypeName = ReferenceCodeConstants.FEE_TYPE.NAME

        String methodField = FieldConstants.METHOD_OF_PAYMENT
        String methodName = ReferenceCodeConstants.PAYMENT_METHOD.NAME

        when: "The target method executed"
        paymentValidator.validateForUpdate(vo)

        then: "The expected calls are made"
        1 * referenceValidator.validate(vo, feeTypeField, feeTypeName, feeType)
        1 * referenceValidator.validate(vo, methodField, methodName, method)
        0 * _

        and: "The expected result"
        thrown(ValidationException)
        vo.hasErrors()
        vo.getFieldMessages().containsKey(FieldConstants.PAYMENT_ID)
    }
}