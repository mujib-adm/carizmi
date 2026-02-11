package org.sofumar.portal.security.filters

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.sofumar.portal.constants.FieldConstants
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import spock.lang.Specification
import spock.lang.Subject

class JsonAuthenticationFilterSpec extends Specification {

    AuthenticationManager authenticationManager = Mock()

    @Subject
    JsonAuthenticationFilter filter = new JsonAuthenticationFilter()

    void setup() {
        filter.setAuthenticationManager(authenticationManager)
    }

    def "test - attemptAuthentication: Should throw exception for non-POST method"() {
        given: "A GET request"
        String method = "GET"
        HttpServletRequest request = Mock(HttpServletRequest)
        HttpServletResponse response = Mock(HttpServletResponse)

        when: "Attempting authentication"
        filter.attemptAuthentication(request, response)

        then: "Method check is performed"
        2 * request.getMethod() >> method
        0 * _

        and: "Exception is thrown"
        AuthenticationServiceException e = thrown()
        e.message.contains("Authentication method not supported")
    }

    def "test - attemptAuthentication: Should authenticate successfully with valid JSON"() {
        given: "A POST request with valid JSON credentials"
        String username = "testuser"
        String password = "password123"
        String json = "{\"username\":\"${username}\", \"password\":\"${password}\"}"

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login")
        request.setContent(json.getBytes())
        HttpServletResponse response = Mock(HttpServletResponse)

        Authentication authenticationResult = Mock(Authentication)
        UsernamePasswordAuthenticationToken capturedAuthToken = null

        when: "Attempting authentication"
        Authentication result = filter.attemptAuthentication(request, response)

        then: "AuthenticationManager is called with correct credentials"
        1 * authenticationManager.authenticate(_ as UsernamePasswordAuthenticationToken) >> { UsernamePasswordAuthenticationToken token ->
            capturedAuthToken = token
            return authenticationResult
        }
        0 * _

        and: "The result matches and credentials are correct"
        result == authenticationResult
        capturedAuthToken != null
        capturedAuthToken.principal == username
        capturedAuthToken.credentials == password
        request.getAttribute(FieldConstants.USERNAME) == username
        noExceptionThrown()
    }

    def "test - attemptAuthentication: Should handle missing credentials by defaulting to empty strings"() {
        given: "A POST request with empty JSON object"
        String json = "{}"
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login")
        request.setContent(json.getBytes())
        HttpServletResponse response = Mock(HttpServletResponse)

        Authentication authenticationResult = Mock(Authentication)
        UsernamePasswordAuthenticationToken capturedAuthToken = null

        when: "Attempting authentication"
        filter.attemptAuthentication(request, response)

        then: "AuthenticationManager is called with empty credentials"
        1 * authenticationManager.authenticate(_ as UsernamePasswordAuthenticationToken) >> { UsernamePasswordAuthenticationToken token ->
            capturedAuthToken = token
            return authenticationResult
        }
        0 * _

        and: "The credentials are empty strings"
        capturedAuthToken.principal == ""
        capturedAuthToken.credentials == ""
        request.getAttribute(FieldConstants.USERNAME) == ""
        noExceptionThrown()
    }

    def "test - attemptAuthentication: Should throw exception for malformed JSON"() {
        given: "A POST request with malformed JSON"
        String json = "invalid-json"
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login")
        request.setContent(json.getBytes())
        HttpServletResponse response = Mock(HttpServletResponse)

        when: "Attempting authentication"
        filter.attemptAuthentication(request, response)

        then: "No authentication calls are made"
        0 * _

        and: "AuthenticationServiceException is thrown"
        AuthenticationServiceException e = thrown()
        e.message.contains("Failed to parse authentication request body")
    }
}