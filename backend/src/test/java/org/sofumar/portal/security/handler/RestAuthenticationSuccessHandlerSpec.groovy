package org.sofumar.portal.security.handler

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.sofumar.portal.constants.Role
import org.sofumar.portal.core.businesslogic.User
import org.sofumar.portal.core.vo.UserVO
import org.sofumar.portal.framework.service.RefreshTokenService
import org.sofumar.portal.security.JwtService
import org.sofumar.portal.security.SofumarUserDetails
import org.sofumar.portal.security.CookieService
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import spock.lang.Specification

class RestAuthenticationSuccessHandlerSpec extends Specification {

    RefreshTokenService refreshTokenService = Mock()
    ObjectMapper objectMapper = new ObjectMapper()
    User user = Mock()
    JwtService jwtService = Mock()
    CookieService cookieService = Mock()
    RestAuthenticationSuccessHandler handler = new RestAuthenticationSuccessHandler(refreshTokenService, objectMapper, user, jwtService, cookieService)

    HttpServletRequest request = Mock()
    HttpServletResponse response = Mock()
    Authentication authentication = Mock()
    StringWriter stringWriter = new StringWriter()
    PrintWriter writer = new PrintWriter(stringWriter)

    def "should handle successful authentication"() {
        given:
        String username = "Test"
        String firstName = "Test"
        UserVO userVO = new UserVO(username: username, role: Role.MEMBER, firstName: firstName)
        SofumarUserDetails userDetails = new SofumarUserDetails(userVO)

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
        1 * response.getWriter() >> writer
        0 * _

        and:
        String jsonResponse = stringWriter.toString()
        jsonResponse.contains("\"statusCode\":200")
        jsonResponse.contains("\"statusDesc\":\"OK\"")
        // Non-sensitive metadata in response
        !jsonResponse.contains("\"token\"")
        !jsonResponse.contains("\"refreshToken\"")
        jsonResponse.contains("\"firstName\":\"Test\"")
        jsonResponse.contains("\"role\":\"MEMBER\"")
    }
}