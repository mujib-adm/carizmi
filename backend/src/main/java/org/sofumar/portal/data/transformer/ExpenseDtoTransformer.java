package org.sofumar.portal.data.transformer;

import org.sofumar.portal.data.dto.ExpenseDto;
import org.sofumar.portal.core.vo.ExpenseVO;
import org.springframework.stereotype.Service;

@Service
public class ExpenseDtoTransformer implements Transformer<ExpenseVO, ExpenseDto> {

    @Override
    public ExpenseDto transform(ExpenseVO vo) {
        if (vo == null)
            return null;
        return ExpenseDto.builder()
                .expenseID(vo.getExpenseID())
                .dateOfExpense(vo.getDateOfExpense())
                .category(vo.getCategory())
                .description(vo.getDescription())
                .amount(vo.getAmount())
                .build();
    }
}