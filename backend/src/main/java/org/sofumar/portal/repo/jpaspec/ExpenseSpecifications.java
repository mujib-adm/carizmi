package org.sofumar.portal.repo.jpaspec;

import java.time.LocalDate;

import org.sofumar.portal.constants.FieldConstants;
import org.sofumar.portal.data.vo.ExpenseVO;
import org.springframework.data.jpa.domain.Specification;

public class ExpenseSpecifications {

    public static Specification<ExpenseVO> hasCategory(String category) {
        return (root, query, cb) -> category == null ? null : cb.equal(root.get(FieldConstants.CATEGORY), category);
    }

    public static Specification<ExpenseVO> dateOfExpenseBetween(LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            if (from == null && to == null)
                return null;
            if (from != null && to != null)
                return cb.between(root.get(FieldConstants.DATE_OF_EXPENSE), from, to);
            if (from != null)
                return cb.greaterThanOrEqualTo(root.get(FieldConstants.DATE_OF_EXPENSE), from);
            return cb.lessThanOrEqualTo(root.get(FieldConstants.DATE_OF_EXPENSE), to);
        };
    }
}