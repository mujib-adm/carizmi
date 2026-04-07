package org.sofumar.portal.controller

import org.sofumar.portal.core.businesslogic.User
import org.sofumar.portal.data.dto.request.UserRoleUpdateRequestDto
import org.sofumar.portal.data.dto.request.UserStatusUpdateRequestDto
import org.sofumar.portal.data.dto.response.UserResponseDto
import org.sofumar.portal.framework.data.response.GlobalResponse
import org.sofumar.portal.testbase.BaseSpecification
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Subject

class UserControllerSpec extends BaseSpecification {

    User user = Mock()

    @Subject
    UserController userController = new UserController(user)

    def "test - getAllUsers: Should delegate to user service and wrap result"() {
        given: "A request for all users"
        List<UserResponseDto> dtoList = [UserResponseDto.builder().username("user1").build()]

        when: "The target method executed"
        ResponseEntity<GlobalResponse<List<UserResponseDto>>> result = userController.getAllUsers()

        then: "The expected calls are made"
        1 * user.getAllUsers() >> dtoList
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.OK
        result.body.responseData.size() == 1
        result.body.responseData[0].username == "user1"
        noExceptionThrown()
    }

    def "test - updateRole: Should delegate to user service and wrap result"() {
        given: "A user ID and role update request"
        Integer id = 1
        String role = "ADMIN"
        UserRoleUpdateRequestDto request = new UserRoleUpdateRequestDto(role: role)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<Void>> result = userController.updateRole(id, request)

        then: "The expected calls are made"
        1 * user.updateUserRole(id, role)
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.OK
        noExceptionThrown()
    }

    def "test - toggleStatus: Should delegate to user service and wrap result"() {
        given: "A user ID and status update request"
        Integer id = 1
        Boolean active = true
        UserStatusUpdateRequestDto request = new UserStatusUpdateRequestDto(active: active)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<Void>> result = userController.toggleStatus(id, request)

        then: "The expected calls are made"
        1 * user.toggleUserStatus(id, active)
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.OK
        noExceptionThrown()
    }
}