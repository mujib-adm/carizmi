package io.carizmi.infrastructure.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.carizmi.framework.message.MessageType;
import io.carizmi.framework.util.ResponseUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.time.Duration;

import static io.carizmi.framework.message.constant.CommonMessages.TOO_MANY_REQUESTS;

/**
 * Rate limiting filter using Bucket4j.
 * Limits requests based on IP address.
 * Uses Caffeine cache to bound memory and auto-evict stale entries.
 *
 * <p><b>Note:</b> Rate-limit state is held in-memory and lost on restart.
 * This is acceptable for single-instance deployments (e.g., Oracle Cloud "Always Free").
 * For multi-instance deployments, consider Redis-backed rate limiting.</p>
 */
@Component
public class RequestsRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_CACHE_SIZE = 10_000;
    private static final Duration CACHE_EXPIRY = Duration.ofMinutes(10);

    private final Cache<String, Bucket> cache = Caffeine.newBuilder()
            .maximumSize(MAX_CACHE_SIZE)
            .expireAfterAccess(CACHE_EXPIRY)
            .build();

    @Value("${app.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${app.rate-limit.capacity:50}")
    private int capacity;

    @Value("${app.rate-limit.refill-tokens:50}")
    private int refillTokens;

    @Value("${app.rate-limit.refill-duration:60s}")
    private Duration refillDuration;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = resolveClientIP(request);
        Bucket bucket = resolveBucket(ip);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            new ObjectMapper().writeValue(response.getOutputStream(),
                    ResponseUtils.withStatusAndData(HttpStatus.TOO_MANY_REQUESTS, MessageType.ERROR,
                            TOO_MANY_REQUESTS.getMessageText()).getBody());
        }
    }

    private String resolveClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }

    private Bucket resolveBucket(String ip) {
        return cache.get(ip, this::newBucket);
    }

    private Bucket newBucket(String apiKey) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(refillTokens, refillDuration)
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}