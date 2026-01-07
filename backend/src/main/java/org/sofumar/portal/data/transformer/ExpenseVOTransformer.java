package org.sofumar.portal.data.transformer;

import org.sofumar.portal.data.dto.ExpenseDto;
import org.sofumar.portal.data.vo.ExpenseVO;
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
        if (dto == null)
            return existing;
        existing.setDateOfExpense(dto.getDateOfExpense());
        existing.setCategory(dto.getCategory());
        existing.setDescription(dto.getDescription());
        existing.setAmount(dto.getAmount());
        return existing;
    }
}