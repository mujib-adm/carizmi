package io.carizmi.infrastructure.filter;

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
 * Assigns a unique {@code requestId} to every HTTP request via SLF4J MDC,
 * enabling per-request log correlation without modifying individual log statements.
 *
 * <p>Reuses the client-supplied {@code X-Request-Id} header if present;
 * otherwise generates a UUID. The ID is echoed in the response header.</p>
 *
 * <p>Runs at {@code Ordered.HIGHEST_PRECEDENCE + 1} (just after CorsFilter).</p>
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