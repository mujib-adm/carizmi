package io.carizmi.domain.membership.service;

import java.util.List;

import io.carizmi.domain.membership.data.dto.MemberDto;
import io.carizmi.shared.data.dto.MemberJoinDateProjection;
import io.carizmi.domain.membership.data.dto.response.MemberLookupDto;
import io.carizmi.domain.membership.data.dto.response.MemberSummaryDto;
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

    List<MemberJoinDateProjection> findActiveMemberJoinDates();

}