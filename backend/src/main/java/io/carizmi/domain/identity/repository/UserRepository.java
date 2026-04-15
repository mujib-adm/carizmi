package io.carizmi.domain.identity.repository;

import io.carizmi.domain.identity.model.UserVO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserVO, Integer>, JpaSpecificationExecutor<UserVO> {
}