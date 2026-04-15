package io.carizmi.domain.identity.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import io.carizmi.shared.constants.FieldConstants;
import io.carizmi.framework.data.response.GlobalResponse;
import io.carizmi.framework.util.ResponseUtils;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

import io.carizmi.domain.identity.service.User;
import org.springframework.security.authentication.BadCredentialsException;

import static io.carizmi.framework.message.constant.CommonMessages.ACCOUNT_DISABLED;
import static io.carizmi.framework.message.constant.CommonMessages.ACCOUNT_TEMP_LOCKED;
import static io.carizmi.framework.message.constant.CommonMessages.INVALID_CREDENTIALS;

@Component
@RequiredArgsConstructor
public class RestAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper;
    private final User user;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String username = (String) request.getAttribute(FieldConstants.USERNAME);

        if (username != null && exception instanceof BadCredentialsException) {
            user.onLoginFailure(username);
        }

        GlobalResponse<Void> body;
        if (exception instanceof LockedException) {
            body = ResponseUtils.unauthenticatedResp(ACCOUNT_TEMP_LOCKED.getMessageText());
        } else if (exception instanceof DisabledException) {
            body = ResponseUtils.unauthenticatedResp(ACCOUNT_DISABLED.getMessageText());
        } else {
            body = ResponseUtils.unauthenticatedResp(INVALID_CREDENTIALS.getMessageText());
        }

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}