package io.carizmi.domain.finance.repository;

import io.carizmi.domain.finance.data.dto.projection.ExpenseCategoryTotalProjection;
import io.carizmi.domain.finance.model.ExpenseVO;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<ExpenseVO, Integer>, JpaSpecificationExecutor<ExpenseVO> {

    @Query("select coalesce(sum(e.amount), 0) from ExpenseVO e where e.dateOfExpense >= :start and e.dateOfExpense <= :end")
    BigDecimal sumAmountByDateOfExpenseBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("select e.category as category, coalesce(sum(e.amount), 0) as totalAmount from ExpenseVO e where e.dateOfExpense >= :start and e.dateOfExpense <= :end group by e.category order by sum(e.amount) desc")
    List<ExpenseCategoryTotalProjection> findExpenseCategoryTotalsBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);
}