package org.sofumar.portal.core.businesslogic;

import java.time.LocalDate;
import java.util.List;

import org.sofumar.portal.data.dto.ExpenseDto;
import org.sofumar.portal.core.vo.ExpenseVO;
import org.sofumar.portal.framework.bl.BusinessLogic;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

public interface Expense extends BusinessLogic<ExpenseVO> {

    ResponseEntity<GlobalResponse<Integer>> addExpense(ExpenseDto requestDto);

    ResponseEntity<GlobalResponse<Void>> updateExpense(ExpenseDto requestDto);

    ResponseEntity<GlobalResponse<Void>> deleteExpense(Integer expenseID);

    ResponseEntity<GlobalResponse<ExpenseDto>> getExpense(Integer expenseID);

    ResponseEntity<GlobalResponse<List<ExpenseDto>>> searchExpenses(
            String category, LocalDate dateFrom, LocalDate dateTo,
            int page, int size, String sortField, String sortOrder);

    BigDecimal sumAmountByDateOfExpenseBetween(LocalDate start, LocalDate end);

    BigDecimal sumAmountByDateOfExpenseBefore(LocalDate date);
}