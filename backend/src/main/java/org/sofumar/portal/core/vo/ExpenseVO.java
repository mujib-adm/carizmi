package org.sofumar.portal.core.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.sofumar.portal.constants.FieldConstants;
import org.sofumar.portal.constants.TableConstants;
import org.sofumar.portal.framework.vo.ValueObject;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = TableConstants.EXPENSE_TABLE)
public class ExpenseVO extends ValueObject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = FieldConstants.EXPENSE_ID)
    private Integer expenseID;

    @NotNull
    @Column(name = FieldConstants.DATE_OF_EXPENSE, nullable = false)
    private LocalDate dateOfExpense;

    @NotBlank
    @Column(name = FieldConstants.CATEGORY, nullable = false)
    private String category;

    @NotBlank
    @Column(name = FieldConstants.DESCRIPTION, nullable = false)
    private String description;

    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 12, fraction = 2)
    @Column(name = FieldConstants.AMOUNT, nullable = false)
    private BigDecimal amount;

    @Override
    public String getTableName() {
        return TableConstants.EXPENSE_TABLE;
    }
}