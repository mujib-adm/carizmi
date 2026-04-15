package io.carizmi.domain.finance.service;

import java.time.LocalDate;

import io.carizmi.domain.finance.data.dto.ExpenseDto;
import io.carizmi.domain.finance.data.dto.request.ExpenseSearchRequestDto;
import io.carizmi.domain.finance.model.ExpenseVO;
import io.carizmi.framework.bl.BusinessLogic;
import io.carizmi.framework.data.response.PagedResult;
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