package org.sofumar.portal.config.props;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.sofumar.portal.core.vo.UserVO;
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