package io.carizmi.domain.finance.validation;

import lombok.RequiredArgsConstructor;
import io.carizmi.domain.platform.constants.BaselineConstants;
import io.carizmi.domain.platform.service.SystemSetting;
import org.apache.commons.lang3.StringUtils;
import io.carizmi.shared.constants.FieldConstants;
import io.carizmi.shared.constants.ReferenceConstants;
import io.carizmi.shared.message.ValidationMessages;
import io.carizmi.domain.finance.model.PaymentVO;
import io.carizmi.framework.bl.AbstractDomainValidator;
import io.carizmi.domain.platform.validation.ReferenceValidator;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentValidator extends AbstractDomainValidator<PaymentVO> {

    private final ReferenceValidator referenceValidator;
    private final SystemSetting systemSetting;

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
        validate(vo);
    }

    private void validateMember(PaymentVO vo) {
        if (vo.getMember() == null) {
            validateRequired(vo, FieldConstants.MEMBER_ID, null);
        } else {
            validateRequired(vo, FieldConstants.MEMBER_ID, vo.getMember().getMemberID());
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
        }
    }
}