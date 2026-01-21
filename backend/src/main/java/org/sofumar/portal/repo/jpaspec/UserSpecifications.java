package org.sofumar.portal.repo.jpaspec;

import org.sofumar.portal.constants.FieldConstants;
import org.sofumar.portal.data.vo.UserVO;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecifications {
    public static Specification<UserVO> hasUsername(String username) {
        return (root, query, cb) -> cb.equal(root.get(FieldConstants.USERNAME), username);
    }

    public static Specification<UserVO> hasRole(String role) {
        return (root, query, cb) -> cb.equal(root.get(FieldConstants.ROLE), role);
    }

    public static Specification<UserVO> isActive(boolean active) {
        return (root, query, cb) -> cb.equal(root.get(FieldConstants.ACTIVE), active);
    }
}