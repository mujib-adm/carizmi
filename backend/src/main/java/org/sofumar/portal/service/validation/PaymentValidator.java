package org.sofumar.portal.service.validation;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.sofumar.portal.constants.FieldConstants;
import org.sofumar.portal.constants.ReferenceConstants;
import org.sofumar.portal.core.vo.PaymentVO;
import org.sofumar.portal.framework.exception.ValidationException;
import org.sofumar.portal.framework.util.LabelUtils;
import org.springframework.stereotype.Service;

import static org.sofumar.portal.constants.MessagesConstants.REQUIRED_FIELD;

@Service
@RequiredArgsConstructor
public class PaymentValidator {

    private final ReferenceValidator referenceValidator;

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
            vo.addFieldMessage(FieldConstants.PAYMENT_ID, REQUIRED_FIELD.addMessageArgs(LabelUtils.toLabel(FieldConstants.PAYMENT_ID)));
        }
        validate(vo);
    }

    private void validateMember(PaymentVO vo) {
        if (vo.getMember() == null || vo.getMember().getMemberID() == null) {
            vo.addFieldMessage(FieldConstants.MEMBER_ID, REQUIRED_FIELD.addMessageArgs(LabelUtils.toLabel(FieldConstants.MEMBER_ID)));
        }
    }

    private void validateFeeType(PaymentVO vo) {
        if (StringUtils.isBlank(vo.getFeeType())) {
            vo.addFieldMessage(FieldConstants.FEE_TYPE, REQUIRED_FIELD.addMessageArgs(LabelUtils.toLabel(FieldConstants.FEE_TYPE)));
        } else {
            referenceValidator.validate(vo, FieldConstants.FEE_TYPE, ReferenceConstants.FEE_TYPE.NAME, vo.getFeeType());
        }
    }

    private void validateAmount(PaymentVO vo) {
        if (vo.getAmount() == null) {
            vo.addFieldMessage(FieldConstants.AMOUNT, REQUIRED_FIELD.addMessageArgs(LabelUtils.toLabel(FieldConstants.AMOUNT)));
        }
    }

    private void validateDateReceived(PaymentVO vo) {
        if (vo.getDateReceived() == null) {
            vo.addFieldMessage(FieldConstants.DATE_RECEIVED, REQUIRED_FIELD.addMessageArgs(LabelUtils.toLabel(FieldConstants.DATE_RECEIVED)));
        }
    }

    private void validateMethodOfPayment(PaymentVO vo) {
        if (StringUtils.isBlank(vo.getMethodOfPayment())) {
            vo.addFieldMessage(FieldConstants.METHOD_OF_PAYMENT, REQUIRED_FIELD.addMessageArgs(LabelUtils.toLabel(FieldConstants.METHOD_OF_PAYMENT)));
        } else {
            referenceValidator.validate(vo, FieldConstants.METHOD_OF_PAYMENT,
                    ReferenceConstants.PAYMENT_METHOD.NAME,
                    vo.getMethodOfPayment());
        }
    }

    private void validatePeriodLogic(PaymentVO vo) {
        if (ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE.equals(vo.getFeeType())) {
            if (vo.getYear() == null) {
                vo.addFieldMessage(FieldConstants.YEAR, REQUIRED_FIELD.addMessageArgs(LabelUtils.toLabel(FieldConstants.YEAR)));
            }
            if (vo.getQuarter() == null) {
                vo.addFieldMessage(FieldConstants.QUARTER, REQUIRED_FIELD.addMessageArgs(LabelUtils.toLabel(FieldConstants.QUARTER)));
            }
        }
    }
}