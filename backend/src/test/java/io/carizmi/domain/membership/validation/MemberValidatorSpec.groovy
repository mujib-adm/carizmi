package io.carizmi.domain.membership.validation

import io.carizmi.domain.platform.validation.ReferenceValidator

import io.carizmi.shared.constants.FieldConstants
import io.carizmi.shared.constants.ReferenceConstants
import io.carizmi.domain.membership.model.MemberVO
import io.carizmi.testbase.BaseSpecification
import spock.lang.Subject
import spock.lang.Unroll
import io.carizmi.domain.finance.service.Payment
import io.carizmi.domain.platform.constants.BaselineConstants
import io.carizmi.domain.platform.model.SystemSettingsVO
import io.carizmi.domain.platform.service.SystemSetting
import java.time.LocalDate

class MemberValidatorSpec extends BaseSpecification {

    ReferenceValidator referenceValidator = Mock()
    Payment payment = Mock()
    SystemSetting systemSetting = Mock()

    @Subject
    MemberValidator memberValidator = new MemberValidator(referenceValidator, payment, systemSetting)

    def "test - validate: Should pass for valid VO"() {
        given: "A valid MemberVO"
        String firstName = "John"
        String lastName = "Doe"
        String phone = "123-456-7890"
        String email = "john.doe@example.com"
        String status = ReferenceConstants.MEMBER_STATUS.ACTIVE
        String state = "MN"
        String zip = "12345"
        MemberVO vo = new MemberVO(firstName: firstName, lastName: lastName, phone: phone, email: email, status: status, state: state, zip: zip)

        String fieldName = FieldConstants.STATUS
        String referenceName = ReferenceConstants.MEMBER_STATUS.NAME

        when: "The target method executed"
        memberValidator.validate(vo)

        then: "The expected calls are made"
        1 * referenceValidator.validate(vo, fieldName, referenceName, status)
        0 * _

        and: "The expected result"
        !vo.hasErrors()
        noExceptionThrown()
    }

    @Unroll
    def "test - validate: Handling basic field validations [field: #field, value: #value]"() {
        given: "A MemberVO with a specific field variation"
        String defaultStatus = ReferenceConstants.MEMBER_STATUS.ACTIVE
        MemberVO vo = new MemberVO(
                firstName: field == FieldConstants.FIRST_NAME ? value : "John",
                lastName: field == FieldConstants.LAST_NAME ? value : "Doe",
                phone: field == FieldConstants.PHONE ? value : "123-456-7890",
                status: field == FieldConstants.STATUS ? value : defaultStatus,
                state: field == FieldConstants.STATE ? value : "MN"
        )

        String fieldName = FieldConstants.STATUS
        String referenceName = ReferenceConstants.MEMBER_STATUS.NAME

        when: "The target method executed"
        memberValidator.validate(vo)

        then: "The expected calls are made"
        if (field != fieldName || value) {
            String arg = (field == fieldName) ? value : defaultStatus
            1 * referenceValidator.validate(vo, fieldName, referenceName, arg)
        }
        0 * _

        and: "The expected result"
        vo.hasErrors()
        vo.getFieldMessages().containsKey(field)
        noExceptionThrown()

        where:
        field                     | value
        FieldConstants.FIRST_NAME | null
        FieldConstants.FIRST_NAME | ""
        FieldConstants.LAST_NAME  | null
        FieldConstants.LAST_NAME  | ""
        FieldConstants.PHONE      | null
        FieldConstants.PHONE      | ""
        FieldConstants.STATUS     | null
        FieldConstants.STATUS     | ""
        FieldConstants.STATE      | null
        FieldConstants.STATE      | ""
    }

    @Unroll
    def "test - validate: Handling regex validations [field: #field, value: #value, isValid: #isValid]"() {
        given: "A MemberVO with regex-validated field"
        String defaultStatus = ReferenceConstants.MEMBER_STATUS.ACTIVE
        MemberVO vo = new MemberVO(
                firstName: "John", lastName: "Doe", status: defaultStatus, state: "MN", phone: "123-456-7890"
        )
        vo."$field" = value

        String fieldName = FieldConstants.STATUS
        String referenceName = ReferenceConstants.MEMBER_STATUS.NAME

        when: "The target method executed"
        memberValidator.validate(vo)

        then: "The expected calls are made"
        1 * referenceValidator.validate(vo, fieldName, referenceName, defaultStatus)
        0 * _

        and: "The expected result"
        if (isValid) {
            !vo.hasErrors()
        } else {
            vo.hasErrors()
            vo.getFieldMessages().containsKey(field)
        }
        noExceptionThrown()

        where:
        field                | value            | isValid
        FieldConstants.PHONE | "1234567890"     | true
        FieldConstants.PHONE | "(123) 456-7890" | true
        FieldConstants.PHONE | "123-456-789"    | false
        FieldConstants.PHONE | "abc-def-ghij"   | false
        FieldConstants.EMAIL | "test@test.com"  | true
        FieldConstants.EMAIL | "invalid-email"  | false
        FieldConstants.ZIP   | "12345"          | true
        FieldConstants.ZIP   | "12345-6789"     | true
        FieldConstants.ZIP   | "1234"           | false
    }

    def "test - validateForUpdate: Should validate memberID and call validate"() {
        given: "A MemberVO for update"
        Integer memberID = 1
        String status = ReferenceConstants.MEMBER_STATUS.ACTIVE
        MemberVO vo = new MemberVO(memberID: memberID, firstName: "John", lastName: "Doe", phone: "123-456-7890", status: status, state: "MN")

        String fieldName = FieldConstants.STATUS
        String referenceName = ReferenceConstants.MEMBER_STATUS.NAME

        when: "The target method executed"
        memberValidator.validateForUpdate(vo)

        then: "The expected calls are made"
        1 * referenceValidator.validate(vo, fieldName, referenceName, status)
        0 * _

        and: "The expected result"
        !vo.hasErrors()
        noExceptionThrown()
    }

    def "test - validateForUpdate: Should catch missing memberID"() {
        given: "A MemberVO for update without ID"
        String status = ReferenceConstants.MEMBER_STATUS.ACTIVE
        MemberVO vo = new MemberVO(firstName: "John", lastName: "Doe", phone: "123-456-7890", status: status, state: "MN")

        String fieldName = FieldConstants.STATUS
        String referenceName = ReferenceConstants.MEMBER_STATUS.NAME

        when: "The target method executed"
        memberValidator.validateForUpdate(vo)

        then: "The expected calls are made"
        1 * referenceValidator.validate(vo, fieldName, referenceName, status)
        0 * _

        and: "The expected result"
        noExceptionThrown()
        vo.hasErrors()
        vo.getFieldMessages().containsKey(FieldConstants.MEMBER_ID)
    }

    @Unroll
    def "test - isNotMatchRegex: Internal helper coverage [value: #value, expected: #expected]"() {
        given: "A value and a regex"
        String regex = "^[abc]+\$"

        when: "The internal helper is called"
        boolean result = memberValidator.isNotMatchRegex(value, regex)

        then: "No mock interactions occurred"
        0 * _

        and: "The result matches expectation"
        result == expected

        where:
        value | expected
        "abc" | false
        "def" | true
        ""    | true
        null  | true
    }


    def "test - validateForDelete: Rejects deletion when member has payment history"() {
        given: "A member with payments recorded"
        MemberVO vo = new MemberVO(memberID: 42, joinDate: LocalDate.of(2026, 1, 1))

        when: "validateForDelete is called"
        memberValidator.validateForDelete(vo)

        then: "payment check returns true and error is captured"
        1 * payment.hasPaymentsForMember(42) >> true
        0 * _

        and:
        vo.hasErrors()
    }

    def "test - validateForDelete: Rejects deletion when member was active in a closed fiscal year"() {
        given: "A member with no payments but joinDate in a closed year"
        MemberVO vo = new MemberVO(memberID: 42, joinDate: LocalDate.of(2025, 5, 1))
        String closingKey = BaselineConstants.getYearlyBaselineKey(2026)

        when: "validateForDelete is called"
        memberValidator.validateForDelete(vo)

        then: "payment check returns false but baseline exists for closed year"
        1 * payment.hasPaymentsForMember(42) >> false
        1 * systemSetting.findBySettingKey(closingKey) >> Optional.of(new SystemSettingsVO())
        0 * _

        and:
        vo.hasErrors()
    }

    def "test - validateForDelete: Allows deletion when member has no payments and joinDate is in open year"() {
        given: "A member with no payments and joinDate in an open year"
        MemberVO vo = new MemberVO(memberID: 42, joinDate: LocalDate.of(2026, 5, 1))
        String closingKey = BaselineConstants.getYearlyBaselineKey(2027)

        when: "validateForDelete is called"
        memberValidator.validateForDelete(vo)

        then: "payment check returns false and year is open"
        1 * payment.hasPaymentsForMember(42) >> false
        1 * systemSetting.findBySettingKey(closingKey) >> Optional.empty()
        0 * _

        and:
        !vo.hasErrors()
    }
}