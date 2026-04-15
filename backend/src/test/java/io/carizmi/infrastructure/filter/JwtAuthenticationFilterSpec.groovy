package io.carizmi.infrastructure.filter

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import io.carizmi.infrastructure.security.TokenBlacklistService
import io.carizmi.domain.identity.security.JwtService
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class JwtAuthenticationFilterSpec extends Specification {

    TokenBlacklistService blacklistService = Mock()
    BearerTokenResolver bearerTokenResolver = Mock()
    JwtService jwtService = Mock()

    @Subject
    JwtAuthenticationFilter filter = new JwtAuthenticationFilter(blacklistService, bearerTokenResolver, jwtService)

    HttpServletRequest request = Mock()
    HttpServletResponse response = Mock()
    FilterChain chain = Mock()

    void setup() {
        SecurityContextHolder.clearContext()
    }

    def "test - doFilterInternal: Should allow OPTIONS requests to pass through"() {
        given: "An OPTIONS request"
        String method = "OPTIONS"

        when: "The filter is executed"
        filter.doFilterInternal(request, response, chain)

        then: "Only method check and doFilter are called"
        1 * request.getMethod() >> method
        1 * chain.doFilter(request, response)
        0 * _

        and: "Security context remains empty"
        SecurityContextHolder.getContext().getAuthentication() == null
        noExceptionThrown()
    }

    @Unroll
    def "test - doFilterInternal: Should skip excluded path: #path"() {
        given: "A request for an excluded path"
        String method = "GET"
        String servletPath = path

        when: "The filter is executed"
        filter.doFilterInternal(request, response, chain)

        then: "Excluded path logic is triggered"
        1 * request.getMethod() >> method
        1 * request.getServletPath() >> servletPath
        1 * chain.doFilter(request, response)
        0 * _

        and: "Security context remains empty"
        SecurityContextHolder.getContext().getAuthentication() == null
        noExceptionThrown()

        where:
        path << ["/auth/login", "/auth/refresh", "/swagger-ui/index.html", "/v3/api-docs"]
    }

    def "test - doFilterInternal: Should authenticate with valid token"() {
        given: "A request with a valid token"
        String method = "GET"
        String servletPath = "/api/members"
        String token = "valid.jwt.token"

        String username = "testuser"
        List<String> roles = ["ADMIN"]
        Claims claims = Mock(Claims)

        when: "The filter is executed"
        filter.doFilterInternal(request, response, chain)

        then: "Token is resolved and validated"
        1 * request.getMethod() >> method
        1 * request.getServletPath() >> servletPath
        1 * request.getCookies() >> null
        1 * bearerTokenResolver.resolve(request) >> token
        1 * blacklistService.isTokenRevoked(token) >> false
        1 * jwtService.getClaims(token) >> claims
        1 * claims.getSubject() >> username
        1 * claims.get("roles") >> roles
        1 * chain.doFilter(request, response)
        0 * _

        and: "Security context is populated with correct principal"
        SecurityContextHolder.getContext().getAuthentication() != null
        UserDetails principal = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()
        principal.getUsername() == username
        SecurityContextHolder.getContext().getAuthentication().getAuthorities().any { it.authority == "ROLE_ADMIN" }
        noExceptionThrown()
    }

    def "test - doFilterInternal: Should authenticate with valid token from cookie (cookie-first resolution)"() {
        given: "A request with a valid token in cookie"
        String method = "GET"
        String servletPath = "/api/members"
        String token = "cookie.jwt.token"

        String username = "cookieuser"
        List<String> roles = ["MEMBER"]
        Claims claims = Mock(Claims)
        Cookie accessCookie = new Cookie("access_token", token)

        when: "The filter is executed"
        filter.doFilterInternal(request, response, chain)

        then: "Token is read from cookie — bearer resolver is NOT called"
        1 * request.getMethod() >> method
        1 * request.getServletPath() >> servletPath
        2 * request.getCookies() >> ([accessCookie] as Cookie[])
        1 * blacklistService.isTokenRevoked(token) >> false
        1 * jwtService.getClaims(token) >> claims
        1 * claims.getSubject() >> username
        1 * claims.get("roles") >> roles
        1 * chain.doFilter(request, response)
        0 * _

        and: "Security context is populated with correct principal"
        SecurityContextHolder.getContext().getAuthentication() != null
        UserDetails principal = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()
        principal.getUsername() == username
        SecurityContextHolder.getContext().getAuthentication().getAuthorities().any { it.authority == "ROLE_MEMBER" }
        noExceptionThrown()
    }

    def "test - doFilterInternal: Should fallback to bearer resolver when cookie has empty value"() {
        given: "A request with an empty access_token cookie"
        String method = "GET"
        String servletPath = "/api/members"
        String headerToken = "header.jwt.token"
        Cookie emptyCookie = new Cookie("access_token", "")

        String username = "headeruser"
        List<String> roles = ["ADMIN"]
        Claims claims = Mock(Claims)

        when: "The filter is executed"
        filter.doFilterInternal(request, response, chain)

        then: "Cookie is empty, so bearer resolver is called as fallback"
        1 * request.getMethod() >> method
        1 * request.getServletPath() >> servletPath
        2 * request.getCookies() >> ([emptyCookie] as Cookie[])
        1 * bearerTokenResolver.resolve(request) >> headerToken
        1 * blacklistService.isTokenRevoked(headerToken) >> false
        1 * jwtService.getClaims(headerToken) >> claims
        1 * claims.getSubject() >> username
        1 * claims.get("roles") >> roles
        1 * chain.doFilter(request, response)
        0 * _

        and: "Security context is populated from header token"
        SecurityContextHolder.getContext().getAuthentication() != null
        noExceptionThrown()
    }

    def "test - doFilterInternal: Should clear context if token is revoked"() {
        given: "A request with a revoked token"
        String method = "GET"
        String servletPath = "/api/members"
        String token = "revoked.jwt.token"

        when: "The filter is executed"
        filter.doFilterInternal(request, response, chain)

        then: "Blacklist check fails and context is cleared"
        1 * request.getMethod() >> method
        1 * request.getServletPath() >> servletPath
        1 * request.getCookies() >> null
        1 * bearerTokenResolver.resolve(request) >> token
        1 * blacklistService.isTokenRevoked(token) >> true
        1 * chain.doFilter(request, response)
        0 * _

        and: "Security context is empty"
        SecurityContextHolder.getContext().getAuthentication() == null
        noExceptionThrown()
    }

    def "test - doFilterInternal: Should clear context if JWT parsing fails"() {
        given: "A request with an invalid token"
        String method = "GET"
        String servletPath = "/api/members"
        String token = "invalid.jwt.token"

        when: "The filter is executed"
        filter.doFilterInternal(request, response, chain)

        then: "Parsing throws JwtException"
        1 * request.getMethod() >> method
        1 * request.getServletPath() >> servletPath
        1 * request.getCookies() >> null
        1 * bearerTokenResolver.resolve(request) >> token
        1 * blacklistService.isTokenRevoked(token) >> false
        1 * jwtService.getClaims(token) >> { throw new JwtException("Invalid token") }
        1 * chain.doFilter(request, response)
        0 * _

        and: "Security context is empty"
        SecurityContextHolder.getContext().getAuthentication() == null
        noExceptionThrown()
    }

    def "test - doFilterInternal: Should continue without authentication if no token provided"() {
        given: "A request without a token"
        String method = "GET"
        String servletPath = "/api/members"

        when: "The filter is executed"
        filter.doFilterInternal(request, response, chain)

        then: "No token found, just continue"
        1 * request.getMethod() >> method
        1 * request.getServletPath() >> servletPath
        1 * request.getCookies() >> null
        1 * bearerTokenResolver.resolve(request) >> null
        1 * chain.doFilter(request, response)
        0 * _

        and: "Security context remains empty"
        SecurityContextHolder.getContext().getAuthentication() == null
        noExceptionThrown()
    }

    @Unroll
    def "test - getTokenFromCookie: token extraction logic [hasCookies: #hasCookies, hasAccessToken: #hasAccessToken, tokenValue: #tokenValue, expectedResult: #expectedResult]"() {
        given: "A request with certain cookies"
        Cookie[] cookies = null
        if (hasCookies) {
            List<Cookie> cookieList = [new Cookie("other_cookie", "value")]
            if (hasAccessToken) {
                cookieList.add(new Cookie("access_token", tokenValue != null ? tokenValue : ""))
            }
            cookies = cookieList as Cookie[]
        }

        when: "The target method is executed"
        String result = filter.getTokenFromCookie(request)

        then: "The expected calls are made"
        _ * request.getCookies() >> cookies
        0 * _

        and: "The result is correct"
        result == expectedResult

        where:
        hasCookies | hasAccessToken | tokenValue      | expectedResult
        false      | false          | null            | null
        true       | false          | null            | null
        true       | true           | ""              | null
        true       | true           | null            | null
        true       | true           | "my.test.token" | "my.test.token"
    }
}