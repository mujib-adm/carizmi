package org.sofumar.portal.framework.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sofumar.portal.framework.service.TokenBlacklistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final List<String> EXCLUDED_PATHS = List.of(
            "/auth/**",
            "/api/auth/**",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    );
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Value("${jwt.secret}")
    private String secret;

    private final TokenBlacklistService blacklistService;
    @Autowired
    public JwtAuthFilter(TokenBlacklistService blacklistService) {
        this.blacklistService = blacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String path = request.getServletPath();
        // Skip excluded paths
        boolean isExcluded = EXCLUDED_PATHS.stream().anyMatch(p -> pathMatcher.match(p, path));
        if (isExcluded) {
            chain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                // Check blacklist BEFORE parsing claims
                if (blacklistService.isTokenRevoked(token)) {
//                    throw new JwtException("Token has been revoked.");
                    sendJsonError(response, "Token has been revoked.");
                    return;
                }
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret)))
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                String username = claims.getSubject();
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) claims.get("roles");
                Collection<GrantedAuthority> auths = roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).collect(Collectors.toSet());
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(username, null, auths);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException e) {
                logger.debug("Invalid JWT: {}", e.getMessage());
                SecurityContextHolder.clearContext();
                // chain.doFilter(request, response); // do not sendError here
                sendJsonError(response, "Invalid or expired token");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private void sendJsonError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(
                String.format("{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\"}",
                        java.time.Instant.now().toString(), HttpServletResponse.SC_UNAUTHORIZED, message)
        );
    }
}