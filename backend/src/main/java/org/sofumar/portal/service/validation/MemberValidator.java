package org.sofumar.portal.service.validation;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.sofumar.portal.constants.FieldConstants;
import org.sofumar.portal.message.ValidationMessages;
import org.sofumar.portal.constants.ReferenceConstants;
import org.sofumar.portal.core.vo.MemberVO;
import org.sofumar.portal.framework.bl.AbstractDomainValidator;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberValidator extends AbstractDomainValidator<MemberVO> {
    private static final String PHONE_REGEX = "^\\(?\\d{3}\\)?[- ]?\\d{3}[- ]?\\d{4}$";
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
    private static final String ZIP_REGEX = "^\\d{5}(-\\d{4})?$";

    private final ReferenceValidator referenceValidator;

    @Override
    public void validate(MemberVO vo) {
        validateFirstName(vo);
        validateLastName(vo);
        validatePhone(vo);
        validateEmail(vo);
        validateStatus(vo);
        validateState(vo);
        validateZip(vo);
    }

    @Override
    public void validateForUpdate(MemberVO vo) {
        validateMemberID(vo);
        validate(vo);
    }

    private void validateMemberID(MemberVO vo) {
        validateRequired(vo, FieldConstants.MEMBER_ID, vo.getMemberID());
    }

    private void validateFirstName(MemberVO vo) {
        validateRequired(vo, FieldConstants.FIRST_NAME, vo.getFirstName());
    }

    private void validateLastName(MemberVO vo) {
        validateRequired(vo, FieldConstants.LAST_NAME, vo.getLastName());
    }

    private void validatePhone(MemberVO vo) {
        if (StringUtils.isBlank(vo.getPhone())) {
            validateRequired(vo, FieldConstants.PHONE, vo.getPhone());
        } else {
            validateRegex(vo, FieldConstants.PHONE, vo.getPhone(), PHONE_REGEX);
        }
    }

    private void validateEmail(MemberVO vo) {
        if (StringUtils.isNotBlank(vo.getEmail())) {
            validateRegex(vo, FieldConstants.EMAIL, vo.getEmail(), EMAIL_REGEX);
        }
    }

    private void validateStatus(MemberVO vo) {
        if (StringUtils.isBlank(vo.getStatus())) {
            validateRequired(vo, FieldConstants.STATUS, vo.getStatus());
        } else {
            referenceValidator.validate(vo, FieldConstants.STATUS, ReferenceConstants.MEMBER_STATUS.NAME,
                    vo.getStatus());
        }
    }

    private void validateState(MemberVO vo) {
        validateRequired(vo, FieldConstants.STATE, vo.getState());
    }

    private void validateZip(MemberVO vo) {
        if (StringUtils.isNotBlank(vo.getZip())) {
            validateRegex(vo, FieldConstants.ZIP, vo.getZip(), ZIP_REGEX);
        }
    }
}