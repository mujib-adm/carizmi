package io.carizmi.domain.identity.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import io.carizmi.domain.identity.service.User;
import io.carizmi.domain.identity.data.dto.UserDto;
import io.carizmi.domain.identity.data.dto.request.PasswordUpdateRequestDto;
import io.carizmi.domain.identity.data.dto.response.TokenDto;
import io.carizmi.domain.identity.data.dto.response.UserProfileDto;
import io.carizmi.framework.data.response.GlobalResponse;
import io.carizmi.framework.util.ResponseUtils;
import io.carizmi.domain.identity.security.CookieService;
import io.carizmi.infrastructure.security.annotation.IsAdmin;
import io.carizmi.infrastructure.security.annotation.IsAuthenticated;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Authentication and account management APIs")
@RequiredArgsConstructor
public class AuthController {

    private final User user;
    private final CookieService cookieService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    @IsAdmin
    public ResponseEntity<GlobalResponse<Void>> register(@Valid @RequestBody UserDto requestDto) {
        user.register(requestDto);
        return ResponseUtils.ok("User registered successfully.");
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout current user")
    @IsAuthenticated
    public ResponseEntity<GlobalResponse<Void>> logout(HttpServletRequest request, HttpServletResponse response) {
        // Read tokens from cookies for blacklisting
        String accessToken = cookieService.getAccessToken(request).orElse(null);
        String refreshToken = cookieService.getRefreshToken(request).orElse(null);

        user.logout(accessToken, refreshToken);

        // Clear auth cookies
        cookieService.clearAuthCookies(response);

        return ResponseUtils.ok("Logged out successfully");
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<GlobalResponse<Void>> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookieService.getRefreshToken(request).orElse(null);
        if (refreshToken == null) {
            return ResponseUtils.badRequest("Refresh token is required.");
        }

        TokenDto tokenDto = user.refreshToken(refreshToken);

        if (tokenDto.getToken() != null) {
            cookieService.addAccessTokenCookie(response, tokenDto.getToken());
        }
        if (tokenDto.getRefreshToken() != null) {
            cookieService.addRefreshTokenCookie(response, tokenDto.getRefreshToken());
        }

        // Don't expose tokens in response body (they're in cookies)
        return ResponseUtils.ok("Token refreshed successfully");
    }

    @GetMapping("/profile")
    @Operation(summary = "Get current user profile")
    @IsAuthenticated
    public ResponseEntity<GlobalResponse<UserProfileDto>> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseUtils.okWithData(user.getProfile(userDetails.getUsername()));
    }

    @PostMapping("/password-update")
    @Operation(summary = "Update current user password")
    @IsAuthenticated
    public ResponseEntity<GlobalResponse<Void>> updatePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request,
            HttpServletResponse response,
            @Valid @RequestBody PasswordUpdateRequestDto requestDto) {

        // Read access token from cookie for blacklisting after password change
        String token = cookieService.getAccessToken(request).orElse(null);
        user.updatePassword(userDetails.getUsername(), token, requestDto);

        // If we reach here, password update was successful — clear cookies to force re-login
        cookieService.clearAuthCookies(response);

        return ResponseUtils.ok("Password updated. Please sign in again.");
    }
}