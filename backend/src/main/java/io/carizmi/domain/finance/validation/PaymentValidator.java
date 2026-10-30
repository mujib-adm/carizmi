package io.carizmi.domain.finance.validation;

import io.carizmi.domain.finance.model.PaymentVO;
import io.carizmi.domain.membership.service.MemberMembershipPeriod;
import io.carizmi.domain.platform.constants.BaselineConstants;
import io.carizmi.domain.platform.service.SystemSetting;
import io.carizmi.domain.platform.validation.ReferenceValidator;
import io.carizmi.framework.bl.AbstractDomainValidator;
import io.carizmi.shared.constants.FieldConstants;
import io.carizmi.shared.constants.ReferenceConstants;
import io.carizmi.shared.message.ValidationMessages;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentValidator extends AbstractDomainValidator<PaymentVO> {

    private final ReferenceValidator referenceValidator;
    private final SystemSetting systemSetting;
    private final MemberMembershipPeriod memberMembershipPeriod;

    @Override
    public void validate(PaymentVO vo) {
        validateMember(vo);
        validateFeeType(vo);
        validateAmount(vo);
        validateDateReceived(vo);
        validateMethodOfPayment(vo);
        validatePeriodLogic(vo);
    }

    @Override
    public void validateForUpdate(PaymentVO vo) {
        validateRequired(vo, FieldConstants.PAYMENT_ID, vo.getPaymentID());
        validateRecordNotClosed(vo);
        validate(vo);
    }

    @Override
    public void validateForDelete(PaymentVO vo) {
        if (vo.getDateReceived() != null) {
            int receiptYear = vo.getDateReceived().getYear();
            String closingKey = BaselineConstants.getYearlyBaselineKey(receiptYear + 1);
            if (systemSetting.findBySettingKey(closingKey).isPresent()) {
                vo.addFieldMessage(FieldConstants.DATE_RECEIVED,
                        ValidationMessages.ERR_TRANSACTION_CLOSED_YEAR_DELETE.addMessageArgs(String.valueOf(receiptYear)));
            }
        }
    }

    private void validateRecordNotClosed(PaymentVO existingVO) {
        if (existingVO.getDateReceived() != null) {
            int receiptYear = existingVO.getDateReceived().getYear();
            String closingKey = BaselineConstants.getYearlyBaselineKey(receiptYear + 1);
            if (systemSetting.findBySettingKey(closingKey).isPresent()) {
                existingVO.addFieldMessage(FieldConstants.DATE_RECEIVED,
                        ValidationMessages.ERR_TRANSACTION_CLOSED_YEAR_MUTATION.addMessageArgs(String.valueOf(receiptYear)));
            }
        }
    }

    private void validateMember(PaymentVO vo) {
        if (vo.getMember() == null) {
            validateRequired(vo, FieldConstants.MEMBER_ID, null);
            return;
        }

        validateRequired(vo, FieldConstants.MEMBER_ID, vo.getMember().getMemberID());
        if (ReferenceConstants.MEMBER_STATUS.PENDING.equals(vo.getMember().getStatus())) {
            if (!ReferenceConstants.FEE_TYPE.REGISTRATION_FEE.equals(vo.getFeeType())) {
                vo.addFieldMessage(FieldConstants.MEMBER_ID, ValidationMessages.ERR_PAYMENT_MEMBER_PENDING);
            }
        }
    }

    private void validateFeeType(PaymentVO vo) {
        if (StringUtils.isBlank(vo.getFeeType())) {
            validateRequired(vo, FieldConstants.FEE_TYPE, vo.getFeeType());
        } else {
            referenceValidator.validate(vo, FieldConstants.FEE_TYPE, ReferenceConstants.FEE_TYPE.NAME, vo.getFeeType());
        }
    }

    private void validateAmount(PaymentVO vo) {
        validateRequired(vo, FieldConstants.AMOUNT, vo.getAmount());
    }

    private void validateDateReceived(PaymentVO vo) {
        validateRequired(vo, FieldConstants.DATE_RECEIVED, vo.getDateReceived());
        if (vo.getDateReceived() != null) {
            int receiptYear = vo.getDateReceived().getYear();
            String closingKey = BaselineConstants.getYearlyBaselineKey(receiptYear + 1);
            if (systemSetting.findBySettingKey(closingKey).isPresent()) {
                vo.addFieldMessage(FieldConstants.DATE_RECEIVED,
                        ValidationMessages.ERR_FISCAL_YEAR_CLOSED.addMessageArgs(String.valueOf(receiptYear)));
            }
        }
    }

    private void validateMethodOfPayment(PaymentVO vo) {
        if (StringUtils.isBlank(vo.getMethodOfPayment())) {
            validateRequired(vo, FieldConstants.METHOD_OF_PAYMENT, vo.getMethodOfPayment());
        } else {
            referenceValidator.validate(vo, FieldConstants.METHOD_OF_PAYMENT,
                    ReferenceConstants.PAYMENT_METHOD.NAME,
                    vo.getMethodOfPayment());
        }
    }

    private void validatePeriodLogic(PaymentVO vo) {
        if (ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE.equals(vo.getFeeType())) {
            validateRequired(vo, FieldConstants.YEAR, vo.getYear());
            validateRequired(vo, FieldConstants.QUARTER, vo.getQuarter());

            if (vo.getYear() != null && vo.getQuarter() != null && vo.getMember() != null && vo.getMember().getMemberID() != null) {
                boolean isActiveInPeriod = memberMembershipPeriod.isMemberActiveInQuarter(vo.getMember().getMemberID(), vo.getYear(), vo.getQuarter());

                if (!isActiveInPeriod) {
                    vo.addFieldMessage(FieldConstants.QUARTER,
                            ValidationMessages.ERR_PAYMENT_MEMBER_NOT_ACTIVE_IN_PERIOD.addMessageArgs(
                                    String.valueOf(vo.getYear()), vo.getQuarter()));
                }
            }
        }
    }
}