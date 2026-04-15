package io.carizmi.domain.finance.data.transformer;

import io.carizmi.domain.finance.data.dto.ExpenseDto;
import io.carizmi.domain.finance.model.ExpenseVO;
import io.carizmi.framework.data.transformer.Transformer;
import org.springframework.stereotype.Service;

@Service
public class ExpenseVOTransformer implements Transformer<ExpenseDto, ExpenseVO> {

    @Override
    public ExpenseVO transform(ExpenseDto dto) {
        if (dto == null)
            return null;
        ExpenseVO vo = new ExpenseVO();
        vo.setExpenseID(dto.getExpenseID());
        vo.setDateOfExpense(dto.getDateOfExpense());
        vo.setCategory(dto.getCategory());
        vo.setDescription(dto.getDescription());
        vo.setAmount(dto.getAmount());
        return vo;
    }

    public ExpenseVO transformForUpdate(ExpenseDto dto, ExpenseVO existing) {
        if (dto == null || existing == null)
            return existing;
        existing.setDateOfExpense(dto.getDateOfExpense());
        existing.setCategory(dto.getCategory());
        existing.setDescription(dto.getDescription());
        existing.setAmount(dto.getAmount());
        return existing;
    }
}