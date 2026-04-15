package io.carizmi.domain.identity.repository.spec;

import io.carizmi.shared.constants.Role;
import io.carizmi.shared.constants.FieldConstants;
import io.carizmi.domain.identity.model.UserVO;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;

public class UserSpecifications {
    @NonNull
    public static Specification<UserVO> hasUsername(String username) {
        return (root, query, cb) -> {
            if (username == null) {
                return cb.disjunction();
            }
            return cb.equal(cb.lower(root.get(FieldConstants.USERNAME)), username.toLowerCase());
        };
    }

    @NonNull
    public static Specification<UserVO> hasRole(Role role) {
        return (root, query, cb) -> cb.equal(root.get(FieldConstants.ROLE), role);
    }

    @NonNull
    public static Specification<UserVO> hasEmail(String email) {
        return (root, query, cb) -> {
            if (email == null) {
                return cb.disjunction();
            }
            return cb.equal(cb.lower(root.get(FieldConstants.EMAIL)), email.toLowerCase());
        };
    }

    @NonNull
    public static Specification<UserVO> notUserId(Integer userId) {
        return (root, query, cb) -> {
            if (userId == null) {
                return cb.conjunction();
            }
            return cb.notEqual(root.get(FieldConstants.USER_ID), userId);
        };
    }

    @NonNull
    public static Specification<UserVO> isActive(boolean active) {
        return (root, query, cb) -> cb.equal(root.get(FieldConstants.ACTIVE), active);
    }
}