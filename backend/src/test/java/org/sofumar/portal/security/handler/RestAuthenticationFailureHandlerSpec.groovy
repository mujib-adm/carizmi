package org.sofumar.portal.security.handler

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.sofumar.portal.core.businesslogic.User
import org.sofumar.portal.constants.FieldConstants
import org.sofumar.portal.framework.data.response.GlobalMsg
import org.sofumar.portal.framework.data.response.GlobalResponse
import org.sofumar.portal.testbase.BaseSpecification
import org.sofumar.portal.testbase.ServletCaptureHelper
import org.springframework.http.MediaType
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.DisabledException
import org.springframework.security.authentication.LockedException
import org.springframework.security.core.AuthenticationException

import static org.sofumar.portal.framework.message.constant.CommonMessages.INVALID_CREDENTIALS
import static org.sofumar.portal.framework.message.constant.CommonMessages.ACCOUNT_TEMP_LOCKED
import static org.sofumar.portal.framework.message.constant.CommonMessages.ACCOUNT_DISABLED

class RestAuthenticationFailureHandlerSpec extends BaseSpecification {

    ObjectMapper objectMapper = new ObjectMapper()
    User user = Mock()
    RestAuthenticationFailureHandler handler = new RestAuthenticationFailureHandler(objectMapper, user)

    HttpServletRequest request = Mock()
    HttpServletResponse response = Mock()

    ServletCaptureHelper capture

    def setup() {
        // Install the reusable servlet output capture and set the response to return it
        capture = captureServletOutput()
        response.getOutputStream() >> capture.getServletOutputStream()
    }

    def "should handle BadCredentialsException and call onLoginFailure"() {
        given:
        String username = "Test"
        AuthenticationException exception = new BadCredentialsException("Bad credentials")

        when:
        handler.onAuthenticationFailure(request, response, exception)

        then:
        1 * request.getAttribute(FieldConstants.USERNAME) >> username
        1 * user.onLoginFailure(username)
        1 * response.setStatus(HttpServletResponse.SC_UNAUTHORIZED)
        1 * response.setContentType(MediaType.APPLICATION_JSON_VALUE)
        1 * response.getOutputStream() >> capture.getServletOutputStream()
        0 * _

        and: "The response body is valid JSON and contains expected fields"
        String jsonResponse = capture.getByteArrayOutputStream().toString('UTF-8')
        GlobalResponse globalResponse = objectMapper.readValue(jsonResponse, GlobalResponse)
        globalResponse.getStatusCode() == HttpServletResponse.SC_UNAUTHORIZED
        globalResponse.getGlobalMessages()
        globalResponse.getGlobalMessages().stream().anyMatch({ GlobalMsg gm -> gm.getMessage() == INVALID_CREDENTIALS.getMessageText() })
        // Non-sensitive metadata not present
        !jsonResponse.contains('"token"')
        !jsonResponse.contains('"refreshToken"')
        noExceptionThrown()
    }

    def "test LockedException"() {
        given:
        String username = "Test"
        AuthenticationException exception = new LockedException("User locked")

        when:
        handler.onAuthenticationFailure(request, response, exception)

        then:
        1 * request.getAttribute(FieldConstants.USERNAME) >> username
        1 * response.setStatus(HttpServletResponse.SC_UNAUTHORIZED)
        1 * response.setContentType(MediaType.APPLICATION_JSON_VALUE)
        response.getOutputStream() >> capture.getServletOutputStream()
        0 * _

        and:
        String jsonResponse = capture.getByteArrayOutputStream().toString('UTF-8')
        GlobalResponse globalResponse = objectMapper.readValue(jsonResponse, GlobalResponse)
        globalResponse.getStatusCode() == HttpServletResponse.SC_UNAUTHORIZED
        globalResponse.getGlobalMessages()
        globalResponse.getGlobalMessages().stream().anyMatch({ GlobalMsg gm -> gm.getMessage() == ACCOUNT_TEMP_LOCKED.getMessageText() })
        // Non-sensitive metadata not present
        !jsonResponse.contains('"token"')
        !jsonResponse.contains('"refreshToken"')
        noExceptionThrown()
    }

    def "should handle DisabledException"() {
        given:
        String username = "Test"
        AuthenticationException exception = new DisabledException("User disabled")

        when:
        handler.onAuthenticationFailure(request, response, exception)

        then:
        1 * request.getAttribute(FieldConstants.USERNAME) >> username
        1 * response.setStatus(HttpServletResponse.SC_UNAUTHORIZED)
        1 * response.setContentType(MediaType.APPLICATION_JSON_VALUE)
        response.getOutputStream() >> capture.getServletOutputStream()
        0 * _

        and:
        String jsonResponse = capture.getByteArrayOutputStream().toString('UTF-8')
        GlobalResponse globalResponse = objectMapper.readValue(jsonResponse, GlobalResponse)
        globalResponse.getStatusCode() == HttpServletResponse.SC_UNAUTHORIZED
        globalResponse.getGlobalMessages()
        globalResponse.getGlobalMessages().stream().anyMatch({ GlobalMsg gm -> gm.getMessage() == ACCOUNT_DISABLED.getMessageText() })
        // Non-sensitive metadata not present
        !jsonResponse.contains('"token"')
        !jsonResponse.contains('"refreshToken"')
        noExceptionThrown()
    }
}