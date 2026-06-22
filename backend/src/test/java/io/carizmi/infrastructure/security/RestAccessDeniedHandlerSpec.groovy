package io.carizmi.infrastructure.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import io.carizmi.framework.data.response.GlobalMsg
import io.carizmi.framework.data.response.GlobalResponse
import io.carizmi.testbase.BaseSpecification
import io.carizmi.testbase.ServletCaptureHelper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.ObjectMapper

import static io.carizmi.framework.message.constant.CommonMessages.ACCESS_DENIED

class RestAccessDeniedHandlerSpec extends BaseSpecification {

    ObjectMapper objectMapper = JsonMapper.builder().build()
    RestAccessDeniedHandler handler = new RestAccessDeniedHandler(objectMapper)

    HttpServletRequest request = Mock(HttpServletRequest)
    HttpServletResponse response = Mock(HttpServletResponse)

    ServletCaptureHelper capture

    def setup() {
        capture = captureServletOutput()
        response.getOutputStream() >> capture.getServletOutputStream()
    }

    def "test - handle should set 403 status, JSON content type and write response body with exception message"() {

        given: "Valid exception and response setup"
        String exceptionMessage = "Access is denied"
        AccessDeniedException exception = new AccessDeniedException(exceptionMessage)

        when: "The target method executed"
        handler.handle(request, response, exception)

        then: "The expected calls are made"
        1 * response.setStatus(HttpServletResponse.SC_FORBIDDEN)
        1 * response.setContentType(MediaType.APPLICATION_JSON_VALUE)
        1 * response.getOutputStream() >> capture.getServletOutputStream()
        0 * _

        and: "The expected result - parse JSON and assert"
        String jsonResponse = capture.getByteArrayOutputStream().toString('UTF-8')
        GlobalResponse globalResponse = objectMapper.readValue(jsonResponse, GlobalResponse)
        globalResponse.getStatusCode() == HttpServletResponse.SC_FORBIDDEN
        globalResponse.getGlobalMessages()
        globalResponse.getGlobalMessages().stream().anyMatch({ GlobalMsg gm -> gm.getMessage() == ACCESS_DENIED.getMessageText() })
        noExceptionThrown()
    }
}