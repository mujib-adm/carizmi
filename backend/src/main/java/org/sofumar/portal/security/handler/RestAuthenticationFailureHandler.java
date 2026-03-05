package org.sofumar.portal.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.sofumar.portal.constants.FieldConstants;
import org.sofumar.portal.framework.message.Message;
import org.sofumar.portal.framework.util.ResponseUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

import org.sofumar.portal.core.businesslogic.User;
import org.springframework.security.authentication.BadCredentialsException;

@Component
@RequiredArgsConstructor
public class RestAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper;
    private final User user;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        String errorMessage = "Invalid username or password.";
        String username = (String) request.getAttribute(FieldConstants.USERNAME);

        if (username != null) {
            if (exception instanceof LockedException) {
                errorMessage = "Account temporarily locked. Try again later.";
            } else if (exception instanceof BadCredentialsException) {
                user.onLoginFailure(username);
            } else if (exception instanceof DisabledException) {
                errorMessage = "Your account is inactive. Please contact support for assistance.";
            }
        } else if (exception instanceof DisabledException) {
            errorMessage = "Your account is inactive. Please contact support for assistance.";
        }

        objectMapper.writeValue(response.getWriter(), ResponseUtils.withStatus(HttpStatus.UNAUTHORIZED, Message.Type.ERROR, errorMessage).getBody());
    }
}