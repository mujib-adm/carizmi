package org.sofumar.portal.controller

import org.sofumar.portal.core.businesslogic.User
import org.sofumar.portal.data.dto.UserDto
import org.sofumar.portal.data.dto.request.PasswordUpdateRequestDto
import org.sofumar.portal.data.dto.response.UserProfileDto
import org.sofumar.portal.framework.data.response.GlobalResponse
import org.sofumar.portal.testsupport.BaseSpecification
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.userdetails.UserDetails
import spock.lang.Subject
import spock.lang.Unroll

class AuthControllerSpec extends BaseSpecification {

    User user = Mock()

    @Subject
    AuthController authController = new AuthController(user)

    def "test - register: Should delegate to user service"() {
        given: "A registration request"
        String username = "Test"
        UserDto requestDto = new UserDto(username: username)
        ResponseEntity<GlobalResponse<Void>> expectedResponse = new ResponseEntity<>(HttpStatus.OK)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<Void>> result = authController.register(requestDto)

        then: "The expected calls are made"
        1 * user.register(requestDto) >> expectedResponse
        0 * _

        and: "The expected result"
        result == expectedResponse
        result.statusCode == HttpStatus.OK
        noExceptionThrown()
    }

    @Unroll
    def "test - logout: Handling header and body permutations [header: #header, refreshBody: #refreshBody]"() {
        given: "Logout payloads"
        Map<String, String> body = refreshBody ? [refreshToken: refreshBody] : null
        String bearerPrefix = "Bearer "
        int prefixLength = bearerPrefix.length()
        String expectedAccessToken = header?.startsWith(bearerPrefix) ? header.substring(prefixLength) : null
        String expectedMessage = "Successfully logged out"

        when: "The target method executed"
        ResponseEntity<?> result = authController.logout(header, body)

        then: "The expected calls are made"
        1 * user.logout(expectedAccessToken, refreshBody)
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.OK
        ((Map) result.body).message == expectedMessage
        noExceptionThrown()

        where:
        header           | refreshBody
        "Bearer access"  | "refresh"
        "Invalid header" | "refresh"
        null             | "refresh"
        "Bearer access"  | null
    }

    @Unroll
    def "test - refreshToken: Handling presence of token [token: #token, expectedStatus: #expectedStatus]"() {
        given: "A refresh request"
        String tokenKey = "refreshToken"
        Map<String, String> body = token ? [(tokenKey): token] : [:]
        ResponseEntity<?> serviceResponse = new ResponseEntity<>(HttpStatus.OK)

        when: "The target method executed"
        ResponseEntity<?> result = authController.refreshToken(body)

        then: "The expected calls are made"
        if (token) {
            1 * user.refreshToken(token) >> serviceResponse
        }
        0 * _

        and: "The expected result"
        result.statusCode == expectedStatus
        if (expectedStatus == HttpStatus.OK) {
            result == serviceResponse
        }
        noExceptionThrown()

        where:
        token     | expectedStatus
        "refresh" | HttpStatus.OK
        null      | HttpStatus.BAD_REQUEST
    }

    @Unroll
    def "test - getCurrentUser: Handling userDetails state [details: #details, expectedStatus: #expectedStatus]"() {
        given: "A profile request"
        String username = "Test"
        UserDetails userDetails = null
        if (details) {
            userDetails = Mock(UserDetails)
            userDetails.getUsername() >> username
        }
        ResponseEntity<GlobalResponse<UserProfileDto>> serviceResponse = new ResponseEntity<>(HttpStatus.OK)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<UserProfileDto>> result = authController.getCurrentUser(userDetails)

        then: "The expected calls are made"
        if (details) {
            1 * userDetails.getUsername() >> username
            1 * user.getProfile(username) >> serviceResponse
        }
        0 * _

        and: "The expected result"
        result.statusCode == expectedStatus
        if (details) {
            result == serviceResponse
        }
        noExceptionThrown()

        where:
        details | expectedStatus
        true    | HttpStatus.OK
        false   | HttpStatus.UNAUTHORIZED
    }

    @Unroll
    def "test - updatePassword: Handling userDetails and header [details: #details, header: #header, expectedStatus: #expectedStatus]"() {
        given: "A password update request"
        String username = "Test"
        UserDetails userDetails = null
        if (details) {
            userDetails = Mock(UserDetails)
            userDetails.getUsername() >> username
        }
        PasswordUpdateRequestDto requestDto = new PasswordUpdateRequestDto()
        ResponseEntity<GlobalResponse<Void>> serviceResponse = new ResponseEntity<>(HttpStatus.OK)
        String bearerPrefix = "Bearer "
        int prefixLength = bearerPrefix.length()
        String expectedToken = header?.startsWith(bearerPrefix) ? header.substring(prefixLength) : null

        when: "The target method executed"
        ResponseEntity<GlobalResponse<Void>> result = authController.updatePassword(userDetails, header, requestDto)

        then: "The expected calls are made"
        if (details) {
            1 * userDetails.getUsername() >> username
            1 * user.updatePassword(username, expectedToken, requestDto) >> serviceResponse
        }
        0 * _

        and: "The expected result"
        result.statusCode == expectedStatus
        if (details) {
            result == serviceResponse
        }
        noExceptionThrown()

        where:
        details | header         | expectedStatus
        true    | "Bearer token" | HttpStatus.OK
        true    | "Invalid"      | HttpStatus.OK
        true    | null           | HttpStatus.OK
        false   | "Bearer token" | HttpStatus.UNAUTHORIZED
    }
}