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

    def "test - getAllUsers: Should delegate to user service"() {
        given: "A request for all users"
        String username = "user1"
        Integer expectedSize = 1
        List<UserResponseDto> dtoList = [UserResponseDto.builder().username(username).build()]
        GlobalResponse<List<UserResponseDto>> responseBody = GlobalResponse.withResponseData(dtoList)
        ResponseEntity<GlobalResponse<List<UserResponseDto>>> expectedResponse = new ResponseEntity<>(responseBody, HttpStatus.OK)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<List<UserResponseDto>>> result = userController.getAllUsers()

        then: "The expected calls are made"
        1 * user.getAllUsers() >> expectedResponse
        0 * _

        and: "The expected result"
        result == expectedResponse
        result.body.responseData.size() == expectedSize
        result.body.responseData[0].username == username
        noExceptionThrown()
    }

    def "test - updateRole: Should delegate to user service"() {
        given: "A userVO ID and role update request"
        Integer id = 1
        String role = "ADMIN"
        UserRoleUpdateRequestDto request = new UserRoleUpdateRequestDto(role: role)
        ResponseEntity<GlobalResponse<Void>> expectedResponse = new ResponseEntity<>(HttpStatus.OK)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<Void>> result = userController.updateRole(id, request)

        then: "The expected calls are made"
        1 * user.updateUserRole(id, role) >> expectedResponse
        0 * _

        and: "The expected result"
        result == expectedResponse
        result.statusCode == HttpStatus.OK
        noExceptionThrown()
    }

    def "test - toggleStatus: Should delegate to user service"() {
        given: "A userVO ID and status update request"
        Integer id = 1
        Boolean active = true
        UserStatusUpdateRequestDto request = new UserStatusUpdateRequestDto(active: active)
        ResponseEntity<GlobalResponse<Void>> expectedResponse = new ResponseEntity<>(HttpStatus.OK)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<Void>> result = userController.toggleStatus(id, request)

        then: "The expected calls are made"
        1 * user.toggleUserStatus(id, active) >> expectedResponse
        0 * _

        and: "The expected result"
        result == expectedResponse
        result.statusCode == HttpStatus.OK
        noExceptionThrown()
    }
}