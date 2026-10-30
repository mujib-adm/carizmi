package io.carizmi.domain.finance.validation

import io.carizmi.domain.membership.service.MemberMembershipPeriod
import io.carizmi.domain.platform.constants.BaselineConstants
import io.carizmi.domain.platform.model.SystemSettingsVO
import io.carizmi.domain.platform.service.SystemSetting
import io.carizmi.domain.platform.validation.ReferenceValidator

import io.carizmi.shared.constants.FieldConstants
import io.carizmi.shared.constants.ReferenceConstants
import io.carizmi.domain.membership.model.MemberVO
import io.carizmi.domain.finance.model.PaymentVO
import io.carizmi.shared.message.ValidationMessages
import io.carizmi.testbase.BaseSpecification
import spock.lang.Subject
import spock.lang.Unroll

import java.time.LocalDate

class PaymentValidatorSpec extends BaseSpecification {

    ReferenceValidator referenceValidator = Mock()
    SystemSetting systemSetting = Mock()
    MemberMembershipPeriod memberMembershipPeriod = Mock()

    @Subject
    PaymentValidator paymentValidator = new PaymentValidator(referenceValidator, systemSetting, memberMembershipPeriod)

    def "test - validate: Should pass for valid Non-Membership Payment"() {
        given: "A valid PaymentVO (e.g., REGISTRATION_FEE)"
        MemberVO member = new MemberVO(memberID: 1, status: ReferenceConstants.MEMBER_STATUS.ACTIVE)
        String feeType = ReferenceConstants.FEE_TYPE.REGISTRATION_FEE
        BigDecimal amount = 50.0
        LocalDate date = LocalDate.now()
        String method = ReferenceConstants.PAYMENT_METHOD.CASH
        PaymentVO vo = new PaymentVO(member: member, feeType: feeType, amount: amount, dateReceived: date, methodOfPayment: method)

        String feeTypeField = FieldConstants.FEE_TYPE
        String feeTypeName = ReferenceConstants.FEE_TYPE.NAME

        String methodField = FieldConstants.METHOD_OF_PAYMENT
        String methodName = ReferenceConstants.PAYMENT_METHOD.NAME

        when: "The target method executed"
        paymentValidator.validate(vo)

        then: "The expected calls are made"
        1 * referenceValidator.validate(vo, feeTypeField, feeTypeName, feeType)
        1 * referenceValidator.validate(vo, methodField, methodName, method)
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(date.year + 1)) >> Optional.empty()
        0 * _

        and: "The expected result"
        !vo.hasErrors()
        noExceptionThrown()
    }

    def "test - validate: Should pass for valid Membership Payment"() {
        given: "A valid Membership PaymentVO"
        MemberVO member = new MemberVO(memberID: 1, status: ReferenceConstants.MEMBER_STATUS.ACTIVE)
        String feeType = ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE
        BigDecimal amount = 100.0
        LocalDate date = LocalDate.now()
        String method = ReferenceConstants.PAYMENT_METHOD.CHECK
        Integer year = 2024
        Integer quarter = 1
        PaymentVO vo = new PaymentVO(member: member, feeType: feeType, amount: amount, dateReceived: date, methodOfPayment: method, year: year, quarter: quarter)

        String feeTypeField = FieldConstants.FEE_TYPE
        String feeTypeName = ReferenceConstants.FEE_TYPE.NAME

        String methodField = FieldConstants.METHOD_OF_PAYMENT
        String methodName = ReferenceConstants.PAYMENT_METHOD.NAME

        when: "The target method executed"
        paymentValidator.validate(vo)

        then: "The expected calls are made"
        1 * referenceValidator.validate(vo, feeTypeField, feeTypeName, feeType)
        1 * referenceValidator.validate(vo, methodField, methodName, method)
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(date.year + 1)) >> Optional.empty()
        1 * memberMembershipPeriod.isMemberActiveInQuarter(1, year, quarter) >> true
        0 * _

        and: "The expected result"
        !vo.hasErrors()
        noExceptionThrown()
    }

    @Unroll
    def "test - validate: Handling field validations [field: #field, value: #value]"() {
        given: "A PaymentVO with a specific field variation"
        String defaultFeeType = ReferenceConstants.FEE_TYPE.REGISTRATION_FEE
        String defaultMethod = ReferenceConstants.PAYMENT_METHOD.CASH

        MemberVO member = field == FieldConstants.MEMBER_ID && value == null ? null : new MemberVO(memberID: 1, status: ReferenceConstants.MEMBER_STATUS.ACTIVE)
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
        String feeTypeName = ReferenceConstants.FEE_TYPE.NAME

        String paymentMethodField = FieldConstants.METHOD_OF_PAYMENT
        String methodName = ReferenceConstants.PAYMENT_METHOD.NAME

        when: "The target method executed"
        paymentValidator.validate(vo)

        then: "The expected calls are made"
        if (!(field == feeTypeField && (value == null || value == ""))) {
            1 * referenceValidator.validate(vo, feeTypeField, feeTypeName, field == feeTypeField ? value : defaultFeeType)
        }

        if (!(field == paymentMethodField && (value == null || value == ""))) {
            1 * referenceValidator.validate(vo, paymentMethodField, methodName, field == paymentMethodField ? value : defaultMethod)
        }
        if (vo.getDateReceived() != null) {
            1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(vo.getDateReceived().year + 1)) >> Optional.empty()
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

    def "test - validate: Handling memberID, should catch missing member ID even if member object exists"() {
        given: "A PaymentVO with member object but null ID"
        MemberVO testMember = new MemberVO(memberID: null)
        String testFeeType = ReferenceConstants.FEE_TYPE.REGISTRATION_FEE
        String testMethod = ReferenceConstants.PAYMENT_METHOD.CASH
        PaymentVO vo = new PaymentVO(member: testMember, feeType: testFeeType, amount: 50.0, dateReceived: LocalDate.now(), methodOfPayment: testMethod)

        String feeTypeField = FieldConstants.FEE_TYPE
        String feeTypeName = ReferenceConstants.FEE_TYPE.NAME
        String methodField = FieldConstants.METHOD_OF_PAYMENT
        String methodName = ReferenceConstants.PAYMENT_METHOD.NAME

        when: "The target method executed"
        paymentValidator.validate(vo)

        then: "The expected calls are made"
        1 * referenceValidator.validate(vo, feeTypeField, feeTypeName, testFeeType)
        1 * referenceValidator.validate(vo, methodField, methodName, testMethod)
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(LocalDate.now().year + 1)) >> Optional.empty()
        0 * _

        and: "The error is captured for member ID"
        vo.hasErrors()
        vo.getFieldMessages().containsKey(FieldConstants.MEMBER_ID)
    }

    @Unroll
    def "test - validate: Handling period logic for Membership [year: #year, quarter: #quarter]"() {
        given: "A Membership PaymentVO with partial period"
        MemberVO member = new MemberVO(memberID: 1, status: ReferenceConstants.MEMBER_STATUS.ACTIVE)
        String feeType = ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE
        String method = ReferenceConstants.PAYMENT_METHOD.CASH
        PaymentVO vo = new PaymentVO(member: member, feeType: feeType, amount: 100.0, dateReceived: LocalDate.now(), methodOfPayment: method, year: year, quarter: quarter)

        String feeTypeField = FieldConstants.FEE_TYPE
        String feeTypeName = ReferenceConstants.FEE_TYPE.NAME

        String methodField = FieldConstants.METHOD_OF_PAYMENT
        String methodName = ReferenceConstants.PAYMENT_METHOD.NAME

        when: "The target method executed"
        paymentValidator.validate(vo)

        then: "The expected calls are made"
        1 * referenceValidator.validate(vo, feeTypeField, feeTypeName, feeType)
        1 * referenceValidator.validate(vo, methodField, methodName, method)
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(LocalDate.now().year + 1)) >> Optional.empty()
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
        MemberVO member = new MemberVO(memberID: 1, status: ReferenceConstants.MEMBER_STATUS.ACTIVE)
        String feeType = ReferenceConstants.FEE_TYPE.REGISTRATION_FEE
        String method = ReferenceConstants.PAYMENT_METHOD.CASH
        PaymentVO vo = new PaymentVO(paymentID: paymentID, member: member, feeType: feeType, amount: 50.0, dateReceived: LocalDate.now(), methodOfPayment: method)

        String feeTypeField = FieldConstants.FEE_TYPE
        String feeTypeName = ReferenceConstants.FEE_TYPE.NAME

        String methodField = FieldConstants.METHOD_OF_PAYMENT
        String methodName = ReferenceConstants.PAYMENT_METHOD.NAME

        when: "The target method executed"
        paymentValidator.validateForUpdate(vo)

        then: "The expected calls are made"
        1 * referenceValidator.validate(vo, feeTypeField, feeTypeName, feeType)
        1 * referenceValidator.validate(vo, methodField, methodName, method)
        2 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(LocalDate.now().year + 1)) >> Optional.empty()
        0 * _

        and: "The expected result"
        !vo.hasErrors()
        noExceptionThrown()
    }

    def "test - validateForUpdate: Should catch missing paymentID"() {
        given: "A PaymentVO for update without ID"
        MemberVO member = new MemberVO(memberID: 1, status: ReferenceConstants.MEMBER_STATUS.ACTIVE)
        String feeType = ReferenceConstants.FEE_TYPE.REGISTRATION_FEE
        String method = ReferenceConstants.PAYMENT_METHOD.CASH
        PaymentVO vo = new PaymentVO(member: member, feeType: feeType, amount: 50.0, dateReceived: LocalDate.now(), methodOfPayment: method)

        String feeTypeField = FieldConstants.FEE_TYPE
        String feeTypeName = ReferenceConstants.FEE_TYPE.NAME

        String methodField = FieldConstants.METHOD_OF_PAYMENT
        String methodName = ReferenceConstants.PAYMENT_METHOD.NAME

        when: "The target method executed"
        paymentValidator.validateForUpdate(vo)

        then: "The expected calls are made"
        1 * referenceValidator.validate(vo, feeTypeField, feeTypeName, feeType)
        1 * referenceValidator.validate(vo, methodField, methodName, method)
        2 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(LocalDate.now().year + 1)) >> Optional.empty()
        0 * _

        and: "The expected result"
        noExceptionThrown()
        vo.hasErrors()
        vo.getFieldMessages().containsKey(FieldConstants.PAYMENT_ID)
    }

    def "test - validate: Fails if dateReceived is in a closed fiscal year"() {
        given:
        int year = LocalDate.now().year
        PaymentVO vo = new PaymentVO(
                member: new MemberVO(memberID: 1, status: ReferenceConstants.MEMBER_STATUS.ACTIVE),
                feeType: ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE,
                amount: new BigDecimal("100.00"),
                dateReceived: LocalDate.of(year, 6, 15),
                methodOfPayment: ReferenceConstants.PAYMENT_METHOD.CASH,
                year: year,
                quarter: 2
        )

        when: "Validation is executed"
        paymentValidator.validate(vo)

        then: "Error is attached to dateReceived"
        1 * referenceValidator.validate(vo, FieldConstants.FEE_TYPE, ReferenceConstants.FEE_TYPE.NAME, ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE)
        1 * referenceValidator.validate(vo, FieldConstants.METHOD_OF_PAYMENT, ReferenceConstants.PAYMENT_METHOD.NAME, ReferenceConstants.PAYMENT_METHOD.CASH)
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(year + 1)) >> Optional.of(new SystemSettingsVO())
        1 * memberMembershipPeriod.isMemberActiveInQuarter(1, year, 2) >> true
        0 * _

        and: "The expected result"
        vo.hasErrors()
        vo.getFieldMessages().containsKey(FieldConstants.DATE_RECEIVED)
        noExceptionThrown()
    }

    def "test - validate: Passes if dateReceived is in an open fiscal year"() {
        given:
        int year = LocalDate.now().year
        PaymentVO vo = new PaymentVO(
                member: new MemberVO(memberID: 1, status: ReferenceConstants.MEMBER_STATUS.ACTIVE),
                feeType: ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE,
                amount: new BigDecimal("100.00"),
                dateReceived: LocalDate.of(year + 1, 2, 10),
                methodOfPayment: ReferenceConstants.PAYMENT_METHOD.CASH,
                year: year + 1,
                quarter: 4
        )

        when: "Validation is executed"
        paymentValidator.validate(vo)

        then: "No dateReceived errors"
        1 * referenceValidator.validate(vo, FieldConstants.FEE_TYPE, ReferenceConstants.FEE_TYPE.NAME, ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE)
        1 * referenceValidator.validate(vo, FieldConstants.METHOD_OF_PAYMENT, ReferenceConstants.PAYMENT_METHOD.NAME, ReferenceConstants.PAYMENT_METHOD.CASH)
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(year + 2)) >> Optional.empty()
        1 * memberMembershipPeriod.isMemberActiveInQuarter(1, year + 1, 4) >> true
        0 * _

        and: "The expected result"
        !vo.hasErrors()
        !vo.getFieldMessages().containsKey(FieldConstants.DATE_RECEIVED)
        noExceptionThrown()
    }

    def "test - validateForDelete: Fails if payment is in a closed fiscal year"() {
        given:
        int year = 2025
        PaymentVO vo = new PaymentVO(paymentID: 10, dateReceived: LocalDate.of(year, 4, 15))

        when: "validateForDelete is called"
        paymentValidator.validateForDelete(vo)

        then: "Error is attached to dateReceived"
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(year + 1)) >> Optional.of(new SystemSettingsVO())
        0 * _

        and:
        vo.hasErrors()
        vo.getFieldMessages().containsKey(FieldConstants.DATE_RECEIVED)
    }

    def "test - validateForDelete: Passes if payment is in an open fiscal year"() {
        given:
        int year = 2026
        PaymentVO vo = new PaymentVO(paymentID: 10, dateReceived: LocalDate.of(year, 4, 15))

        when: "validateForDelete is called"
        paymentValidator.validateForDelete(vo)

        then: "No error is attached"
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(year + 1)) >> Optional.empty()
        0 * _

        and:
        !vo.hasErrors()
    }

    def "test - validateForUpdate: Fails if payment is in a closed fiscal year"() {
        given:
        int year = 2025
        PaymentVO vo = new PaymentVO(paymentID: 10, dateReceived: LocalDate.of(year, 4, 15))

        when: "validateForUpdate is called"
        paymentValidator.validateForUpdate(vo)

        then: "Error is attached to dateReceived"
        2 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(year + 1)) >> Optional.of(new SystemSettingsVO())
        0 * _

        and:
        vo.hasErrors()
        vo.getFieldMessages().containsKey(FieldConstants.DATE_RECEIVED)
    }

    def "test - validateForUpdate: Passes closed check if payment is in an open fiscal year"() {
        given:
        int year = 2026
        MemberVO member = new MemberVO(memberID: 1, status: ReferenceConstants.MEMBER_STATUS.ACTIVE)
        PaymentVO vo = new PaymentVO(paymentID: 10, member: member, feeType: ReferenceConstants.FEE_TYPE.REGISTRATION_FEE,
                amount: 50.0, dateReceived: LocalDate.of(year, 4, 15), methodOfPayment: ReferenceConstants.PAYMENT_METHOD.CASH)

        when: "validateForUpdate is called"
        paymentValidator.validateForUpdate(vo)

        then: "No error is attached"
        2 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(year + 1)) >> Optional.empty()
        1 * referenceValidator.validate(vo, FieldConstants.FEE_TYPE, ReferenceConstants.FEE_TYPE.NAME, vo.feeType)
        1 * referenceValidator.validate(vo, FieldConstants.METHOD_OF_PAYMENT, ReferenceConstants.PAYMENT_METHOD.NAME, vo.methodOfPayment)
        0 * _

        and:
        !vo.hasErrors()
    }

    def "test - validate: Member with PENDING status is blocked with ERR_PAYMENT_MEMBER_PENDING when paying non-registration fee"() {
        given: "A non-registration payment for a PENDING member"
        PaymentVO vo = new PaymentVO(
                member: new MemberVO(memberID: 88, status: ReferenceConstants.MEMBER_STATUS.PENDING),
                feeType: ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE,
                amount: new BigDecimal("60.00"),
                dateReceived: LocalDate.now(),
                methodOfPayment: ReferenceConstants.PAYMENT_METHOD.CASH,
                year: 2026,
                quarter: 1
        )

        when: "Validated"
        paymentValidator.validate(vo)

        then:
        1 * referenceValidator.validate(vo, FieldConstants.FEE_TYPE, ReferenceConstants.FEE_TYPE.NAME, vo.feeType)
        1 * referenceValidator.validate(vo, FieldConstants.METHOD_OF_PAYMENT, ReferenceConstants.PAYMENT_METHOD.NAME, vo.methodOfPayment)
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(LocalDate.now().year + 1)) >> Optional.empty()
        1 * memberMembershipPeriod.isMemberActiveInQuarter(88, 2026, 1) >> false
        0 * _

        and:
        vo.hasErrors()
        vo.getFieldMessages().containsKey(FieldConstants.MEMBER_ID)
        vo.getFieldMessages().get(FieldConstants.MEMBER_ID).any { it.message.messageID == ValidationMessages.ERR_PAYMENT_MEMBER_PENDING.messageID }
    }

    def "test - validate: Member with PENDING status is allowed to pay REGISTRATION_FEE"() {
        given: "A REGISTRATION_FEE payment for a PENDING member"
        PaymentVO vo = new PaymentVO(
                member: new MemberVO(memberID: 88, status: ReferenceConstants.MEMBER_STATUS.PENDING),
                feeType: ReferenceConstants.FEE_TYPE.REGISTRATION_FEE,
                amount: new BigDecimal("50.00"),
                dateReceived: LocalDate.now(),
                methodOfPayment: ReferenceConstants.PAYMENT_METHOD.CASH
        )

        when: "Validated"
        paymentValidator.validate(vo)

        then:
        1 * referenceValidator.validate(vo, FieldConstants.FEE_TYPE, ReferenceConstants.FEE_TYPE.NAME, vo.feeType)
        1 * referenceValidator.validate(vo, FieldConstants.METHOD_OF_PAYMENT, ReferenceConstants.PAYMENT_METHOD.NAME, vo.methodOfPayment)
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(LocalDate.now().year + 1)) >> Optional.empty()
        0 * _

        and:
        !vo.hasErrors()
    }

    def "test - validate: Rejects payment for quarter occurring during hiatus"() {
        given: "A member who left in 2026, attempted payment for 2027 Q2"
        PaymentVO vo = new PaymentVO(
                member: new MemberVO(memberID: 77, status: ReferenceConstants.MEMBER_STATUS.INACTIVE),
                feeType: ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE,
                amount: new BigDecimal("60.00"),
                dateReceived: LocalDate.of(2028, 5, 1),
                methodOfPayment: ReferenceConstants.PAYMENT_METHOD.CASH,
                year: 2027,
                quarter: 2
        )

        when: "Validated"
        paymentValidator.validate(vo)

        then:
        1 * referenceValidator.validate(vo, FieldConstants.FEE_TYPE, ReferenceConstants.FEE_TYPE.NAME, vo.feeType)
        1 * referenceValidator.validate(vo, FieldConstants.METHOD_OF_PAYMENT, ReferenceConstants.PAYMENT_METHOD.NAME, vo.methodOfPayment)
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(2029)) >> Optional.empty()
        1 * memberMembershipPeriod.isMemberActiveInQuarter(77, 2027, 2) >> false
        0 * _

        and:
        vo.hasErrors()
        vo.getFieldMessages().containsKey(FieldConstants.QUARTER)
    }

    def "test - validate: Membership payment allowed for inactive member settling historical active arrears"() {
        given: "A member who was active in 2026 Q1, now inactive, settling 2026 Q1 dues"
        PaymentVO vo = new PaymentVO(
                member: new MemberVO(memberID: 77, status: ReferenceConstants.MEMBER_STATUS.INACTIVE),
                feeType: ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE,
                amount: new BigDecimal("60.00"),
                dateReceived: LocalDate.of(2028, 5, 1),
                methodOfPayment: ReferenceConstants.PAYMENT_METHOD.CASH,
                year: 2026,
                quarter: 1
        )

        when: "Validated"
        paymentValidator.validate(vo)

        then: "Payment is accepted because member had an active period covering 2026 Q1"
        1 * referenceValidator.validate(vo, FieldConstants.FEE_TYPE, ReferenceConstants.FEE_TYPE.NAME, vo.feeType)
        1 * referenceValidator.validate(vo, FieldConstants.METHOD_OF_PAYMENT, ReferenceConstants.PAYMENT_METHOD.NAME, vo.methodOfPayment)
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(2029)) >> Optional.empty()
        1 * memberMembershipPeriod.isMemberActiveInQuarter(77, 2026, 1) >> true
        0 * _

        and:
        !vo.hasErrors()
    }

    def "test - validate: Active member payment for valid quarter succeeds"() {
        given: "A membership payment for an eligible quarter"
        PaymentVO vo = new PaymentVO(
                member: new MemberVO(memberID: 99, status: ReferenceConstants.MEMBER_STATUS.ACTIVE),
                feeType: ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE,
                amount: new BigDecimal("60.00"),
                dateReceived: LocalDate.of(2026, 5, 1),
                methodOfPayment: ReferenceConstants.PAYMENT_METHOD.CASH,
                year: 2026,
                quarter: 2
        )

        when: "Validated"
        paymentValidator.validate(vo)

        then: "Quarter eligibility check succeeds"
        1 * referenceValidator.validate(vo, FieldConstants.FEE_TYPE, ReferenceConstants.FEE_TYPE.NAME, vo.feeType)
        1 * referenceValidator.validate(vo, FieldConstants.METHOD_OF_PAYMENT, ReferenceConstants.PAYMENT_METHOD.NAME, vo.methodOfPayment)
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(2027)) >> Optional.empty()
        1 * memberMembershipPeriod.isMemberActiveInQuarter(99, 2026, 2) >> true
        0 * _

        and:
        !vo.hasErrors()
    }

    def "test - validateForDelete: Passes when dateReceived is null"() {
        given: "A payment without dateReceived"
        PaymentVO vo = new PaymentVO(paymentID: 10, dateReceived: null)

        when:
        paymentValidator.validateForDelete(vo)

        then:
        0 * _

        and:
        !vo.hasErrors()
    }

    def "test - validateForUpdate: Passes record not closed check when dateReceived is null"() {
        given: "A payment for update without dateReceived"
        MemberVO member = new MemberVO(memberID: 1, status: ReferenceConstants.MEMBER_STATUS.ACTIVE)
        PaymentVO vo = new PaymentVO(paymentID: 10, member: member, feeType: ReferenceConstants.FEE_TYPE.REGISTRATION_FEE,
                amount: 50.0, dateReceived: null, methodOfPayment: ReferenceConstants.PAYMENT_METHOD.CASH)

        when:
        paymentValidator.validateForUpdate(vo)

        then:
        1 * referenceValidator.validate(vo, FieldConstants.FEE_TYPE, ReferenceConstants.FEE_TYPE.NAME, vo.feeType)
        1 * referenceValidator.validate(vo, FieldConstants.METHOD_OF_PAYMENT, ReferenceConstants.PAYMENT_METHOD.NAME, vo.methodOfPayment)
        0 * _

        and:
        vo.hasErrors()
        vo.getFieldMessages().containsKey(FieldConstants.DATE_RECEIVED)
    }

    def "test - validate: Membership payment with null member safely bypasses active period check"() {
        given: "A membership fee payment with null member"
        PaymentVO vo = new PaymentVO(
                member: null,
                feeType: ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE,
                amount: new BigDecimal("60.00"),
                dateReceived: LocalDate.of(2026, 5, 1),
                methodOfPayment: ReferenceConstants.PAYMENT_METHOD.CASH,
                year: 2026,
                quarter: 2
        )

        when: "Validated"
        paymentValidator.validate(vo)

        then: "Member required triggers and memberMembershipPeriod is never called"
        1 * referenceValidator.validate(vo, FieldConstants.FEE_TYPE, ReferenceConstants.FEE_TYPE.NAME, vo.feeType)
        1 * referenceValidator.validate(vo, FieldConstants.METHOD_OF_PAYMENT, ReferenceConstants.PAYMENT_METHOD.NAME, vo.methodOfPayment)
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(2027)) >> Optional.empty()
        0 * _

        and:
        vo.hasErrors()
        vo.getFieldMessages().containsKey(FieldConstants.MEMBER_ID)
    }

    def "test - validate: Membership payment with member having null memberID safely bypasses active period check"() {
        given: "A membership fee payment with member having null memberID"
        PaymentVO vo = new PaymentVO(
                member: new MemberVO(memberID: null, status: ReferenceConstants.MEMBER_STATUS.ACTIVE),
                feeType: ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE,
                amount: new BigDecimal("60.00"),
                dateReceived: LocalDate.of(2026, 5, 1),
                methodOfPayment: ReferenceConstants.PAYMENT_METHOD.CASH,
                year: 2026,
                quarter: 2
        )

        when:
        paymentValidator.validate(vo)

        then:
        1 * referenceValidator.validate(vo, FieldConstants.FEE_TYPE, ReferenceConstants.FEE_TYPE.NAME, vo.feeType)
        1 * referenceValidator.validate(vo, FieldConstants.METHOD_OF_PAYMENT, ReferenceConstants.PAYMENT_METHOD.NAME, vo.methodOfPayment)
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(2027)) >> Optional.empty()
        0 * _

        and:
        vo.hasErrors()
        vo.getFieldMessages().containsKey(FieldConstants.MEMBER_ID)
    }
}