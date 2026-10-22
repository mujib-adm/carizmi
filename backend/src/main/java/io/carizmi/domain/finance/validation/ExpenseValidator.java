package io.carizmi.domain.finance.validation;

import lombok.RequiredArgsConstructor;
import io.carizmi.domain.platform.constants.BaselineConstants;
import io.carizmi.domain.platform.service.SystemSetting;
import org.apache.commons.lang3.StringUtils;
import io.carizmi.shared.constants.FieldConstants;
import io.carizmi.shared.constants.ReferenceConstants;
import io.carizmi.shared.message.ValidationMessages;
import io.carizmi.domain.finance.model.ExpenseVO;
import io.carizmi.framework.bl.AbstractDomainValidator;
import io.carizmi.domain.platform.validation.ReferenceValidator;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExpenseValidator extends AbstractDomainValidator<ExpenseVO> {

    private final ReferenceValidator referenceValidator;
    private final SystemSetting systemSetting;

    @Override
    public void validate(ExpenseVO vo) {
        validateDateOfExpense(vo);
        validateCategory(vo);
        validateDescription(vo);
        validateAmount(vo);
    }

    @Override
    public void validateForUpdate(ExpenseVO vo) {
        validateRequired(vo, FieldConstants.EXPENSE_ID, vo.getExpenseID());
        validate(vo);
    }

    private void validateDateOfExpense(ExpenseVO vo) {
        validateRequired(vo, FieldConstants.DATE_OF_EXPENSE, vo.getDateOfExpense());
        if (vo.getDateOfExpense() != null) {
            int expenseYear = vo.getDateOfExpense().getYear();
            String closingKey = BaselineConstants.getYearlyBaselineKey(expenseYear + 1);
            if (systemSetting.findBySettingKey(closingKey).isPresent()) {
                vo.addFieldMessage(FieldConstants.DATE_OF_EXPENSE,
                        ValidationMessages.ERR_FISCAL_YEAR_CLOSED.addMessageArgs(String.valueOf(expenseYear)));
            }
        }
    }

    private void validateCategory(ExpenseVO vo) {
        if (StringUtils.isBlank(vo.getCategory())) {
            validateRequired(vo, FieldConstants.CATEGORY, vo.getCategory());
        } else {
            referenceValidator.validate(vo, FieldConstants.CATEGORY, ReferenceConstants.EXPENSE_CATEGORY.NAME, vo.getCategory());
        }
    }

    private void validateDescription(ExpenseVO vo) {
        validateRequired(vo, FieldConstants.DESCRIPTION, vo.getDescription());
    }

    private void validateAmount(ExpenseVO vo) {
        validateRequired(vo, FieldConstants.AMOUNT, vo.getAmount());
    }
}