package io.carizmi.domain.identity.security

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import io.carizmi.testbase.BaseSpecification
import org.springframework.test.util.ReflectionTestUtils
import spock.lang.Subject
import spock.lang.Unroll

class CookieServiceSpec extends BaseSpecification {

    @Subject
    CookieService cookieService = new CookieService()

    HttpServletResponse response = Mock()

    def setup() {
        ReflectionTestUtils.setField(cookieService, "secure", true)
        ReflectionTestUtils.setField(cookieService, "accessTokenExpirationMinutes", 5)
        ReflectionTestUtils.setField(cookieService, "refreshTokenExpirationMinutes", 15L)
        ReflectionTestUtils.setField(cookieService, "contextPath", "/api")
    }

    def "test - addAccessTokenCookie: Should add an httpOnly access token cookie"() {
        given: "An access token"
        String token = "access-token-value"

        when: "The target method executed"
        cookieService.addAccessTokenCookie(response, token)

        then: "A cookie is added with correct properties"
        1 * response.addCookie({ Cookie c ->
            c.name == "access_token" &&
            c.value == token &&
            c.httpOnly &&
            c.secure &&
            c.path == "/api" &&
            c.maxAge == 300 // 5 minutes * 60
        })
        0 * _

        and:
        noExceptionThrown()
    }

    def "test - addRefreshTokenCookie: Should add an httpOnly refresh token cookie"() {
        given: "A refresh token"
        String token = "refresh-token-value"

        when: "The target method executed"
        cookieService.addRefreshTokenCookie(response, token)

        then: "A cookie is added with correct properties"
        1 * response.addCookie({ Cookie c ->
            c.name == "refresh_token" &&
            c.value == token &&
            c.httpOnly &&
            c.secure &&
            c.path == "/api" &&
            c.maxAge == 900 // 15 minutes * 60
        })
        0 * _

        and:
        noExceptionThrown()
    }

    def "test - clearAuthCookies: Should add expired access and refresh cookies"() {
        when: "The target method executed"
        cookieService.clearAuthCookies(response)

        then: "Both cookies are cleared with maxAge 0"
        1 * response.addCookie({ Cookie c ->
            c.name == "access_token" &&
            c.value == "" &&
            c.maxAge == 0 &&
            c.httpOnly &&
            c.path == "/api"
        })
        1 * response.addCookie({ Cookie c ->
            c.name == "refresh_token" &&
            c.value == "" &&
            c.maxAge == 0 &&
            c.httpOnly &&
            c.path == "/api"
        })
        0 * _

        and:
        noExceptionThrown()
    }

    def "test - getAccessToken: Should return token value from access_token cookie"() {
        given: "A request with an access_token cookie"
        Cookie accessCookie = new Cookie("access_token", "my-access-token")
        Cookie otherCookie = new Cookie("other", "value")
        HttpServletRequest request = Stub(HttpServletRequest)
        request.getCookies() >> ([otherCookie, accessCookie] as Cookie[])

        when: "The target method executed"
        Optional<String> result = cookieService.getAccessToken(request)

        then:
        0 * _

        and: "The expected result"
        result.isPresent()
        result.get() == "my-access-token"
    }

    def "test - getRefreshToken: Should return token value from refresh_token cookie"() {
        given: "A request with a refresh_token cookie"
        Cookie refreshCookie = new Cookie("refresh_token", "my-refresh-token")
        HttpServletRequest request = Stub(HttpServletRequest)
        request.getCookies() >> ([refreshCookie] as Cookie[])

        when: "The target method executed"
        Optional<String> result = cookieService.getRefreshToken(request)

        then:
        0 * _

        and: "The expected result"
        result.isPresent()
        result.get() == "my-refresh-token"
    }

    def "test - getAccessToken: Should return empty when no cookies present"() {
        given: "A request with no cookies"
        HttpServletRequest request = Stub(HttpServletRequest)
        request.getCookies() >> null

        when: "The target method executed"
        Optional<String> result = cookieService.getAccessToken(request)

        then:
        0 * _

        and: "The expected result"
        result.isEmpty()
    }

    def "test - getRefreshToken: Should return empty when no cookies present"() {
        given: "A request with no cookies"
        HttpServletRequest request = Stub(HttpServletRequest)
        request.getCookies() >> null

        when: "The target method executed"
        Optional<String> result = cookieService.getRefreshToken(request)

        then:
        0 * _

        and: "The expected result"
        result.isEmpty()
    }

    def "test - getAccessToken: Should return empty when access_token cookie is not found"() {
        given: "Cookies without access_token"
        Cookie otherCookie = new Cookie("other", "value")
        HttpServletRequest request = Stub(HttpServletRequest)
        request.getCookies() >> ([otherCookie] as Cookie[])

        when: "The target method executed"
        Optional<String> result = cookieService.getAccessToken(request)

        then:
        0 * _

        and: "The expected result"
        result.isEmpty()
    }

    @Unroll
    def "test - getAccessToken: Should return empty for blank or null cookie value [value: #value]"() {
        given: "An access_token cookie with empty/null value"
        Cookie accessCookie = new Cookie("access_token", value)
        HttpServletRequest request = Stub(HttpServletRequest)
        request.getCookies() >> ([accessCookie] as Cookie[])

        when: "The target method executed"
        Optional<String> result = cookieService.getAccessToken(request)

        then:
        0 * _

        and: "The expected result"
        result.isEmpty()

        where:
        value << ["", null]
    }

    def "test - addAccessTokenCookie: Should set secure=false when configured"() {
        given: "Secure flag is disabled"
        ReflectionTestUtils.setField(cookieService, "secure", false)
        String token = "token-value"

        when: "The target method executed"
        cookieService.addAccessTokenCookie(response, token)

        then: "Cookie is not secure"
        1 * response.addCookie({ Cookie c ->
            c.name == "access_token" &&
            !c.secure
        })
        0 * _

        and:
        noExceptionThrown()
    }
}