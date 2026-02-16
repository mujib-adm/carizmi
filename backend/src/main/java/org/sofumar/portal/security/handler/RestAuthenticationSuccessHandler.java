package org.sofumar.portal.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.sofumar.portal.core.businesslogic.User;
import org.sofumar.portal.framework.service.RefreshTokenService;
import org.sofumar.portal.framework.util.ResponseUtils;
import org.sofumar.portal.security.JwtService;
import org.sofumar.portal.security.SofumarUserDetails;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RestAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final RefreshTokenService refreshTokenService;
    private final ObjectMapper objectMapper;
    private final User user;
    private final JwtService jwtService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        SofumarUserDetails userDetails = (SofumarUserDetails) authentication.getPrincipal();

        // Delegate success logic (resetting attempts) to BL
        user.onLoginSuccess(userDetails.getUsername());

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = refreshTokenService.createRefreshToken(userDetails.getUsername());

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, String> data = Map.of(
                "token", accessToken,
                "refreshToken", refreshToken,
                "role", userDetails.getUserVO().getRole().name(),
                "firstName", userDetails.getUserVO().getFirstName()
        );

        objectMapper.writeValue(response.getWriter(), ResponseUtils.withMap(data).getBody());
    }
}