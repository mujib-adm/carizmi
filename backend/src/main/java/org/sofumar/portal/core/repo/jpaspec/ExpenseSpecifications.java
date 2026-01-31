package org.sofumar.portal.core.repo.jpaspec;

import java.time.LocalDate;

import org.sofumar.portal.constants.FieldConstants;
import org.sofumar.portal.core.vo.ExpenseVO;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;

public class ExpenseSpecifications {

    @NonNull
    public static Specification<ExpenseVO> hasCategory(String category) {
        return (root, query, cb) -> category == null ? null : cb.equal(root.get(FieldConstants.CATEGORY), category);
    }

    @NonNull
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