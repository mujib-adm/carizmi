package org.sofumar.portal.service.validation

import org.sofumar.portal.constants.FieldConstants
import org.sofumar.portal.constants.ReferenceConstants
import org.sofumar.portal.core.vo.MemberVO
import org.sofumar.portal.testbase.BaseSpecification
import spock.lang.Subject
import spock.lang.Unroll

class MemberValidatorSpec extends BaseSpecification {

    ReferenceValidator referenceValidator = Mock()

    @Subject
    MemberValidator memberValidator = new MemberValidator(referenceValidator)

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
}