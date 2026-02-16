package org.sofumar.portal.security.handler

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.ServletOutputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.sofumar.portal.framework.data.response.GlobalMsg
import org.sofumar.portal.framework.data.response.GlobalResponse
import spock.lang.Specification

class RestAccessDeniedHandlerSpec extends Specification {

    ObjectMapper objectMapper = Mock(ObjectMapper)
    RestAccessDeniedHandler handler = new RestAccessDeniedHandler(objectMapper)

    HttpServletRequest request = Mock(HttpServletRequest)
    HttpServletResponse response = Mock(HttpServletResponse)
    ServletOutputStream outputStream = Mock(ServletOutputStream)

    def "test - handle should set 403 status, JSON content type and write response body with exception message"() {

        given: "Valid exception and response setup"
        String exceptionMessage = "Access is denied"
        AccessDeniedException exception = new AccessDeniedException(exceptionMessage)
        Object capturedBody = null

        when: "The target method executed"
        handler.handle(request, response, exception)

        then: "The expected calls are made"
        1 * response.setStatus(HttpServletResponse.SC_FORBIDDEN)
        1 * response.setContentType(MediaType.APPLICATION_JSON_VALUE)
        1 * response.getOutputStream() >> outputStream
        1 * objectMapper.writeValue(outputStream, _ as Object) >> { ServletOutputStream os, Object body ->
            capturedBody = body
        }
        0 * _

        and: "The expected result"
        capturedBody != null
        noExceptionThrown()
    }

    def "test - handle should propagate exception message correctly to response body"() {

        given: "Custom exception message"
        String exceptionMessage = "Forbidden resource"
        AccessDeniedException exception = new AccessDeniedException(exceptionMessage)
        Object capturedBody = null

        when: "The target method executed"
        handler.handle(request, response, exception)

        then: "The expected calls are made"
        1 * response.setStatus(HttpServletResponse.SC_FORBIDDEN)
        1 * response.setContentType(MediaType.APPLICATION_JSON_VALUE)
        1 * response.getOutputStream() >> outputStream
        1 * objectMapper.writeValue(outputStream, _ as Object) >> { ServletOutputStream os, Object body ->
            capturedBody = body
        }
        0 * _

        and: "The expected result"
        capturedBody != null
        capturedBody != null
        GlobalResponse responseBody = (GlobalResponse) capturedBody
        List<GlobalMsg> messages = responseBody.getGlobalMessages()
        messages != null
        messages.size() > 0
        messages[0].getMessage() == exceptionMessage
        noExceptionThrown()
    }

    def "test - handle should support null exception message safely"() {

        given: "Exception with null message"
        String exceptionMessage = null
        AccessDeniedException exception = new AccessDeniedException(exceptionMessage)
        Object capturedBody = null

        when: "The target method executed"
        handler.handle(request, response, exception)

        then: "The expected calls are made"
        1 * response.setStatus(HttpServletResponse.SC_FORBIDDEN)
        1 * response.setContentType(MediaType.APPLICATION_JSON_VALUE)
        1 * response.getOutputStream() >> outputStream
        1 * objectMapper.writeValue(outputStream, _ as Object) >> { ServletOutputStream os, Object body ->
            capturedBody = body
        }
        0 * _

        and: "The expected result"
        capturedBody != null
        noExceptionThrown()
    }

    def "test - handle should always set status to FORBIDDEN and content type to JSON"() {

        given: "Standard access denied exception"
        String exceptionMessage = "Denied"
        AccessDeniedException exception = new AccessDeniedException(exceptionMessage)
        Object capturedBody = null

        when: "The target method executed"
        handler.handle(request, response, exception)

        then: "The expected calls are made"
        1 * response.setStatus(HttpServletResponse.SC_FORBIDDEN)
        1 * response.setContentType(MediaType.APPLICATION_JSON_VALUE)
        1 * response.getOutputStream() >> outputStream
        1 * objectMapper.writeValue(outputStream, _ as Object) >> { ServletOutputStream os, Object body ->
            capturedBody = body
        }
        0 * _

        and: "The expected result"
        capturedBody != null
        response != null
        exception != null
        noExceptionThrown()
    }
}