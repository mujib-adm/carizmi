package org.sofumar.portal.security.handler

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.sofumar.portal.core.businesslogic.User
import org.sofumar.portal.constants.FieldConstants
import org.springframework.http.MediaType
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.DisabledException
import org.springframework.security.authentication.LockedException
import org.springframework.security.core.AuthenticationException
import spock.lang.Specification

class RestAuthenticationFailureHandlerSpec extends Specification {

    ObjectMapper objectMapper = new ObjectMapper()
    User user = Mock()
    RestAuthenticationFailureHandler handler = new RestAuthenticationFailureHandler(objectMapper, user)

    HttpServletRequest request = Mock()
    HttpServletResponse response = Mock()
    StringWriter stringWriter = new StringWriter()
    PrintWriter writer = new PrintWriter(stringWriter)

    def "should handle BadCredentialsException and call onLoginFailure"() {
        given:
        String username = "Test"
        AuthenticationException exception = new BadCredentialsException("Bad credentials")

        when:
        handler.onAuthenticationFailure(request, response, exception)

        then:
        1 * user.onLoginFailure(username)
        1 * response.setStatus(HttpServletResponse.SC_UNAUTHORIZED)
        1 * response.setContentType(MediaType.APPLICATION_JSON_VALUE)
        1 * request.getAttribute(FieldConstants.USERNAME) >> username
        1 * response.getWriter() >> writer
        0 * _

        and:
        String jsonResponse = stringWriter.toString()
        jsonResponse.contains("\"statusCode\":401")
        jsonResponse.contains("\"statusDesc\":\"Unauthorized\"")
        jsonResponse.contains("Invalid username or password.")
    }

    def "should handle LockedException"() {
        given:
        String username = "Test"
        AuthenticationException exception = new LockedException("User locked")

        when:
        handler.onAuthenticationFailure(request, response, exception)

        then:
        0 * user.onLoginFailure(_)
        1 * response.setStatus(HttpServletResponse.SC_UNAUTHORIZED)
        1 * response.setContentType(MediaType.APPLICATION_JSON_VALUE)
        1 * request.getAttribute(FieldConstants.USERNAME) >> username
        1 * response.getWriter() >> writer
        0 * _

        and:
        String jsonResponse = stringWriter.toString()
        jsonResponse.contains("\"statusCode\":401")
        jsonResponse.contains("\"statusDesc\":\"Unauthorized\"")
        jsonResponse.contains("Account temporarily locked")
    }

    def "should handle DisabledException"() {
        given:
        String username = "Test"
        AuthenticationException exception = new DisabledException("User disabled")

        when:
        handler.onAuthenticationFailure(request, response, exception)

        then:
        0 * user.onLoginFailure(_)
        1 * response.setStatus(HttpServletResponse.SC_UNAUTHORIZED)
        1 * response.setContentType(MediaType.APPLICATION_JSON_VALUE)
        1 * request.getAttribute(FieldConstants.USERNAME) >> username
        1 * response.getWriter() >> writer
        0 * _

        and:
        String jsonResponse = stringWriter.toString()
        jsonResponse.contains("\"statusCode\":401")
        jsonResponse.contains("\"statusDesc\":\"Unauthorized\"")
        jsonResponse.contains("Your account is inactive")
    }

    def "should handle DisabledException when username is null"() {
        given:
        AuthenticationException exception = new DisabledException("User disabled")

        when:
        handler.onAuthenticationFailure(request, response, exception)

        then:
        0 * user.onLoginFailure(_)
        1 * response.setStatus(HttpServletResponse.SC_UNAUTHORIZED)
        1 * response.setContentType(MediaType.APPLICATION_JSON_VALUE)
        1 * request.getAttribute(FieldConstants.USERNAME) >> null
        1 * response.getWriter() >> writer
        0 * _

        and:
        String jsonResponse = stringWriter.toString()
        jsonResponse.contains("\"statusCode\":401")
        jsonResponse.contains("\"statusDesc\":\"Unauthorized\"")
        jsonResponse.contains("Your account is inactive")
    }
}