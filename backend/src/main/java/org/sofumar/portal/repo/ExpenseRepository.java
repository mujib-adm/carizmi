package org.sofumar.portal.repo;

import org.sofumar.portal.data.vo.ExpenseVO;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<ExpenseVO, Integer> {
//    @Query("select sum(e.amount) from ExpenseVO e where e.dateOfExpense between :from and :to")
//    BigDecimal sumAmountBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}