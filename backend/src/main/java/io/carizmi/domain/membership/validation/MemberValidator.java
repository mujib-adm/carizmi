package io.carizmi.domain.membership.validation;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import io.carizmi.shared.constants.FieldConstants;
import io.carizmi.shared.constants.ReferenceConstants;
import io.carizmi.domain.membership.model.MemberVO;
import io.carizmi.framework.bl.AbstractDomainValidator;
import io.carizmi.domain.platform.validation.ReferenceValidator;
import io.carizmi.shared.util.PhoneUtils;
import org.springframework.stereotype.Service;

import io.carizmi.domain.finance.service.Payment;
import io.carizmi.domain.platform.constants.BaselineConstants;
import io.carizmi.domain.platform.service.SystemSetting;
import io.carizmi.shared.message.ValidationMessages;

@Service
@RequiredArgsConstructor
public class MemberValidator extends AbstractDomainValidator<MemberVO> {
    private static final String PHONE_REGEX = PhoneUtils.US_PHONE_REGEX;
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
    private static final String ZIP_REGEX = "^\\d{5}(-\\d{4})?$";

    private final ReferenceValidator referenceValidator;
    private final Payment payment;
    private final SystemSetting systemSetting;

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

    @Override
    public void validateForDelete(MemberVO vo) {
        if (vo.getMemberID() != null && payment.hasPaymentsForMember(vo.getMemberID())) {
            vo.addGlobalMessage(ValidationMessages.ERR_MEMBER_HISTORICAL_STANDING);
            return;
        }

        if (vo.getJoinDate() != null) {
            int joinYear = vo.getJoinDate().getYear();
            String closingKey = BaselineConstants.getYearlyBaselineKey(joinYear + 1);
            if (systemSetting.findBySettingKey(closingKey).isPresent()) {
                vo.addGlobalMessage(ValidationMessages.ERR_MEMBER_HISTORICAL_STANDING);
            }
        }
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