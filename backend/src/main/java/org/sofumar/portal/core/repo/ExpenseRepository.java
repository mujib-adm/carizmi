package org.sofumar.portal.core.repo;

import org.sofumar.portal.core.vo.ExpenseVO;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ExpenseRepository extends JpaRepository<ExpenseVO, Integer>, JpaSpecificationExecutor<ExpenseVO> {

    @Query("select sum(e.amount) from ExpenseVO e where e.dateOfExpense < :date")
    BigDecimal sumAmountByDateOfExpenseBefore(@Param("date") LocalDate date);

    @Query("select sum(e.amount) from ExpenseVO e where e.dateOfExpense >= :start and e.dateOfExpense <= :end")
    BigDecimal sumAmountByDateOfExpenseBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);
}