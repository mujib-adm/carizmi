package org.sofumar.portal.service.validation

import org.sofumar.portal.constants.FieldConstants
import org.sofumar.portal.constants.Role
import org.sofumar.portal.core.vo.UserVO
import org.sofumar.portal.framework.exception.ValidationException
import org.sofumar.portal.testsupport.BaseSpecification
import spock.lang.Subject
import spock.lang.Unroll

class UserValidatorSpec extends BaseSpecification {

    @Subject
    UserValidator userValidator = new UserValidator()

    def "test - validate: Should pass for valid VO"() {
        given: "A valid UserVO"
        String username = "validUser.123"
        String password = "SecurePassword123!"
        UserVO vo = new UserVO(username: username, password: password, role: Role.ADMIN)

        when: "The target method executed"
        userValidator.validate(vo)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        !vo.hasErrors()
        noExceptionThrown()
    }

    @Unroll
    def "test - validate: Handling username validations [username: #username, isValid: #isValid]"() {
        given: "A UserVO with username variation"
        UserVO vo = new UserVO(username: username, password: "SecurePassword123!")

        when: "The target method executed"
        try {
            userValidator.validate(vo)
        } catch (ValidationException e) {
            // expected
        }

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        if (isValid) {
            !vo.hasErrors()
        } else {
            vo.hasErrors()
            vo.getFieldMessages().containsKey(FieldConstants.USERNAME)
        }
        noExceptionThrown()

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
    def "test - validate: Handling password validations [password: #password, isValid: #isValid]"() {
        given: "A UserVO with password variation"
        UserVO vo = new UserVO(username: "validUser", password: password)

        when: "The target method executed"
        try {
            userValidator.validate(vo)
        } catch (ValidationException e) {
            // expected
        }

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        if (isValid) {
            !vo.hasErrors()
        } else {
            vo.hasErrors()
            vo.getFieldMessages().containsKey(FieldConstants.PASSWORD)
        }
        noExceptionThrown()

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
    def "test - validate: Handling role validations [role: #role, isValid: #isValid]"() {
        given: "A UserVO with role variation"
        UserVO vo = new UserVO(username: "validUser", password: "validPassword")
        try {
            vo.role = Role.valueOf(role.toString())
        } catch (IllegalArgumentException e) {
            // leave null
        }

        when: "The target method executed"
        try {
            userValidator.validate(vo)
        } catch (ValidationException e) {
            // expected
        }

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        if (isValid) {
            !vo.hasErrors()
        } else {
            vo.hasErrors()
            vo.getFieldMessages().containsKey(FieldConstants.ROLE)
        }
        noExceptionThrown()

        where:
        role                            | isValid
        Role.ADMIN.name()               | true
        Role.MEMBER.name()              | true
        "INVALID_ROLE"                  | false
        Role.ADMIN.name().toLowerCase() | false // case-sensitive
        ""                              | false
        "   "                           | false
        null                            | false
    }
}