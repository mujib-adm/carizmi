package io.carizmi.domain.membership.repository;

import io.carizmi.domain.membership.model.MemberVO;
import io.carizmi.shared.data.dto.MemberJoinDateProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberRepository extends JpaRepository<MemberVO, Integer>, JpaSpecificationExecutor<MemberVO> {

    @Query("SELECT m.memberID as memberID, m.joinDate as joinDate FROM MemberVO m WHERE m.status = :status")
    List<MemberJoinDateProjection> findJoinDatesByStatus(@Param("status") String status);
}