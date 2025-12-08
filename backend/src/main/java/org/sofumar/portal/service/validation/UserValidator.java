package org.sofumar.portal.service.validation;

import io.micrometer.common.util.StringUtils;
import org.sofumar.portal.constants.FieldConstants;
import org.sofumar.portal.constants.MessagesConstants;
import org.sofumar.portal.data.vo.UserVO;
import org.sofumar.portal.framework.dao.sql.exception.ValidationException;
import org.springframework.stereotype.Service;

@Service
public class UserValidator {
    private static final String USERNAME_REGEX = "^[a-zA-Z0-9._-]{4,}$";
    private static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[\\d\\W]).{12,}$";
    private static final String PASSWORD_REGEX_TEMP = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[\\d\\W]).{5,}$"; // for local testing only

    public void validate(UserVO vo) {
        validateUsername(vo);
        validatePassword(vo);

        if (vo.hasErrors()) {
            throw new ValidationException(vo);
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

    private boolean isNotMatchRegex(String value, String regex) {
        if (StringUtils.isBlank(value)) {
            return true;
        }
        return !value.matches(regex);
    }

}
