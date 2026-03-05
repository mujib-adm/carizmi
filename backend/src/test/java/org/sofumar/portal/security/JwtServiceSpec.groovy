package org.sofumar.portal.security

import io.jsonwebtoken.Claims
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.test.util.ReflectionTestUtils
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class JwtServiceSpec extends Specification {

    @Subject
    JwtService jwtService = new JwtService()

    def setup() {
        // HS512 requires at least 64 bytes (512 bits)
        def secretString = "super-secret-key-that-is-long-enough-for-hs512-algorithm-64-chars!"
        ReflectionTestUtils.setField(jwtService, "secret", Base64.getEncoder().encodeToString(secretString.getBytes()))
        ReflectionTestUtils.setField(jwtService, "expMin", 60)
        ReflectionTestUtils.setField(jwtService, "issuer", "test-issuer")
    }

    def "test - generateAccessToken: Should generate a valid JWT with correct claims"() {
        given:
        String username = "testuser"
        UserDetails userDetails = new User(username, "password", [new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER")])

        when:
        String token = jwtService.generateAccessToken(userDetails)

        then:
        0 * _

        and:
        token != null
        Claims claims = jwtService.getClaims(token)
        claims.subject == username
        claims.issuer == "test-issuer"
        claims.get("roles") == ["ADMIN", "USER"]
        claims.expiration.after(new Date())
    }

    def "test - getClaims: Should parse claims from a valid JWT"() {
        given:
        UserDetails userDetails = new User("user", "pass", [])
        String token = jwtService.generateAccessToken(userDetails)

        when:
        Claims claims = jwtService.getClaims(token)

        then:
        0 * _

        and:
        claims != null
        claims.subject == "user"
    }

    def "test - getRemainingExpirationSeconds: Should return positive seconds for a fresh token"() {
        given:
        UserDetails userDetails = new User("user", "pass", [])
        String token = jwtService.generateAccessToken(userDetails)

        when:
        long remaining = jwtService.getRemainingExpirationSeconds(token)

        then:
        0 * _

        and:
        remaining > 0
        remaining <= 3600
    }

    @Unroll
    def "test - getRemainingExpirationSeconds: Should return 0 for an invalid or expired token [#invalidToken]"() {
        given:
        String token = invalidToken

        when:
        long result = jwtService.getRemainingExpirationSeconds(token)

        then:
        0 * _

        and:
        result == 0

        where:
        invalidToken << [null, "", "invalid.token.string", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e30.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"]
    }
}