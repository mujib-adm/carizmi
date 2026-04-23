package io.carizmi.domain.identity.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import io.carizmi.domain.identity.service.User
import io.carizmi.domain.identity.data.dto.UserDto
import io.carizmi.domain.identity.data.dto.request.PasswordUpdateRequestDto
import io.carizmi.domain.identity.data.dto.response.UserProfileDto
import io.carizmi.domain.identity.data.dto.response.TokenDto
import io.carizmi.framework.data.response.GlobalResponse
import io.carizmi.framework.exception.AuthenticationException
import io.carizmi.framework.exception.ValidationException
import io.carizmi.testbase.BaseSpecification
import io.carizmi.domain.identity.security.CookieService
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

    def "test - register: Should delegate to user service and wrap result"() {
        given: "A registration request"
        UserDto requestDto = new UserDto(username: "Test")

        when: "The target method executed"
        ResponseEntity<GlobalResponse<Void>> result = authController.register(requestDto)

        then: "The expected calls are made"
        1 * user.register(requestDto)
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.OK
        noExceptionThrown()
    }

    @Unroll
    def "test - logout: Should read tokens from cookies, logout, and clear cookies [access: #accessToken, refresh: #refreshToken]"() {
        when: "The target method executed"
        ResponseEntity<GlobalResponse<Void>> result = authController.logout(request, response)

        then: "The expected calls are made"
        1 * cookieService.getAccessToken(request) >> Optional.ofNullable(accessToken)
        1 * cookieService.getRefreshToken(request) >> Optional.ofNullable(refreshToken)
        1 * user.logout(accessToken, refreshToken)
        1 * cookieService.clearAuthCookies(response)
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.OK
        result.body.globalMessages[0].message == "Logged out successfully"
        noExceptionThrown()

        where:
        accessToken | refreshToken
        "access"    | "refresh"
        null        | "refresh"
        "access"    | null
        null        | null
    }

    def "test - refreshToken: Should return bad request when no refresh token"() {
        when: "The target method executed"
        ResponseEntity<GlobalResponse<Void>> result = authController.refreshToken(request, response)

        then: "The expected calls are made"
        1 * cookieService.getRefreshToken(request) >> Optional.empty()
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.BAD_REQUEST
        noExceptionThrown()
    }

    def "test - refreshToken: Should set new cookies when refresh succeeds"() {
        given: "A successful refresh"
        String refreshToken = "old-refresh"
        TokenDto tokenDto = new TokenDto("new-access", "new-refresh")

        when: "The target method executed"
        ResponseEntity<GlobalResponse<Void>> result = authController.refreshToken(request, response)

        then: "The expected calls are made"
        1 * cookieService.getRefreshToken(request) >> Optional.of(refreshToken)
        1 * user.refreshToken(refreshToken) >> tokenDto
        1 * cookieService.addAccessTokenCookie(response, "new-access")
        1 * cookieService.addRefreshTokenCookie(response, "new-refresh")
        0 * _

        and: "Tokens are NOT in response body (they're in cookies)"
        result.statusCode == HttpStatus.OK
        result.body.responseData == null
        noExceptionThrown()
    }

    def "test - refreshToken: Should propagate AuthenticationException when refresh fails"() {
        given: "An expired refresh token"
        String refreshToken = "expired-refresh"

        when: "The target method executed"
        authController.refreshToken(request, response)

        then: "The expected calls are made"
        1 * cookieService.getRefreshToken(request) >> Optional.of(refreshToken)
        1 * user.refreshToken(refreshToken) >> { throw new AuthenticationException() }
        0 * _

        and: "AuthenticationException is propagated to GlobalExceptionHandler"
        thrown(AuthenticationException)
    }

    def "test - getCurrentUser: Should delegate to user service and wrap result"() {
        given: "A profile request"
        String username = "Test"
        UserDetails userDetails = Mock(UserDetails)
        UserProfileDto profileDto = UserProfileDto.builder().username(username).build()

        when: "The target method executed"
        ResponseEntity<GlobalResponse<UserProfileDto>> result = authController.getCurrentUser(userDetails)

        then: "The expected calls are made"
        1 * userDetails.getUsername() >> username
        1 * user.getProfile(username) >> profileDto
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.OK
        result.body.responseData.username == username
        noExceptionThrown()
    }

    def "test - updatePassword: Should delegate to user service and clear cookies on success"() {
        given: "A password update request"
        String username = "Test"
        UserDetails userDetails = Mock(UserDetails)
        PasswordUpdateRequestDto requestDto = new PasswordUpdateRequestDto()
        String accessTokenFromCookie = "access-token"

        when: "The target method executed"
        ResponseEntity<GlobalResponse<Void>> result = authController.updatePassword(userDetails, request, response, requestDto)

        then: "The expected calls are made"
        1 * cookieService.getAccessToken(request) >> Optional.of(accessTokenFromCookie)
        1 * userDetails.getUsername() >> username
        1 * user.updatePassword(username, accessTokenFromCookie, requestDto)
        1 * cookieService.clearAuthCookies(response)
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.OK
        noExceptionThrown()
    }

    def "test - updatePassword: Should propagate ValidationException when password update fails"() {
        given: "A password update request that fails"
        String username = "Test"
        UserDetails userDetails = Mock(UserDetails)
        PasswordUpdateRequestDto requestDto = new PasswordUpdateRequestDto()

        when: "The target method executed"
        authController.updatePassword(userDetails, request, response, requestDto)

        then: "The expected calls are made"
        1 * cookieService.getAccessToken(request) >> Optional.of("access-token")
        1 * userDetails.getUsername() >> username
        1 * user.updatePassword(username, "access-token", requestDto) >> { throw new ValidationException("Incorrect old password.") }
        0 * _

        and: "ValidationException is propagated (cookies NOT cleared)"
        thrown(ValidationException)
    }
}