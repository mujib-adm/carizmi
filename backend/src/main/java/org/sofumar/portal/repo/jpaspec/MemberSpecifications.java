package org.sofumar.portal.repo.jpaspec;

import org.sofumar.portal.data.vo.MemberVO;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class MemberSpecifications {

    public static Specification<MemberVO> hasMemberID(Integer memberID) {
        return (root, query, cb) -> memberID == null ? null : cb.equal(root.get("memberID"), memberID);
    }

    public static Specification<MemberVO> hasFirstName(String firstName) {
        return (root, query, cb) -> firstName == null ? null : cb.like(cb.lower(root.get("firstName")), "%" + firstName.toLowerCase() + "%");
    }

    public static Specification<MemberVO> hasLastName(String lastName) {
        return (root, query, cb) -> lastName == null ? null : cb.like(cb.lower(root.get("lastName")), "%" + lastName.toLowerCase() + "%");
    }

    public static Specification<MemberVO> hasPhone(String phone) {
        return (root, query, cb) -> phone == null ? null : cb.equal(root.get("phone"), phone);
    }

    public static Specification<MemberVO> hasEmail(String email) {
        return (root, query, cb) -> email == null ? null : cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%");
    }

    public static Specification<MemberVO> hasStatus(String status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<MemberVO> hasCity(String city) {
        return (root, query, cb) -> city == null ? null : cb.like(cb.lower(root.get("city")), "%" + city.toLowerCase() + "%");
    }

    public static Specification<MemberVO> hasState(String state) {
        return (root, query, cb) -> state == null ? null : cb.equal(root.get("state"), state);
    }

    public static Specification<MemberVO> hasZip(String zip) {
        return (root, query, cb) -> zip == null ? null : cb.equal(root.get("zip"), zip);
    }

    public static Specification<MemberVO> joinDateAfter(LocalDate date) {
        return (root, query, cb) -> date == null ? null : cb.greaterThanOrEqualTo(root.get("joinDate"), date);
    }

    public static Specification<MemberVO> joinDateBefore(LocalDate date) {
        return (root, query, cb) -> date == null ? null : cb.lessThanOrEqualTo(root.get("joinDate"), date);
    }

    public static Specification<MemberVO> joinDateBetween(LocalDate startDate, LocalDate endDate) {
        return (root, query, cb) -> {
            if (startDate == null && endDate == null) {
                return null;
            }
            if (startDate != null && endDate != null) {
                return cb.between(root.get("joinDate"), startDate, endDate);
            }
            if (startDate != null) {
                return cb.greaterThanOrEqualTo(root.get("joinDate"), startDate);
            }
            return cb.lessThanOrEqualTo(root.get("joinDate"), endDate);
        };
    }
}