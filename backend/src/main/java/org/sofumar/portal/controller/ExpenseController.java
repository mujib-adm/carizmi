package org.sofumar.portal.controller;

import java.util.List;

import org.sofumar.portal.data.dto.ExpenseDto;
import org.sofumar.portal.data.dto.request.ExpenseSearchRequestDto;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.sofumar.portal.core.businesslogic.Expense;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/expenses")
@Tag(name = "Expenses", description = "Expense management APIs")
@RequiredArgsConstructor
public class ExpenseController {

    private final Expense expense;

    @PostMapping("/add")
    @Operation(summary = "Add a new expense")
    @PreAuthorize("hasRole(T(org.sofumar.portal.constants.RoleConstants).ROLE_ADMIN) or hasRole(T(org.sofumar.portal.constants.RoleConstants).ROLE_MANAGER)")
    public ResponseEntity<GlobalResponse<Integer>> addExpense(@RequestBody ExpenseDto requestDto) {
        return expense.addExpense(requestDto);
    }

    @PutMapping("/update")
    @Operation(summary = "Update an existing expense")
    @PreAuthorize("hasRole(T(org.sofumar.portal.constants.RoleConstants).ROLE_ADMIN) or hasRole(T(org.sofumar.portal.constants.RoleConstants).ROLE_MANAGER)")
    public ResponseEntity<GlobalResponse<Void>> updateExpense(@RequestBody ExpenseDto requestDto) {
        return expense.updateExpense(requestDto);
    }

    @DeleteMapping("/delete/{expenseID}")
    @Operation(summary = "Delete expense by ID")
    @PreAuthorize("hasRole(T(org.sofumar.portal.constants.RoleConstants).ROLE_ADMIN) or hasRole(T(org.sofumar.portal.constants.RoleConstants).ROLE_MANAGER)")
    public ResponseEntity<GlobalResponse<Void>> deleteExpense(@PathVariable Integer expenseID) {
        return expense.deleteExpense(expenseID);
    }

    @GetMapping("/get/{expenseID}")
    @Operation(summary = "Get expense by ID")
    public ResponseEntity<GlobalResponse<ExpenseDto>> getExpense(@PathVariable Integer expenseID) {
        return expense.getExpense(expenseID);
    }

    @PostMapping("/search")
    @Operation(summary = "Search expenses")
    public ResponseEntity<GlobalResponse<List<ExpenseDto>>> searchExpenses(@RequestBody ExpenseSearchRequestDto request) {
        return expense.searchExpenses(request);
    }
}