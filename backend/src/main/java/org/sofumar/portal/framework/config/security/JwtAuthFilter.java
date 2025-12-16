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
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
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
    private final BearerTokenResolver bearerTokenResolver;

    @Autowired
    public JwtAuthFilter(TokenBlacklistService blacklistService, BearerTokenResolver bearerTokenResolver) {
        this.blacklistService = blacklistService;
        this.bearerTokenResolver = bearerTokenResolver;
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

        String token = bearerTokenResolver.resolve(request);
        if (token != null) {
            try {
                // Check blacklist BEFORE parsing claims
                if (blacklistService.isTokenRevoked(token)) {
                    throw new AuthenticationCredentialsNotFoundException("Token has been revoked.");
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
                logger.debug("Invalid or expired JWT: {}", e.getMessage());
                SecurityContextHolder.clearContext();
                throw new AuthenticationCredentialsNotFoundException("Invalid or expired token.");
            }
        } else {
            throw new AuthenticationCredentialsNotFoundException("Token not found.");
        }
        chain.doFilter(request, response);
    }
}