package org.sofumar.portal.framework.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.refreshTokenExpirationMinutes:15}")
    private long refreshTokenExpirationMinutes;

    private static final String KEY_PREFIX = "refresh_token:";

    public RefreshTokenService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String createRefreshToken(String username) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(KEY_PREFIX + token, username, Duration.ofMinutes(refreshTokenExpirationMinutes));
        return token;
    }

    public String rotateRefreshToken(String oldToken) {
        Optional<String> usernameOpt = validateRefreshToken(oldToken);
        if (usernameOpt.isPresent()) {
            // Grace period for concurrency (20s)
            redisTemplate.expire(KEY_PREFIX + oldToken, Duration.ofSeconds(20));
            return createRefreshToken(usernameOpt.get());
        }
        throw new IllegalArgumentException("Invalid refresh token");
    }

    public Optional<String> validateRefreshToken(String token) {
        String username = redisTemplate.opsForValue().get(KEY_PREFIX + token);
        return Optional.ofNullable(username);
    }

    public void deleteRefreshToken(String token) {
        redisTemplate.delete(KEY_PREFIX + token);
    }
}