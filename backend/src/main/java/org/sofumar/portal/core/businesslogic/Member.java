package org.sofumar.portal.core.businesslogic;

import java.util.List;

import org.sofumar.portal.data.dto.MemberDto;
import org.sofumar.portal.data.dto.response.MemberJoinDateProjection;
import org.sofumar.portal.data.dto.response.MemberLookupDto;
import org.sofumar.portal.data.dto.response.MemberSummaryDto;
import org.sofumar.portal.core.vo.MemberVO;
import org.sofumar.portal.framework.bl.BusinessLogic;
import org.sofumar.portal.framework.data.response.PagedResult;
import org.sofumar.portal.data.dto.request.MemberSearchRequestDto;
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