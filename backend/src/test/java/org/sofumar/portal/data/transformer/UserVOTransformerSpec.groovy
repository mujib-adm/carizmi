package org.sofumar.portal.data.transformer

import org.sofumar.portal.core.vo.UserVO
import org.sofumar.portal.data.dto.UserDto
import org.sofumar.portal.testsupport.BaseSpecification

class UserVOTransformerSpec extends BaseSpecification {

    UserVOTransformer transformer = new UserVOTransformer()

    def "test - transform: Should transform UserDto to UserVO"() {
        given: "TestData setup"
        UserDto dto = new UserDto(
                username: "jsmith",
                firstName: "Jane",
                lastName: "Smith",
                email: "jsmith@example.com",
                password: "password123"
        )

        when: "The target method executed"
        UserVO result = transformer.transform(dto)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result != null
        result.username == dto.username
        result.firstName == dto.firstName
        result.lastName == dto.lastName
        result.email == dto.email
        result.password == dto.password
        noExceptionThrown()
    }

    def "test - transform: Should return null when input is null"() {
        when: "The target method executed"
        UserVO result = transformer.transform(null)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result == null
        noExceptionThrown()
    }

    def "test - transformList: Should transform list of UserDtos"() {
        given: "TestData setup"
        UserDto dto1 = new UserDto(username: "u1")
        UserDto dto2 = new UserDto(username: "u2")
        List<UserDto> list = [dto1, null, dto2]

        when: "The target method executed"
        List<UserVO> result = transformer.transformList(list)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result.size() == 2
        result[0].username == "u1"
        result[1].username == "u2"
        noExceptionThrown()
    }

    def "test - transformList: Should return empty list when input is null"() {
        when: "The target method executed"
        List<UserVO> result = transformer.transformList(null)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result != null
        result.isEmpty()
        noExceptionThrown()
    }
}