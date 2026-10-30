package io.carizmi.domain.membership.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import io.carizmi.domain.membership.data.dto.MemberDto;
import io.carizmi.domain.membership.data.dto.response.MemberLookupDto;
import io.carizmi.domain.membership.data.dto.response.MemberSummaryDto;
import io.carizmi.domain.membership.model.MemberMembershipPeriodVO;
import io.carizmi.domain.membership.model.MemberVO;
import io.carizmi.framework.bl.BusinessLogic;
import io.carizmi.framework.data.response.PagedResult;
import io.carizmi.domain.membership.data.dto.request.MemberSearchRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;

public interface Member extends BusinessLogic<MemberVO> {

    Integer addMember(MemberDto requestDto);

    void updateMember(MemberDto requestDto);

    void deleteMember(Integer memberID);

    MemberDto getMember(Integer memberID);

    PagedResult<MemberDto> searchMembers(MemberSearchRequestDto request);

    List<MemberLookupDto> lookupMembers(String query);

    MemberSummaryDto getMemberSummary(@NonNull Integer memberID);

    long countActiveMembers();

    List<MemberVO> findAllActiveMembers();

    Page<MemberVO> findActiveMembers(@NonNull Pageable pageable);

    Page<MemberVO> findMembersActiveInYear(int year, @NonNull Pageable pageable);

    List<MemberVO> findAllMembersActiveInYear(int year);

    List<MemberMembershipPeriodVO> findMembershipPeriods(@NonNull Integer memberID);

    Map<Integer, List<MemberMembershipPeriodVO>> findMembershipPeriodsForMembers(@NonNull Collection<Integer> memberIDs);

    long countActiveMembersInDateRange(@NonNull LocalDate start, @NonNull LocalDate end);

    int deactivateDelinquentMembers(int year);

    void activateMemberOnRegistrationFee(@NonNull Integer memberID, @NonNull LocalDate dateReceived);

    void updateMemberJoinDateOnRegistrationFee(@NonNull Integer memberID, @NonNull LocalDate dateReceived);

}