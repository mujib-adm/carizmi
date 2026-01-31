package org.sofumar.portal.core.repo;

import org.sofumar.portal.core.vo.UserVO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserVO, Integer>, JpaSpecificationExecutor<UserVO> {
}