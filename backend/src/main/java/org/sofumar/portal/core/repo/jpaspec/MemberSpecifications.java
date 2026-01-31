package org.sofumar.portal.core.repo.jpaspec;

import jakarta.persistence.criteria.Predicate;
import org.sofumar.portal.constants.FieldConstants;
import org.sofumar.portal.constants.ReferenceCodeConstants;
import org.sofumar.portal.core.vo.MemberVO;
import org.springframework.lang.NonNull;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MemberSpecifications {

    @NonNull
    public static Specification<MemberVO> hasStatus(String status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get(FieldConstants.STATUS), status);
    }

    @NonNull
    public static Specification<MemberVO> hasMemberID(Integer memberID) {
        return (root, query, cb) -> memberID == null ? null : cb.equal(root.get(FieldConstants.MEMBER_ID), memberID);
    }

    @NonNull
    public static Specification<MemberVO> hasFirstName(String firstName) {
        return (root, query, cb) -> firstName == null ? null
                : cb.like(cb.lower(root.get(FieldConstants.FIRST_NAME)), "%" + firstName.toLowerCase() + "%");
    }

    @NonNull
    public static Specification<MemberVO> hasLastName(String lastName) {
        return (root, query, cb) -> lastName == null ? null
                : cb.like(cb.lower(root.get(FieldConstants.LAST_NAME)), "%" + lastName.toLowerCase() + "%");
    }

    @NonNull
    public static Specification<MemberVO> hasPhone(String phone) {
        return (root, query, cb) -> phone == null ? null : cb.equal(root.get(FieldConstants.PHONE), phone);
    }

    @NonNull
    public static Specification<MemberVO> hasEmail(String email) {
        return (root, query, cb) -> email == null ? null
                : cb.like(cb.lower(root.get(FieldConstants.EMAIL)), "%" + email.toLowerCase() + "%");
    }

    @NonNull
    public static Specification<MemberVO> hasState(String state) {
        return (root, query, cb) -> state == null ? null : cb.equal(root.get(FieldConstants.STATE), state);
    }

    @NonNull
    public static Specification<MemberVO> joinDateAfter(LocalDate date) {
        return (root, query, cb) -> date == null ? null : cb.greaterThanOrEqualTo(root.get(FieldConstants.JOIN_DATE), date);
    }

    @NonNull
    public static Specification<MemberVO> joinDateBefore(LocalDate date) {
        return (root, query, cb) -> date == null ? null : cb.lessThanOrEqualTo(root.get(FieldConstants.JOIN_DATE), date);
    }

    @NonNull
    public static Specification<MemberVO> joinDateBetween(LocalDate startDate, LocalDate endDate) {
        return (root, query, cb) -> {
            if (startDate == null && endDate == null) {
                return null;
            }
            if (startDate != null && endDate != null) {
                return cb.between(root.get(FieldConstants.JOIN_DATE), startDate, endDate);
            }
            if (startDate != null) {
                return cb.greaterThanOrEqualTo(root.get(FieldConstants.JOIN_DATE), startDate);
            }
            return cb.lessThanOrEqualTo(root.get(FieldConstants.JOIN_DATE), endDate);
        };
    }

    @NonNull
    public static Specification<MemberVO> lookup(String query) {
        return (root, cq, cb) -> {
            if (query == null || query.trim().isEmpty()) {
                return null; // Or return empty predicate depending on need
            }

            String term = query.trim().toLowerCase();
            // 1. Define the Global Filter (Active Only)
            Predicate isActive = cb.equal(root.get(FieldConstants.STATUS), ReferenceCodeConstants.MEMBER_STATUS.ACTIVE);
            List<Predicate> searchPredicates = new ArrayList<>();
            // 2. Numeric Optimization: Exact ID match if numeric
            if (term.matches("\\d+")) {
                try {
                    // Fast path: Exact ID match
                    searchPredicates.add(cb.equal(root.get(FieldConstants.MEMBER_ID), Long.valueOf(term)));
                } catch (NumberFormatException ignored) {
                    // Fallback to string match if number is too large for Long
                    searchPredicates.add(cb.like(cb.toString(root.get(FieldConstants.MEMBER_ID)), "%" + term + "%"));
                }
            } else {
                // 3. String Search: Fuzzy match name
                String pattern = "%" + term + "%";
                searchPredicates.add(cb.like(cb.lower(root.get(FieldConstants.FIRST_NAME)), pattern));
                searchPredicates.add(cb.like(cb.lower(root.get(FieldConstants.LAST_NAME)), pattern));
                // 4. Split Search: "John Smith"
                if (term.contains(" ")) {
                    String[] parts = term.split("\\s+");
                    if (parts.length >= 2) {
                        String p1 = "%" + parts[0] + "%";
                        String p2 = "%" + parts[1] + "%";
                        // Match "First Last" or "Last First"
                        searchPredicates.add(cb.and(
                                cb.like(cb.lower(root.get(FieldConstants.FIRST_NAME)), p1),
                                cb.like(cb.lower(root.get(FieldConstants.LAST_NAME)), p2)
                        ));
                        searchPredicates.add(cb.and(
                                cb.like(cb.lower(root.get(FieldConstants.FIRST_NAME)), p2),
                                cb.like(cb.lower(root.get(FieldConstants.LAST_NAME)), p1)
                        ));
                    }
                }
            }
            // 5. Combine: Active AND ( Search1 OR Search2 ... )
            return cb.and(isActive, cb.or(searchPredicates.toArray(new Predicate[0])));
        };
    }
}