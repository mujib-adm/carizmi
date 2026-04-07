package org.sofumar.portal.controller;

import java.util.List;

import org.sofumar.portal.data.dto.ExpenseDto;
import org.sofumar.portal.data.dto.request.ExpenseSearchRequestDto;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.sofumar.portal.framework.data.response.PagedResult;
import org.sofumar.portal.framework.util.ResponseUtils;
import org.sofumar.portal.core.businesslogic.Expense;
import org.sofumar.portal.security.annotation.IsAuthenticated;
import org.sofumar.portal.security.annotation.IsAdminOrManager;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import static org.sofumar.portal.message.ValidationMessages.*;

@RestController
@RequestMapping("/expenses")
@Tag(name = "Expenses", description = "Expense management APIs")
@RequiredArgsConstructor
public class ExpenseController {

    private final Expense expense;

    @PostMapping("/add")
    @Operation(summary = "Add a new expense")
    @IsAdminOrManager
    public ResponseEntity<GlobalResponse<Integer>> addExpense(@Valid @RequestBody ExpenseDto requestDto) {
        Integer id = expense.addExpense(requestDto);
        return ResponseUtils.okWithData(id, RECORD_ADDED.addMessageArgs("Expense").getMessageString());
    }

    @PutMapping("/update")
    @Operation(summary = "Update an existing expense")
    @IsAdminOrManager
    public ResponseEntity<GlobalResponse<Void>> updateExpense(@Valid @RequestBody ExpenseDto requestDto) {
        expense.updateExpense(requestDto);
        return ResponseUtils.ok(RECORD_UPDATED.addMessageArgs("Expense").getMessageString());
    }

    @DeleteMapping("/delete/{expenseID}")
    @Operation(summary = "Delete expense by ID")
    @IsAdminOrManager
    public ResponseEntity<GlobalResponse<Void>> deleteExpense(@PathVariable @NonNull Integer expenseID) {
        expense.deleteExpense(expenseID);
        return ResponseUtils.ok(RECORD_DELETED.addMessageArgs("Expense").getMessageString());
    }

    @GetMapping("/get/{expenseID}")
    @Operation(summary = "Get expense by ID")
    @IsAuthenticated
    public ResponseEntity<GlobalResponse<ExpenseDto>> getExpense(@PathVariable @NonNull Integer expenseID) {
        return ResponseUtils.okWithData(expense.getExpense(expenseID));
    }

    @PostMapping("/search")
    @Operation(summary = "Search expenses")
    @IsAuthenticated
    public ResponseEntity<GlobalResponse<List<ExpenseDto>>> searchExpenses(@RequestBody ExpenseSearchRequestDto request) {
        PagedResult<ExpenseDto> result = expense.searchExpenses(request);
        return ResponseUtils.okWithDataPageable(result.items(), result.meta());
    }
}