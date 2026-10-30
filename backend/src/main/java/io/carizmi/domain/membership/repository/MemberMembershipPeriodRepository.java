package io.carizmi.domain.membership.repository;

import io.carizmi.domain.membership.model.MemberMembershipPeriodVO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberMembershipPeriodRepository extends JpaRepository<MemberMembershipPeriodVO, Integer>, JpaSpecificationExecutor<MemberMembershipPeriodVO> {

    List<MemberMembershipPeriodVO> findByMemberIDOrderByStartDateAsc(Integer memberID);

    List<MemberMembershipPeriodVO> findByMemberIDInOrderByStartDateAsc(Collection<Integer> memberIDs);

    Optional<MemberMembershipPeriodVO> findFirstByMemberIDAndEndDateIsNullOrderByStartDateDesc(Integer memberID);

    @Query("""
            SELECT COUNT(p) > 0 FROM MemberMembershipPeriodVO p
            WHERE p.memberID = :memberID
              AND p.startDate <= :rangeEnd
              AND (p.endDate IS NULL OR p.endDate >= :rangeStart)
            """)
    boolean isMemberActiveInDateRange(@Param("memberID") Integer memberID,
                                      @Param("rangeStart") LocalDate rangeStart,
                                      @Param("rangeEnd") LocalDate rangeEnd);

    @Query("""
            SELECT DISTINCT p.memberID FROM MemberMembershipPeriodVO p
            WHERE p.startDate <= :rangeEnd
              AND (p.endDate IS NULL OR p.endDate >= :rangeStart)
            """)
    List<Integer> findActiveMemberIDsInDateRange(@Param("rangeStart") LocalDate rangeStart,
                                                @Param("rangeEnd") LocalDate rangeEnd);
}