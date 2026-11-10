package io.carizmi.domain.identity.security

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.test.util.ReflectionTestUtils
import spock.lang.Specification
import spock.lang.Subject

import java.time.Duration

class RefreshTokenServiceSpec extends Specification {

    StringRedisTemplate redisTemplate = Mock()
    ValueOperations<String, String> valueOperations = Mock()

    @Subject
    RefreshTokenService service

    def setup() {
        redisTemplate.opsForValue() >> valueOperations
        service = new RefreshTokenService(redisTemplate)
        ReflectionTestUtils.setField(service, "refreshTokenExpirationMinutes", 15L)
    }

    def "createRefreshToken should generate a UUID token and store username in Redis with configured TTL"() {
        given:
        String capturedKey = null

        when:
        String token = service.createRefreshToken("admin")

        then:
        1 * redisTemplate.opsForValue() >> valueOperations
        1 * valueOperations.set(_ as String, "admin", Duration.ofMinutes(15)) >> { String key, String val, Duration ttl ->
            capturedKey = key
        }
        0 * _

        and:
        token != null
        UUID.fromString(token) != null
        capturedKey == "refresh_token:" + token
    }

    def "validateRefreshToken should return Optional with username when token exists in Redis"() {
        when:
        Optional<String> result = service.validateRefreshToken("valid-token")

        then:
        1 * redisTemplate.opsForValue() >> valueOperations
        1 * valueOperations.get("refresh_token:valid-token") >> "admin"
        0 * _

        and:
        result.isPresent()
        result.get() == "admin"
    }

    def "validateRefreshToken should return empty Optional when token does not exist in Redis"() {
        when:
        Optional<String> result = service.validateRefreshToken("expired-token")

        then:
        1 * redisTemplate.opsForValue() >> valueOperations
        1 * valueOperations.get("refresh_token:expired-token") >> null
        0 * _

        and:
        result.isEmpty()
    }

    def "rotateRefreshToken should set 20s grace period on old token and issue a new refresh token when valid"() {
        given:
        String capturedNewKey = null

        when:
        String newToken = service.rotateRefreshToken("old-token")

        then:
        2 * redisTemplate.opsForValue() >> valueOperations
        1 * valueOperations.get("refresh_token:old-token") >> "manager_user"
        1 * redisTemplate.expire("refresh_token:old-token", Duration.ofSeconds(20))
        1 * valueOperations.set(_ as String, "manager_user", Duration.ofMinutes(15)) >> { String key, String val, Duration ttl ->
            capturedNewKey = key
        }
        0 * _

        and:
        newToken != null
        newToken != "old-token"
        capturedNewKey == "refresh_token:" + newToken
    }

    def "rotateRefreshToken should throw IllegalArgumentException when old token is invalid or expired"() {
        when:
        service.rotateRefreshToken("invalid-token")

        then:
        1 * redisTemplate.opsForValue() >> valueOperations
        1 * valueOperations.get("refresh_token:invalid-token") >> null
        0 * _

        and:
        IllegalArgumentException ex = thrown()
        ex.message == "Invalid state."
    }

    def "deleteRefreshToken should remove the refresh token key from Redis"() {
        when:
        service.deleteRefreshToken("token-to-delete")

        then:
        1 * redisTemplate.delete("refresh_token:token-to-delete")
        0 * _
    }
}