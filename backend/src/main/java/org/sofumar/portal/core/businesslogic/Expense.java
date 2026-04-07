package org.sofumar.portal.core.businesslogic;

import java.time.LocalDate;

import org.sofumar.portal.data.dto.ExpenseDto;
import org.sofumar.portal.data.dto.request.ExpenseSearchRequestDto;
import org.sofumar.portal.core.vo.ExpenseVO;
import org.sofumar.portal.framework.bl.BusinessLogic;
import org.sofumar.portal.framework.data.response.PagedResult;
import org.springframework.lang.NonNull;

import java.math.BigDecimal;

public interface Expense extends BusinessLogic<ExpenseVO> {

    Integer addExpense(ExpenseDto requestDto);

    void updateExpense(ExpenseDto requestDto);

    void deleteExpense(@NonNull Integer expenseID);

    ExpenseDto getExpense(@NonNull Integer expenseID);

    PagedResult<ExpenseDto> searchExpenses(ExpenseSearchRequestDto request);

    BigDecimal sumAmountByDateOfExpenseBetween(LocalDate start, LocalDate end);

    BigDecimal sumAmountByDateOfExpenseBefore(LocalDate date);
}