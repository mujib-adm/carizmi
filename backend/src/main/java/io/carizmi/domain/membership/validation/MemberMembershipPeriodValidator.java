package io.carizmi.domain.membership.validation;

import io.carizmi.domain.membership.model.MemberMembershipPeriodVO;
import io.carizmi.framework.bl.AbstractDomainValidator;
import io.carizmi.shared.constants.FieldConstants;
import io.carizmi.shared.message.ValidationMessages;
import org.springframework.stereotype.Service;

@Service
public class MemberMembershipPeriodValidator extends AbstractDomainValidator<MemberMembershipPeriodVO> {

    @Override
    public void validate(MemberMembershipPeriodVO vo) {
        validateRequired(vo, FieldConstants.MEMBER_ID, vo.getMemberID());
        validateRequired(vo, FieldConstants.START_DATE, vo.getStartDate());
        if (vo.getEndDate() != null && vo.getStartDate() != null && vo.getEndDate().isBefore(vo.getStartDate())) {
            vo.addFieldMessage(FieldConstants.END_DATE, ValidationMessages.ERR_LEAVE_BEFORE_JOIN);
        }
    }

    @Override
    public void validateForUpdate(MemberMembershipPeriodVO vo) {
        validateRequired(vo, FieldConstants.PERIOD_ID, vo.getPeriodID());
        validate(vo);
    }
}