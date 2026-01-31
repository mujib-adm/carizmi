package org.sofumar.portal.service.validation;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.sofumar.portal.constants.FieldConstants;
import org.sofumar.portal.constants.MessagesConstants;
import org.sofumar.portal.core.vo.MemberVO;
import org.sofumar.portal.framework.exception.ValidationException;
import org.sofumar.portal.framework.util.LabelUtils;
import org.springframework.stereotype.Service;

import static org.sofumar.portal.constants.MessagesConstants.REQUIRED_FIELD;

import org.sofumar.portal.constants.ReferenceCodeConstants;

@Service
@RequiredArgsConstructor
public class MemberValidator {
    private static final String PHONE_REGEX = "^\\(?\\d{3}\\)?[- ]?\\d{3}[- ]?\\d{4}$";
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
    private static final String ZIP_REGEX = "^\\d{5}(-\\d{4})?$";

    private final ReferenceValidator referenceValidator;

    public void validate(MemberVO vo) throws ValidationException {
        validateFirstName(vo);
        validateLastName(vo);
        validatePhone(vo);
        validateEmail(vo);
        validateStatus(vo);
        validateState(vo);
        validateZip(vo);

        if (vo.hasErrors()) {
            throw new ValidationException(vo);
        }
    }

    public void validateForUpdate(MemberVO vo) throws ValidationException {
        validateMemberID(vo);
        validate(vo);
    }

    private void validateMemberID(MemberVO vo) {
        if (vo.getMemberID() == null) {
            vo.addFieldMessage(FieldConstants.MEMBER_ID, REQUIRED_FIELD.addMessageArgs(LabelUtils.toLabel(FieldConstants.MEMBER_ID)));
        }
    }

    private void validateFirstName(MemberVO vo) {
        if (StringUtils.isBlank(vo.getFirstName())) {
            vo.addFieldMessage(FieldConstants.FIRST_NAME, REQUIRED_FIELD.addMessageArgs(LabelUtils.toLabel(FieldConstants.FIRST_NAME)));
        }
    }

    private void validateLastName(MemberVO vo) {
        if (StringUtils.isBlank(vo.getLastName())) {
            vo.addFieldMessage(FieldConstants.LAST_NAME, REQUIRED_FIELD.addMessageArgs(LabelUtils.toLabel(FieldConstants.LAST_NAME)));
        }
    }

    private void validatePhone(MemberVO vo) {
        if (StringUtils.isBlank(vo.getPhone())) {
            vo.addFieldMessage(FieldConstants.PHONE, REQUIRED_FIELD.addMessageArgs(LabelUtils.toLabel(FieldConstants.PHONE)));
        } else if (isNotMatchRegex(vo.getPhone(), PHONE_REGEX)) {
            vo.addFieldMessage(FieldConstants.PHONE, MessagesConstants.INVALID_VALUE);
        }
    }

    private void validateEmail(MemberVO vo) {
        if (StringUtils.isNotBlank(vo.getEmail()) && isNotMatchRegex(vo.getEmail(), EMAIL_REGEX)) {
            vo.addFieldMessage(FieldConstants.EMAIL, MessagesConstants.INVALID_VALUE);
        }
    }

    private void validateStatus(MemberVO vo) {
        if (StringUtils.isBlank(vo.getStatus())) {
            vo.addFieldMessage(FieldConstants.STATUS,
                    REQUIRED_FIELD.addMessageArgs(LabelUtils.toLabel(FieldConstants.STATUS)));
        } else {
            referenceValidator.validate(vo, FieldConstants.STATUS, ReferenceCodeConstants.MEMBER_STATUS.NAME,
                    vo.getStatus());
        }
    }

    private void validateState(MemberVO vo) {
        if (StringUtils.isBlank(vo.getState())) {
            vo.addFieldMessage(FieldConstants.STATE, REQUIRED_FIELD.addMessageArgs(LabelUtils.toLabel(FieldConstants.STATE)));
        }
    }

    private void validateZip(MemberVO vo) {
        if (StringUtils.isNotBlank(vo.getZip()) && isNotMatchRegex(vo.getZip(), ZIP_REGEX)) {
            vo.addFieldMessage(FieldConstants.ZIP, MessagesConstants.INVALID_VALUE);
        }
    }

    private boolean isNotMatchRegex(String value, String regex) {
        if (StringUtils.isBlank(value)) {
            return true;
        }
        return !value.matches(regex);
    }
}