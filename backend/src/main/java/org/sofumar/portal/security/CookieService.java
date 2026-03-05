package org.sofumar.portal.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;

/**
 * Centralizes httpOnly cookie management for JWT access and refresh tokens.
 */
@Service
public class CookieService {

    public static final String ACCESS_TOKEN_COOKIE = "access_token";
    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    // Map keys used in GlobalResponse token exchange
    public static final String TOKEN_MAP_KEY = "token";
    public static final String REFRESH_TOKEN_MAP_KEY = "refreshToken";

    @Value("${app.cookie.secure:false}")
    private boolean secure;

    @Value("${jwt.expirationMinutes:5}")
    private int accessTokenExpirationMinutes;

    @Value("${server.servlet.context-path:/api}")
    private String contextPath;

    @Value("${jwt.refreshTokenExpirationMinutes:15}")
    private long refreshTokenExpirationMinutes;

    public void addAccessTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = buildCookie(ACCESS_TOKEN_COOKIE, token, accessTokenExpirationMinutes * 60);
        response.addCookie(cookie);
    }

    public void addRefreshTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = buildCookie(REFRESH_TOKEN_COOKIE, token, (int) (refreshTokenExpirationMinutes * 60));
        response.addCookie(cookie);
    }

    public void clearAuthCookies(HttpServletResponse response) {
        Cookie accessCookie = buildCookie(ACCESS_TOKEN_COOKIE, "", 0);
        Cookie refreshCookie = buildCookie(REFRESH_TOKEN_COOKIE, "", 0);
        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);
    }

    public Optional<String> getAccessToken(HttpServletRequest request) {
        return getCookieValue(request, ACCESS_TOKEN_COOKIE);
    }

    public Optional<String> getRefreshToken(HttpServletRequest request) {
        return getCookieValue(request, REFRESH_TOKEN_COOKIE);
    }

    private Cookie buildCookie(String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setPath(StringUtils.isNotBlank(contextPath) ? contextPath : "/");
        cookie.setMaxAge(maxAge);
        // SameSite is set via response header since Cookie API doesn't support it directly
        cookie.setAttribute("SameSite", "Strict");
        return cookie;
    }

    private Optional<String> getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .filter(v -> v != null && !v.isEmpty())
                .findFirst();
    }
}