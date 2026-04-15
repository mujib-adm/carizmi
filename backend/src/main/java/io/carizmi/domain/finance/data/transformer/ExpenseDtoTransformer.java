package io.carizmi.domain.finance.data.transformer;

import io.carizmi.domain.finance.data.dto.ExpenseDto;
import io.carizmi.domain.finance.model.ExpenseVO;
import io.carizmi.framework.data.transformer.Transformer;
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