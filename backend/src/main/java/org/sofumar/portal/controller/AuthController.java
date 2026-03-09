package org.sofumar.portal.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sofumar.portal.core.businesslogic.User;
import org.sofumar.portal.data.dto.UserDto;
import org.sofumar.portal.data.dto.request.PasswordUpdateRequestDto;
import org.sofumar.portal.data.dto.response.UserProfileDto;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.sofumar.portal.framework.util.ResponseUtils;
import org.sofumar.portal.security.CookieService;
import org.sofumar.portal.security.annotation.IsAdmin;
import org.sofumar.portal.security.annotation.IsAuthenticated;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.sofumar.portal.security.CookieService.REFRESH_TOKEN_MAP_KEY;
import static org.sofumar.portal.security.CookieService.TOKEN_MAP_KEY;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final User user;
    private final CookieService cookieService;

    @PostMapping("/register")
    @IsAdmin
    public ResponseEntity<GlobalResponse<Void>> register(@Valid @RequestBody UserDto requestDto) {
        return user.register(requestDto);
    }

    @PostMapping("/logout")
    @IsAuthenticated
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        // Read tokens from cookies for blacklisting
        String accessToken = cookieService.getAccessToken(request).orElse(null);
        String refreshToken = cookieService.getRefreshToken(request).orElse(null);

        user.logout(accessToken, refreshToken);

        // Clear auth cookies
        cookieService.clearAuthCookies(response);

        return ResponseEntity.ok(Map.of("message", "Successfully logged out"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookieService.getRefreshToken(request).orElse(null);
        if (refreshToken == null) {
            return ResponseUtils.badRequest("Refresh token is required.");
        }
        ResponseEntity<?> result = user.refreshToken(refreshToken);

        // If refresh was successful, set new cookies from the response map
        if (result.getStatusCode() == HttpStatus.OK) {
            Object responseBody = result.getBody();
            if (responseBody instanceof GlobalResponse<?>) {
                @SuppressWarnings("unchecked")
                GlobalResponse<Void> body = (GlobalResponse<Void>) responseBody;
                if (body.getMap() != null) {
                    String newAccessToken = body.getMap().get(TOKEN_MAP_KEY);
                    String newRefreshToken = body.getMap().get(REFRESH_TOKEN_MAP_KEY);
                    if (newAccessToken != null) {
                        cookieService.addAccessTokenCookie(response, newAccessToken);
                    }
                    if (newRefreshToken != null) {
                        cookieService.addRefreshTokenCookie(response, newRefreshToken);
                    }
                    // Remove tokens from map in response body (they're in cookies)
                    body.setMap(null);
                }
            }
        }
        return result;
    }

    @GetMapping("/profile")
    @IsAuthenticated
    public ResponseEntity<GlobalResponse<UserProfileDto>> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        return user.getProfile(userDetails.getUsername());
    }

    @PostMapping("/password-update")
    @IsAuthenticated
    public ResponseEntity<GlobalResponse<Void>> updatePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request,
            HttpServletResponse response,
            @Valid @RequestBody PasswordUpdateRequestDto requestDto) {

        // Read access token from cookie for blacklisting after password change
        String token = cookieService.getAccessToken(request).orElse(null);

        ResponseEntity<GlobalResponse<Void>> result = user.updatePassword(userDetails.getUsername(), token, requestDto);

        // If password update was successful, clear cookies to force re-login
        if (result.getStatusCode() == HttpStatus.OK) {
            cookieService.clearAuthCookies(response);
        }
        return result;
    }
}