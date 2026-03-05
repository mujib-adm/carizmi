package org.sofumar.portal.service.validation

import org.sofumar.portal.constants.FieldConstants
import org.sofumar.portal.constants.Role
import org.sofumar.portal.core.vo.UserVO
import org.sofumar.portal.testbase.BaseSpecification
import spock.lang.Subject
import spock.lang.Unroll

class UserValidatorSpec extends BaseSpecification {

    @Subject
    UserValidator userValidator = new UserValidator()

    def "test - validate: Should pass for valid VO"() {
        given: "A valid UserVO"
        UserVO vo = createValidVO()

        when: "The target method executed"
        userValidator.validate(vo)

        then: "The expected calls"
        0 * _

        and: "The expected result"
        !vo.hasErrors()
        noExceptionThrown()
    }

    @Unroll
    def "test - validate: first/last name validations [field: #field, value: #value, isValid: #isValid]"() {
        given: "A UserVO with name variation"
        UserVO vo = createValidVO()
        if (field == FieldConstants.FIRST_NAME) vo.firstName = value
        if (field == FieldConstants.LAST_NAME) vo.lastName = value

        when: "The target method executed"
        userValidator.validate(vo)

        then: "The expected calls"
        0 * _

        and: "The expected result"
        if (isValid) {
            !vo.hasErrors()
        } else {
            vo.hasErrors()
            vo.getFieldMessages().containsKey(field)
        }

        where:
        field                     | value  | isValid
        FieldConstants.FIRST_NAME | "John" | true
        FieldConstants.FIRST_NAME | ""     | false
        FieldConstants.FIRST_NAME | null   | false
        FieldConstants.LAST_NAME  | "Doe"  | true
        FieldConstants.LAST_NAME  | ""     | false
        FieldConstants.LAST_NAME  | null   | false
    }

    @Unroll
    def "test - validate: email validations [email: #email, isValid: #isValid]"() {
        given: "A UserVO with email variation"
        UserVO vo = createValidVO()
        vo.email = email

        when: "The target method executed"
        userValidator.validate(vo)

        then: "The expected calls"
        0 * _

        and: "The expected result"
        if (isValid) {
            !vo.hasErrors()
        } else {
            vo.hasErrors()
            vo.getFieldMessages().containsKey(FieldConstants.EMAIL)
        }

        where:
        email               | isValid
        "john.doe@test.com" | true
        "invalid-email"     | false
        ""                  | false
        null                | false
    }

    @Unroll
    def "test - validate: username validations [username: #username, isValid: #isValid]"() {
        given: "A UserVO with username variation"
        UserVO vo = createValidVO()
        vo.username = username

        when: "The target method executed"
        userValidator.validate(vo)

        then: "The expected calls"
        0 * _

        and: "The expected result"
        if (isValid) {
            !vo.hasErrors()
        } else {
            vo.hasErrors()
            vo.getFieldMessages().containsKey(FieldConstants.USERNAME)
        }

        where:
        username      | isValid
        "user123"     | true
        "abc.def-ghi" | true
        "xyz"         | false // too short
        ""            | false // blank
        null          | false // missing
        "user space"  | false // invalid char
    }

    @Unroll
    def "test - validatePassword: password validations [password: #password, isValid: #isValid]"() {
        given: "A UserVO with password variation"
        UserVO vo = createValidVO()
        vo.password = password

        when: "The target method executed"
        userValidator.validatePassword(vo)

        then: "The expected result"
        if (isValid) {
            !vo.hasErrors()
        } else {
            vo.hasErrors()
            vo.getFieldMessages().containsKey(FieldConstants.PASSWORD)
        }

        where:
        password         | isValid
        "Secure123!"     | true
        "aB1!"           | false // too short
        "alllowercase1!" | false // missing uppercase
        "ALLUPPERCASE1!" | false // missing lowercase
        "NoDigitNoSpec"  | false // missing digit/spec
        ""               | false // blank
        null             | false // missing
    }

    @Unroll
    def "test - validate: role validations [role: #role, isValid: #isValid]"() {
        given: "A UserVO with role variation"
        UserVO vo = createValidVO()
        vo.role = role

        when: "The target method executed"
        userValidator.validate(vo)

        then: "The expected result"

        and: "The expected result"
        if (isValid) {
            !vo.hasErrors()
        } else {
            vo.hasErrors()
            vo.getFieldMessages().containsKey(FieldConstants.ROLE)
        }

        where:
        role        | isValid
        Role.ADMIN  | true
        Role.MEMBER | true
        null        | false
    }

    @Unroll
    def "test - isInvalidRole: Testing various role strings [roleStr: #roleStr, expected: #expected]"() {
        expect:
        userValidator.isInvalidRole(roleStr) == expected

        where:
        roleStr   | expected
        "ADMIN"   | false
        "MANAGER" | false
        "MEMBER"  | false
        "INVALID" | true
        ""        | true
        " "       | true
        null      | true
    }

    @Unroll
    def "test - validateRole: direct method call [role: #role, isValid: #isValid]"() {
        given: "A UserVO with role variation"
        UserVO vo = createValidVO()
        vo.role = role

        when: "The target method executed"
        userValidator.validateRole(vo)

        then: "The expected result"
        if (isValid) {
            !vo.hasErrors()
        } else {
            vo.hasErrors()
            vo.getFieldMessages().containsKey(FieldConstants.ROLE)
        }

        where:
        role        | isValid
        Role.ADMIN  | true
        Role.MEMBER | true
        null        | false
    }

    def "test - validateRole: invalid role branch coverage using Spy"() {
        given: "A spy of UserValidator"
        UserValidator validatorSpy = Spy(UserValidator)
        UserVO vo = createValidVO()
        vo.role = Role.ADMIN

        when: "We force isInvalidRole to return true"
        validatorSpy.isInvalidRole(_) >> true
        validatorSpy.validate(vo)

        then: "It hits the invalid role branch and adds an error message"
        vo.hasErrors()
        vo.getFieldMessages().containsKey(FieldConstants.ROLE)
    }

    private static UserVO createValidVO() {
        return new UserVO(
                firstName: "John",
                lastName: "Doe",
                email: "john.doe@test.com",
                username: "validUser",
                password: "SecurePassword123!",
                role: Role.ADMIN
        )
    }
}