package org.sofumar.portal.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.sofumar.portal.core.businesslogic.User
import org.sofumar.portal.data.dto.UserDto
import org.sofumar.portal.data.dto.request.PasswordUpdateRequestDto
import org.sofumar.portal.data.dto.response.UserProfileDto
import org.sofumar.portal.framework.data.response.GlobalResponse
import org.sofumar.portal.testbase.BaseSpecification
import org.sofumar.portal.security.CookieService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.userdetails.UserDetails
import spock.lang.Subject
import spock.lang.Unroll

class AuthControllerSpec extends BaseSpecification {

    User user = Mock()
    CookieService cookieService = Mock()

    @Subject
    AuthController authController = new AuthController(user, cookieService)

    HttpServletRequest request = Mock()
    HttpServletResponse response = Mock()

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
    def "test - logout: Should read tokens from cookies, logout, and clear cookies [access: #accessToken, refresh: #refreshToken]"() {
        given: "Cookie values"
        String expectedMessage = "Successfully logged out"

        when: "The target method executed"
        ResponseEntity<?> result = authController.logout(request, response)

        then: "The expected calls are made"
        1 * cookieService.getAccessToken(request) >> Optional.ofNullable(accessToken)
        1 * cookieService.getRefreshToken(request) >> Optional.ofNullable(refreshToken)
        1 * user.logout(accessToken, refreshToken)
        1 * cookieService.clearAuthCookies(response)
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.OK
        ((Map) result.body).message == expectedMessage
        noExceptionThrown()

        where:
        accessToken | refreshToken
        "access"    | "refresh"
        null        | "refresh"
        "access"    | null
        null        | null
    }

    @Unroll
    def "test - refreshToken: Should return bad request when no refresh token [token: #token, expectedStatus: #expectedStatus]"() {
        given: "A refresh request"
        ResponseEntity<?> serviceResponse = new ResponseEntity<>(HttpStatus.OK)

        when: "The target method executed"
        ResponseEntity<?> result = authController.refreshToken(request, response)

        then: "The expected calls are made"
        1 * cookieService.getRefreshToken(request) >> Optional.ofNullable(token)
        if (token) {
            1 * user.refreshToken(token) >> serviceResponse
        }
        0 * _

        and: "The expected result"
        result.statusCode == expectedStatus
        noExceptionThrown()

        where:
        token     | expectedStatus
        "refresh" | HttpStatus.OK
        null      | HttpStatus.BAD_REQUEST
    }

    def "test - refreshToken: Should set new cookies when refresh succeeds with token map"() {
        given: "A successful refresh response containing tokens"
        String refreshToken = "old-refresh"
        GlobalResponse<Void> body = new GlobalResponse<>()
        body.setMap([token: "new-access", refreshToken: "new-refresh"])
        ResponseEntity<?> serviceResponse = ResponseEntity.ok(body)

        when: "The target method executed"
        ResponseEntity<?> result = authController.refreshToken(request, response)

        then: "The expected calls are made"
        1 * cookieService.getRefreshToken(request) >> Optional.of(refreshToken)
        1 * user.refreshToken(refreshToken) >> serviceResponse
        1 * cookieService.addAccessTokenCookie(response, "new-access")
        1 * cookieService.addRefreshTokenCookie(response, "new-refresh")
        0 * _

        and: "Tokens are removed from response body (set to null)"
        result.statusCode == HttpStatus.OK
        GlobalResponse<Void> responseBody = (GlobalResponse<Void>) result.body
        responseBody.map == null
        noExceptionThrown()
    }

    def "test - refreshToken: Should not set cookies when refresh response has no map"() {
        given: "A successful refresh response without map"
        String refreshToken = "old-refresh"
        GlobalResponse<Void> body = new GlobalResponse<>()
        body.setMap(null)
        ResponseEntity<?> serviceResponse = ResponseEntity.ok(body)

        when: "The target method executed"
        ResponseEntity<?> result = authController.refreshToken(request, response)

        then: "The expected calls are made"
        1 * cookieService.getRefreshToken(request) >> Optional.of(refreshToken)
        1 * user.refreshToken(refreshToken) >> serviceResponse
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.OK
        noExceptionThrown()
    }

    def "test - refreshToken: Should not set cookies when refresh fails"() {
        given: "A failed refresh response"
        String refreshToken = "expired-refresh"
        ResponseEntity<?> serviceResponse = new ResponseEntity<>(HttpStatus.UNAUTHORIZED)

        when: "The target method executed"
        ResponseEntity<?> result = authController.refreshToken(request, response)

        then: "The expected calls are made"
        1 * cookieService.getRefreshToken(request) >> Optional.of(refreshToken)
        1 * user.refreshToken(refreshToken) >> serviceResponse
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.UNAUTHORIZED
        noExceptionThrown()
    }

    def "test - getCurrentUser: Should delegate to user service"() {
        given: "A profile request"
        String username = "Test"
        UserDetails userDetails = Mock(UserDetails)
        ResponseEntity<GlobalResponse<UserProfileDto>> serviceResponse = new ResponseEntity<>(HttpStatus.OK)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<UserProfileDto>> result = authController.getCurrentUser(userDetails)

        then: "The expected calls are made"
        1 * userDetails.getUsername() >> username
        1 * user.getProfile(username) >> serviceResponse
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.OK
        result == serviceResponse
        noExceptionThrown()
    }

    def "test - updatePassword: Should delegate to user service"() {
        given: "A password update request"
        String username = "Test"
        UserDetails userDetails = Mock(UserDetails)
        PasswordUpdateRequestDto requestDto = new PasswordUpdateRequestDto()
        ResponseEntity<GlobalResponse<Void>> serviceResponse = new ResponseEntity<>(HttpStatus.OK)
        String accessTokenFromCookie = "access-token"

        when: "The target method executed"
        ResponseEntity<GlobalResponse<Void>> result = authController.updatePassword(userDetails, request, response, requestDto)

        then: "The expected calls are made"
        1 * cookieService.getAccessToken(request) >> Optional.ofNullable(accessTokenFromCookie)
        1 * userDetails.getUsername() >> username
        1 * user.updatePassword(username, accessTokenFromCookie, requestDto) >> serviceResponse
        1 * cookieService.clearAuthCookies(response) // cookies cleared on successful password update
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.OK
        result == serviceResponse
        noExceptionThrown()
    }

    def "test - updatePassword: Should NOT clear cookies when password update fails"() {
        given: "A password update request that fails"
        String username = "Test"
        UserDetails userDetails = Mock(UserDetails)
        PasswordUpdateRequestDto requestDto = new PasswordUpdateRequestDto()
        ResponseEntity<GlobalResponse<Void>> failedResponse = new ResponseEntity<>(HttpStatus.BAD_REQUEST)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<Void>> result = authController.updatePassword(userDetails, request, response, requestDto)

        then: "The expected calls are made"
        1 * cookieService.getAccessToken(request) >> Optional.of("access-token")
        1 * userDetails.getUsername() >> username
        1 * user.updatePassword(username, "access-token", requestDto) >> failedResponse
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.BAD_REQUEST
        noExceptionThrown()
    }
}