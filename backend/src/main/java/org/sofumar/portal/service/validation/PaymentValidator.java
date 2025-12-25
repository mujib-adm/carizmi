package org.sofumar.portal.service.validation;

import io.micrometer.common.util.StringUtils;
import org.sofumar.portal.constants.FieldConstants;
import org.sofumar.portal.data.vo.PaymentVO;
import org.sofumar.portal.framework.exception.ValidationException;
import org.sofumar.portal.framework.util.LabelUtils;
import org.springframework.stereotype.Service;

import static org.sofumar.portal.constants.MessagesConstants.REQUIRED_FIELD;

@Service
public class PaymentValidator {

    public void validate(PaymentVO vo) throws ValidationException {
        validateMember(vo);
        validateFeeType(vo);
        validateAmount(vo);
        validateDateReceived(vo);
        validateMethodOfPayment(vo);
        validatePeriodLogic(vo);

        if (vo.hasErrors()) {
            throw new ValidationException(vo);
        }
    }

    public void validateForUpdate(PaymentVO vo) throws ValidationException {
        if (vo.getPaymentID() == null) {
            vo.addFieldMessage(FieldConstants.PAYMENT_ID,
                    REQUIRED_FIELD.addMessageArgs(LabelUtils.toLabel(FieldConstants.PAYMENT_ID)));
        }
        validate(vo);
    }

    private void validateMember(PaymentVO vo) {
        if (vo.getMember() == null || vo.getMember().getMemberID() == null) {
            vo.addFieldMessage(FieldConstants.MEMBER_ID,
                    REQUIRED_FIELD.addMessageArgs(LabelUtils.toLabel(FieldConstants.MEMBER_ID)));
        }
    }

    private void validateFeeType(PaymentVO vo) {
        if (StringUtils.isBlank(vo.getFeeType())) {
            vo.addFieldMessage(FieldConstants.FEE_TYPE,
                    REQUIRED_FIELD.addMessageArgs(LabelUtils.toLabel(FieldConstants.FEE_TYPE)));
        }
    }

    private void validateAmount(PaymentVO vo) {
        if (vo.getAmount() == null) {
            vo.addFieldMessage(FieldConstants.AMOUNT,
                    REQUIRED_FIELD.addMessageArgs(LabelUtils.toLabel(FieldConstants.AMOUNT)));
        }
    }

    private void validateDateReceived(PaymentVO vo) {
        if (vo.getDateReceived() == null) {
            vo.addFieldMessage(FieldConstants.DATE_RECEIVED,
                    REQUIRED_FIELD.addMessageArgs(LabelUtils.toLabel(FieldConstants.DATE_RECEIVED)));
        }
    }

    private void validateMethodOfPayment(PaymentVO vo) {
        if (StringUtils.isBlank(vo.getMethodOfPayment())) {
            vo.addFieldMessage(FieldConstants.METHOD_OF_PAYMENT,
                    REQUIRED_FIELD.addMessageArgs(LabelUtils.toLabel(FieldConstants.METHOD_OF_PAYMENT)));
        }
    }

    private void validatePeriodLogic(PaymentVO vo) {
        if ("Membership Fee".equalsIgnoreCase(vo.getFeeType())) {
            if (vo.getYear() == null) {
                vo.addFieldMessage(FieldConstants.YEAR,
                        REQUIRED_FIELD.addMessageArgs(LabelUtils.toLabel(FieldConstants.YEAR)));
            }
            if (vo.getQuarter() == null) {
                vo.addFieldMessage(FieldConstants.QUARTER,
                        REQUIRED_FIELD.addMessageArgs(LabelUtils.toLabel(FieldConstants.QUARTER)));
            }
        }
    }
}