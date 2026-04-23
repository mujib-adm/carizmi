package io.carizmi.infrastructure.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;

    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void revokeToken(String token, long expirationSeconds) {
        redisTemplate.opsForValue().set(token, "revoked", Duration.ofSeconds(expirationSeconds));
    }

    public boolean isTokenRevoked(String token) {
        return redisTemplate.hasKey(token);
    }
}