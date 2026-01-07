package org.sofumar.portal.data.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExpenseDto {
    private Integer expenseID;
    private LocalDate dateOfExpense;
    private String category;
    private String description;
    private BigDecimal amount;
}