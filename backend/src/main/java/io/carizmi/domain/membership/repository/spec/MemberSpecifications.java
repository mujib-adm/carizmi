package io.carizmi.domain.membership.repository.spec;

import jakarta.persistence.criteria.Predicate;
import io.carizmi.shared.constants.FieldConstants;
import io.carizmi.shared.constants.ReferenceConstants;
import io.carizmi.domain.membership.model.MemberVO;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import io.carizmi.shared.util.PhoneUtils;

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
                return null;
            }

            String term = query.trim();
            // 1. Define the Global Filter (Active Only)
            Predicate isActive = cb.equal(root.get(FieldConstants.STATUS), ReferenceConstants.MEMBER_STATUS.ACTIVE);
            List<Predicate> searchPredicates = new ArrayList<>();

            // 2. Phone Strategy: If the query matches a valid 10-digit US phone number format
            Optional<PhoneUtils.PhoneParts> phoneParts = PhoneUtils.parse(term);
            if (phoneParts.isPresent()) {
                PhoneUtils.PhoneParts parts = phoneParts.get();
                // Add exact equality predicates across standard representations for optimal SQL index utilization
                for (String candidate : parts.allFormats()) {
                    searchPredicates.add(cb.equal(root.get(FieldConstants.PHONE), candidate));
                }
            } else if (term.matches("\\d{4,}")) {
                // 3. Member ID Strategy: Exact ID match if numeric with 4 digits minimum
                try {
                    searchPredicates.add(cb.equal(root.get(FieldConstants.MEMBER_ID), Integer.valueOf(term)));
                } catch (NumberFormatException ignored) {
                    // Overflow beyond Integer.MAX_VALUE: cannot be a valid member_id,
                    // leaving searchPredicates empty to return disjunction (1=0) without DB scan
                }
            } else if (term.matches(".*[a-zA-Z].*")) {
                // 4. Name Strategy: Fuzzy match first name and last name
                String lowerTerm = term.toLowerCase();
                String pattern = "%" + lowerTerm + "%";
                searchPredicates.add(cb.like(cb.lower(root.get(FieldConstants.FIRST_NAME)), pattern));
                searchPredicates.add(cb.like(cb.lower(root.get(FieldConstants.LAST_NAME)), pattern));

                if (lowerTerm.contains(" ")) {
                    String[] parts = lowerTerm.split("\\s+");
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

            // 5. If no search strategy matched (e.g. invalid query such as numbers with < 4 digits),
            // return a false disjunction (SQL: status = '01' AND 1=0) to safely return zero records
            // immediately without scanning the database.
            if (searchPredicates.isEmpty()) {
                return cb.and(isActive, cb.disjunction());
            }

            // 6. Combine: Active AND ( Search1 OR Search2 ... )
            return cb.and(isActive, cb.or(searchPredicates.toArray(new Predicate[0])));
        };
    }
}