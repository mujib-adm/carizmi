package io.carizmi.infrastructure.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.carizmi.infrastructure.security.TokenBlacklistService;
import io.carizmi.domain.identity.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static io.carizmi.domain.identity.security.CookieService.ACCESS_TOKEN_COOKIE;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final List<String> EXCLUDED_PATHS = List.of(
            "/auth/login",
            "/auth/refresh",
            "/swagger-ui/index.html",
            "/v3/api-docs/**"
    );
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final TokenBlacklistService blacklistService;
    private final BearerTokenResolver bearerTokenResolver;
    private final JwtService jwtService;

    @Autowired
    public JwtAuthenticationFilter(TokenBlacklistService blacklistService, BearerTokenResolver bearerTokenResolver, JwtService jwtService) {
        this.blacklistService = blacklistService;
        this.bearerTokenResolver = bearerTokenResolver;
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
            throws IOException, ServletException {

        // allow CORS preflight requests to pass through immediately
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String path = request.getServletPath();
        // Skip excluded paths
        boolean isExcluded = path != null && EXCLUDED_PATHS.stream().anyMatch(p -> pathMatcher.match(p, path));
        if (isExcluded) {
            chain.doFilter(request, response);
            return;
        }

        // Read token from httpOnly cookie first, fallback to Authorization header (for Swagger/API testing)
        String token = getTokenFromCookie(request);
        if (token == null) {
            token = bearerTokenResolver.resolve(request);
        }
        if (token != null) {
            try {
                if (blacklistService.isTokenRevoked(token)) {
                    logger.info("Token has been revoked.");
                    SecurityContextHolder.clearContext();
                } else {
                    Claims claims = jwtService.getClaims(token);
                    String username = claims.getSubject();
                    @SuppressWarnings("unchecked")
                    List<String> roles = (List<String>) claims.get("roles");
                    Collection<GrantedAuthority> auths = roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).collect(Collectors.toSet());

                    UserDetails principal = new User(username, "", auths);
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(principal, null, auths);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (JwtException e) {
                logger.info("Invalid or expired JWT: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

    private String getTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
                String value = cookie.getValue();
                return (value != null && !value.isEmpty()) ? value : null;
            }
        }
        return null;
    }
}