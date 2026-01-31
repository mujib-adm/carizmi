package org.sofumar.portal.core.repo.jpaspec;

import org.sofumar.portal.constants.FieldConstants;
import org.sofumar.portal.core.vo.UserVO;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;

public class UserSpecifications {
    @NonNull
    public static Specification<UserVO> hasUsername(String username) {
        return (root, query, cb) -> cb.equal(root.get(FieldConstants.USERNAME), username);
    }

    @NonNull
    public static Specification<UserVO> hasRole(String role) {
        return (root, query, cb) -> cb.equal(root.get(FieldConstants.ROLE), role);
    }

    @NonNull
    public static Specification<UserVO> isActive(boolean active) {
        return (root, query, cb) -> cb.equal(root.get(FieldConstants.ACTIVE), active);
    }
}