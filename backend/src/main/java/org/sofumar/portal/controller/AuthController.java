package org.sofumar.portal.controller;

import lombok.RequiredArgsConstructor;
import org.sofumar.portal.data.dto.UserProfileDto;
import org.sofumar.portal.data.dto.request.LoginRequest;
import org.sofumar.portal.data.dto.request.PasswordUpdateRequestDto;
import org.sofumar.portal.data.dto.request.UserRequestDto;
import org.sofumar.portal.framework.data.msg.Message;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.sofumar.portal.framework.util.ResponseUtils;
import org.sofumar.portal.service.businesslogic.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<GlobalResponse<Void>> register(@RequestBody UserRequestDto requestDto) {
        return userService.register(requestDto);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        return userService.login(request.username(), request.password());
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(name = "Authorization", required = false) String authHeader, @RequestBody(required = false) Map<String, String> body) {
        String accessToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }
        String refreshToken = (body != null) ? body.get("refreshToken") : null;
        
        userService.logout(accessToken, refreshToken);
        return ResponseEntity.ok(Map.of("message", "Successfully logged out"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null) {
            return ResponseUtils.badRequest("Refresh token is required.");
        }
        return userService.refreshToken(refreshToken);
    }

    @GetMapping("/profile")
    public ResponseEntity<GlobalResponse<UserProfileDto>> getCurrentUser(@AuthenticationPrincipal UserDetails user) {
        if (user == null) {
            return ResponseUtils.withStatusAndData(HttpStatus.UNAUTHORIZED, Message.Type.ERROR, "Session expired or User not logged in.");
        }
        return userService.getProfile(user.getUsername());
    }

    @PostMapping("/password-update")
    public ResponseEntity<GlobalResponse<Void>> updatePassword(@AuthenticationPrincipal UserDetails user, @RequestHeader(name = "Authorization") String authHeader, @RequestBody PasswordUpdateRequestDto requestDto) {
        if (user == null) {
            return ResponseUtils.withStatus(HttpStatus.UNAUTHORIZED, Message.Type.ERROR, "Session expired or User not logged in.");
        }
        
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        
        return userService.updatePassword(user.getUsername(), token, requestDto);
    }
}
