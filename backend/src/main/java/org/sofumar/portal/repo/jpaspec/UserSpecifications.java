package org.sofumar.portal.repo.jpaspec;

import org.sofumar.portal.data.vo.UserVO;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecifications {
    public static Specification<UserVO> hasUsername(String username) {
        return (root, query, cb) -> cb.equal(root.get("username"), username);
    }
}
