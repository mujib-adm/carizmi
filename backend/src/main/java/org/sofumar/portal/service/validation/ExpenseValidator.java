package org.sofumar.portal.service.validation;

import org.apache.commons.lang3.StringUtils;
import org.sofumar.portal.constants.FieldConstants;
import org.sofumar.portal.constants.ReferenceCodeConstants;
import org.sofumar.portal.core.vo.ExpenseVO;
import org.sofumar.portal.framework.exception.ValidationException;
import org.sofumar.portal.framework.util.LabelUtils;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import static org.sofumar.portal.constants.MessagesConstants.REQUIRED_FIELD;

@Service
@RequiredArgsConstructor
public class ExpenseValidator {

    private final ReferenceValidator referenceValidator;

    public void validate(ExpenseVO vo) throws ValidationException {
        validateDateOfExpense(vo);
        validateCategory(vo);
        validateDescription(vo);
        validateAmount(vo);

        if (vo.hasErrors()) {
            throw new ValidationException(vo);
        }
    }

    public void validateForUpdate(ExpenseVO vo) throws ValidationException {
        if (vo.getExpenseID() == null) {
            vo.addFieldMessage(FieldConstants.EXPENSE_ID, REQUIRED_FIELD.addMessageArgs(LabelUtils.toLabel(FieldConstants.EXPENSE_ID)));
        }
        validate(vo);
    }

    private void validateDateOfExpense(ExpenseVO vo) {
        if (vo.getDateOfExpense() == null) {
            vo.addFieldMessage(FieldConstants.DATE_OF_EXPENSE, REQUIRED_FIELD.addMessageArgs(LabelUtils.toLabel(FieldConstants.DATE_OF_EXPENSE)));
        }
    }

    private void validateCategory(ExpenseVO vo) {
        if (StringUtils.isBlank(vo.getCategory())) {
            vo.addFieldMessage(FieldConstants.CATEGORY, REQUIRED_FIELD.addMessageArgs(LabelUtils.toLabel(FieldConstants.CATEGORY)));
        } else {
            referenceValidator.validate(vo, FieldConstants.CATEGORY, ReferenceCodeConstants.EXPENSE_CATEGORY.NAME, vo.getCategory());
        }
    }

    private void validateDescription(ExpenseVO vo) {
        if (StringUtils.isBlank(vo.getDescription())) {
            vo.addFieldMessage(FieldConstants.DESCRIPTION, REQUIRED_FIELD.addMessageArgs(LabelUtils.toLabel(FieldConstants.DESCRIPTION)));
        }
    }

    private void validateAmount(ExpenseVO vo) {
        if (vo.getAmount() == null) {
            vo.addFieldMessage(FieldConstants.AMOUNT, REQUIRED_FIELD.addMessageArgs(LabelUtils.toLabel(FieldConstants.AMOUNT)));
        }
    }
}