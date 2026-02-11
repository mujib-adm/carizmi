package org.sofumar.portal.core.repo.jpaspec;

import org.sofumar.portal.constants.Role;
import org.sofumar.portal.constants.FieldConstants;
import org.sofumar.portal.core.vo.UserVO;
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
    public static Specification<UserVO> isActive(boolean active) {
        return (root, query, cb) -> cb.equal(root.get(FieldConstants.ACTIVE), active);
    }
}