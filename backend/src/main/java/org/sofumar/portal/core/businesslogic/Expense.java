package org.sofumar.portal.core.businesslogic;

import java.time.LocalDate;
import java.util.List;

import org.sofumar.portal.data.dto.ExpenseDto;
import org.sofumar.portal.data.dto.request.ExpenseSearchRequestDto;
import org.sofumar.portal.core.vo.ExpenseVO;
import org.sofumar.portal.framework.bl.BusinessLogic;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;

import java.math.BigDecimal;

public interface Expense extends BusinessLogic<ExpenseVO> {

    ResponseEntity<GlobalResponse<Integer>> addExpense(ExpenseDto requestDto);

    ResponseEntity<GlobalResponse<Void>> updateExpense(ExpenseDto requestDto);

    ResponseEntity<GlobalResponse<Void>> deleteExpense(@NonNull Integer expenseID);

    ResponseEntity<GlobalResponse<ExpenseDto>> getExpense(@NonNull Integer expenseID);

    ResponseEntity<GlobalResponse<List<ExpenseDto>>> searchExpenses(ExpenseSearchRequestDto request);

    BigDecimal sumAmountByDateOfExpenseBetween(LocalDate start, LocalDate end);

    BigDecimal sumAmountByDateOfExpenseBefore(LocalDate date);
}