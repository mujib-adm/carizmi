package io.carizmi.domain.identity.security.handler

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import io.carizmi.shared.constants.Role
import io.carizmi.domain.identity.service.User
import io.carizmi.domain.identity.model.UserVO
import io.carizmi.domain.identity.security.RefreshTokenService
import io.carizmi.domain.identity.security.JwtService
import io.carizmi.domain.identity.security.CarizmiUserDetails
import io.carizmi.domain.identity.security.CookieService
import io.carizmi.testbase.BaseSpecification
import io.carizmi.testbase.ServletCaptureHelper
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication

class RestAuthenticationSuccessHandlerSpec extends BaseSpecification {

    RefreshTokenService refreshTokenService = Mock()
    ObjectMapper objectMapper = new ObjectMapper()
    User user = Mock()
    JwtService jwtService = Mock()
    CookieService cookieService = Mock()
    RestAuthenticationSuccessHandler handler = new RestAuthenticationSuccessHandler(refreshTokenService, objectMapper, user, jwtService, cookieService)

    HttpServletRequest request = Mock()
    HttpServletResponse response = Mock()
    Authentication authentication = Mock()

    ServletCaptureHelper capture

    def setup() {
        capture = captureServletOutput()
        response.getOutputStream() >> capture.getServletOutputStream()
    }

    def "should handle successful authentication"() {
        given:
        String username = "Test"
        String firstName = "Test"
        UserVO userVO = new UserVO(username: username, role: Role.MEMBER, firstName: firstName)
        CarizmiUserDetails userDetails = new CarizmiUserDetails(userVO, 15L)

        when:
        handler.onAuthenticationSuccess(request, response, authentication)

        then:
        1 * authentication.getPrincipal() >> userDetails
        1 * jwtService.generateAccessToken(userDetails) >> "generated-access-token"
        1 * user.onLoginSuccess(username)
        1 * refreshTokenService.createRefreshToken(username) >> "refreshToken"
        1 * cookieService.addAccessTokenCookie(response, "generated-access-token")
        1 * cookieService.addRefreshTokenCookie(response, "refreshToken")
        1 * response.setStatus(HttpServletResponse.SC_OK)
        1 * response.setContentType(MediaType.APPLICATION_JSON_VALUE)
        1 * response.getOutputStream() >> capture.getServletOutputStream()
        0 * _

        and: "Response JSON contains user info and not sensitive tokens"
        String jsonResponse = capture.getByteArrayOutputStream().toString('UTF-8')
        jsonResponse.contains('"firstName"')
        jsonResponse.contains('"role"')
        !jsonResponse.contains('"token"')
        !jsonResponse.contains('"refreshToken"')

        and: "No exception occurred"
        noExceptionThrown()
    }
}