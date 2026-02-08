package org.sofumar.portal.service.validation;

import org.apache.commons.lang3.StringUtils;
import org.sofumar.portal.constants.FieldConstants;
import org.sofumar.portal.constants.MessagesConstants;
import org.sofumar.portal.constants.Role;
import org.sofumar.portal.core.vo.UserVO;
import org.sofumar.portal.framework.exception.ValidationException;
import org.springframework.stereotype.Service;

@Service
public class UserValidator {
    private static final String USERNAME_REGEX = "^[a-zA-Z0-9._-]{4,}$";
    private static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[\\d\\W]).{12,}$";
    private static final String PASSWORD_REGEX_TEMP = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[\\d\\W]).{5,}$"; // for local testing only

    public void validate(UserVO vo) throws ValidationException {
        validateUsername(vo);
        validatePassword(vo);
        validateRole(vo);

        if (vo.hasErrors()) {
            throw new ValidationException(vo);
        }
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

    private void validateUsername(UserVO vo) {
        if (isNotMatchRegex(vo.getUsername(), USERNAME_REGEX)) {
            vo.addFieldMessage(FieldConstants.USERNAME, MessagesConstants.INVALID_USERNAME);
        }
    }

    private void validatePassword(UserVO vo) {
        if (isNotMatchRegex(vo.getPassword(), PASSWORD_REGEX_TEMP)) {
            vo.addFieldMessage(FieldConstants.PASSWORD, MessagesConstants.INVALID_PASSWORD);
        }
    }

    private void validateRole(UserVO vo) {
        String role = String.valueOf(vo.getRole());
        if (isInvalidRole(role)) {
            vo.addFieldMessage(FieldConstants.ROLE, MessagesConstants.INVALID_ROLE);
        }
    }

    private boolean isNotMatchRegex(String value, String regex) {
        if (StringUtils.isBlank(value)) {
            return true;
        }
        return !value.matches(regex);
    }

}
