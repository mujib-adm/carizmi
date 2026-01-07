package org.sofumar.portal.controller;

import java.time.LocalDate;
import java.util.List;

import org.sofumar.portal.data.dto.ExpenseDto;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.sofumar.portal.service.businesslogic.ExpenseService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/expenses")
@Tag(name = "Expenses", description = "Expense management APIs")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping("/add")
    @Operation(summary = "Add a new expense")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<GlobalResponse<Integer>> addExpense(@RequestBody ExpenseDto requestDto) {
        return expenseService.addExpense(requestDto);
    }

    @PutMapping("/update")
    @Operation(summary = "Update an existing expense")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<GlobalResponse<Void>> updateExpense(@RequestBody ExpenseDto requestDto) {
        return expenseService.updateExpense(requestDto);
    }

    @DeleteMapping("/delete/{expenseID}")
    @Operation(summary = "Delete expense by ID")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<GlobalResponse<Void>> deleteExpense(@PathVariable Integer expenseID) {
        return expenseService.deleteExpense(expenseID);
    }

    @GetMapping("/get/{expenseID}")
    @Operation(summary = "Get expense by ID")
    public ResponseEntity<GlobalResponse<ExpenseDto>> getExpense(@PathVariable Integer expenseID) {
        return expenseService.getExpense(expenseID);
    }

    @GetMapping("/search")
    @Operation(summary = "Search expenses")
    public ResponseEntity<GlobalResponse<List<ExpenseDto>>> searchExpenses(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortOrder) {
        return expenseService.searchExpenses(category, dateFrom, dateTo, page, size, sortField, sortOrder);
    }
}