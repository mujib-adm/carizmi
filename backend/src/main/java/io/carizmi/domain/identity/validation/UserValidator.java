package io.carizmi.domain.identity.validation;

import org.apache.commons.lang3.StringUtils;
import io.carizmi.shared.constants.FieldConstants;
import io.carizmi.shared.message.ValidationMessages;
import io.carizmi.shared.constants.Role;
import io.carizmi.domain.identity.model.UserVO;
import io.carizmi.framework.bl.AbstractDomainValidator;
import org.springframework.stereotype.Service;

@Service
public class UserValidator extends AbstractDomainValidator<UserVO> {
    private static final String USERNAME_REGEX = "^[a-zA-Z0-9._-]{4,}$";
    private static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[\\d\\W]).{8,}$";

    @Override
    public void validate(UserVO vo) {
        validateNames(vo);
        validateUsername(vo);
        validateEmail(vo);
        validateRole(vo);
    }

    /**
     * Raw Password validation: validates the PLAINTEXT password against the regex.
     * Must be called explicitly by business methods BEFORE the VO enters the add()/update() lifecycle where encoding occurs.
     */
    public void validatePassword(UserVO vo) {
        validateRegex(vo, FieldConstants.PASSWORD, vo.getPassword(), PASSWORD_REGEX, ValidationMessages.INVALID_PASSWORD);
    }

    public boolean isInvalidRole(String role) {
        if (StringUtils.isBlank(role)) return true;
        try {
            Role.valueOf(role);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private void validateNames(UserVO vo) {
        validateRequired(vo, FieldConstants.FIRST_NAME, vo.getFirstName());
        validateRequired(vo, FieldConstants.LAST_NAME, vo.getLastName());
    }

    private void validateUsername(UserVO vo) {
        validateRegex(vo, FieldConstants.USERNAME, vo.getUsername(), USERNAME_REGEX, ValidationMessages.INVALID_USERNAME);
    }

    private void validateEmail(UserVO vo) {
        if (StringUtils.isBlank(vo.getEmail())) {
            validateRequired(vo, FieldConstants.EMAIL, vo.getEmail());
            return;
        }
        // Basic email format check (VO has @Email annotation for JPA, but we do domain check here for fail-fast)
        if (!vo.getEmail().contains("@")) {
            vo.addFieldMessage(FieldConstants.EMAIL, ValidationMessages.INVALID_VALUE);
        }
    }

    private void validateRole(UserVO vo) {
        if (vo.getRole() == null) {
            validateRequired(vo, FieldConstants.ROLE, null);
            return;
        }
        String role = String.valueOf(vo.getRole());
        if (isInvalidRole(role)) {
            vo.addFieldMessage(FieldConstants.ROLE, ValidationMessages.INVALID_ROLE);
        }
    }

}