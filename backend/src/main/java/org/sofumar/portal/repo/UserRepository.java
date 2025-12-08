package org.sofumar.portal.repo;

import org.sofumar.portal.data.vo.UserVO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserVO, Integer>, JpaSpecificationExecutor<UserVO> {
}