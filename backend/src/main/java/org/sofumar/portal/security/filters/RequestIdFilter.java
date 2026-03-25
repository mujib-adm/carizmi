package org.sofumar.portal.security.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Generates a unique {@code requestId} for every incoming HTTP request and
 * stores it in the SLF4J MDC (Mapped Diagnostic Context).
 *
 * <h3>How it works</h3>
 * <p>MDC is a thread-local map managed by SLF4J. By calling
 * {@code MDC.put("requestId", uuid)} before the request is processed, every
 * subsequent {@code logger.info(...)}, {@code logger.error(...)}, etc.
 * executed on the same thread automatically has access to the {@code requestId}
 * — <strong>no changes to individual log statements are needed</strong>.</p>
 *
 * <h3>Production (JSON structured logging)</h3>
 * <p>In the {@code prod} profile, {@code logback-spring.xml} uses the
 * Logstash encoder ({@code LogstashEncoder}), which automatically serializes
 * <em>all</em> MDC fields into the JSON log output. Every log line produced
 * during a request will include the {@code requestId} field, for example:</p>
 * <pre>{@code
 * {
 *   "timestamp": "2026-03-23T18:23:43.123Z",
 *   "level": "INFO",
 *   "logger": "o.s.p.security.JwtAuthenticationFilter",
 *   "message": "Token has been revoked.",
 *   "requestId": "a3f8d1b6-0b3b-4b1a-9c1a-1a2b3c4d5e6f"
 * }
 * }</pre>
 * <p>This enables per-request log correlation by grepping for a single
 * {@code requestId} value across all log output.</p>
 *
 * <h3>Filter ordering</h3>
 * <p>Runs at {@code Ordered.HIGHEST_PRECEDENCE + 1} (just after
 * {@code CorsFilter}) so that every downstream filter, controller,
 * and service benefits from the {@code requestId}.</p>
 *
 * <h3>Client-supplied ID</h3>
 * <p>If the incoming request already carries an {@code X-Request-Id} header
 * (e.g. set by a load balancer or API gateway), that value is reused instead
 * of generating a new UUID. The same value is echoed back in the response
 * header for client-side correlation.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1) // Just after CorsFilter
public class RequestIdFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_KEY = "requestId";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // Accept a client-supplied request ID (e.g. from a load balancer) or generate one
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (StringUtils.isBlank(requestId)) {
            requestId = UUID.randomUUID().toString();
        }

        MDC.put(REQUEST_ID_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(REQUEST_ID_KEY);
        }
    }
}