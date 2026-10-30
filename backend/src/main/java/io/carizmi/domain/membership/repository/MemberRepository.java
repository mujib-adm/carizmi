package io.carizmi.domain.membership.repository;

import io.carizmi.domain.membership.model.MemberVO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends JpaRepository<MemberVO, Integer>, JpaSpecificationExecutor<MemberVO> {
}