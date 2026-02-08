package org.sofumar.portal.data.transformer

import org.sofumar.portal.constants.Role
import org.sofumar.portal.core.vo.UserVO
import org.sofumar.portal.data.dto.response.UserResponseDto
import org.sofumar.portal.testsupport.BaseSpecification

class UserResponseDtoTransformerSpec extends BaseSpecification {

    UserResponseDtoTransformer transformer = new UserResponseDtoTransformer()

    def "test - transform: Should transform UserVO to UserResponseDto"() {
        given: "TestData setup"
        UserVO vo = new UserVO(
                userID: 1,
                username: "jdoe",
                email: "jdoe@example.com",
                firstName: "John",
                lastName: "Doe",
                role: Role.ADMIN,
                active: true
        )

        when: "The target method executed"
        UserResponseDto result = transformer.transform(vo)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result != null
        result.userID == vo.userID
        result.username == vo.username
        result.email == vo.email
        result.firstName == vo.firstName
        result.lastName == vo.lastName
        result.role == vo.role.name()
        result.isActive() == vo.isActive()
        noExceptionThrown()
    }

    def "test - transform: Should return null when input is null"() {
        when: "The target method executed"
        UserResponseDto result = transformer.transform(null)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result == null
        noExceptionThrown()
    }

    def "test - transformList: Should transform list of UserVOs"() {
        given: "TestData setup"
        UserVO vo1 = new UserVO(userID: 1, username: "u1")
        UserVO vo2 = new UserVO(userID: 2, username: "u2")
        List<UserVO> list = [vo1, null, vo2]

        when: "The target method executed"
        List<UserResponseDto> result = transformer.transformList(list)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result.size() == 2
        result[0].userID == 1
        result[1].userID == 2
        noExceptionThrown()
    }

    def "test - transformList: Should return empty list when input is null"() {
        when: "The target method executed"
        List<UserResponseDto> result = transformer.transformList(null)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result != null
        result.isEmpty()
        noExceptionThrown()
    }
}