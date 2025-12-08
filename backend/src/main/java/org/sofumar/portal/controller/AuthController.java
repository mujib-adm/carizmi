package org.sofumar.portal.controller;

import org.sofumar.portal.data.dto.request.LoginRequest;
import org.sofumar.portal.data.dto.request.UserRequestDto;
import org.sofumar.portal.framework.data.msg.Message;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.sofumar.portal.framework.util.ResponseUtils;
import org.sofumar.portal.service.businesslogic.UserService;
import org.springframework.beans.factory.annotation.Autowired;
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
public class AuthController {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<GlobalResponse> register(@RequestBody UserRequestDto requestDto) {
        return userService.register(requestDto);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        return userService.login(request.username(), request.password());
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(name = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            userService.logout(token);
        }
        return ResponseEntity.ok(Map.of("message", "Successfully logged out"));
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserDetails user) {
        if (user == null) {
            return ResponseUtils.withStatus(HttpStatus.UNAUTHORIZED, Message.Type.ERROR, "Session expired or User not logged in.");
        }
        return ResponseUtils.withMap(Map.of("username", user.getUsername(), "role", user.getAuthorities().toString()));
    }
}


