package org.sofumar.portal.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.sofumar.portal.core.businesslogic.User;
import org.sofumar.portal.data.dto.response.LoginResponseDto;
import org.sofumar.portal.framework.service.RefreshTokenService;
import org.sofumar.portal.framework.util.ResponseUtils;
import org.sofumar.portal.security.JwtService;
import org.sofumar.portal.security.SofumarUserDetails;
import org.sofumar.portal.security.CookieService;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RestAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final RefreshTokenService refreshTokenService;
    private final ObjectMapper objectMapper;
    private final User user;
    private final JwtService jwtService;
    private final CookieService cookieService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        SofumarUserDetails userDetails = (SofumarUserDetails) authentication.getPrincipal();

        // Delegate success logic (resetting attempts) to BL
        user.onLoginSuccess(userDetails.getUsername());

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = refreshTokenService.createRefreshToken(userDetails.getUsername());

        // Set tokens as httpOnly cookies
        cookieService.addAccessTokenCookie(response, accessToken);
        cookieService.addRefreshTokenCookie(response, refreshToken);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        // Return non-sensitive metadata in the JSON body (tokens are in cookies)
        LoginResponseDto loginResponse = new LoginResponseDto(
                userDetails.getUserVO().getRole().name(),
                userDetails.getUserVO().getFirstName()
        );

        objectMapper.writeValue(response.getOutputStream(), ResponseUtils.okWithData(loginResponse).getBody());
    }
}