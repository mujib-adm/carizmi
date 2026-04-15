package io.carizmi.infrastructure.bootstrap;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import io.carizmi.domain.identity.model.UserVO;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "admin.default")
@Validated
public record AdminDefault(
        @NotBlank String firstname,
        @NotBlank String lastname,
        @Email @NotBlank String email,
        @NotBlank String username,
        String password
) {
    public UserVO toUserVO() {
        UserVO userVO = new UserVO();
        userVO.setFirstName(firstname);
        userVO.setLastName(lastname);
        userVO.setEmail(email);
        userVO.setUsername(username);
        userVO.setPassword(password);
        return userVO;
    }
}