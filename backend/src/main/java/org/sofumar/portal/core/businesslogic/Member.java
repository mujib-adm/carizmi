package org.sofumar.portal.core.businesslogic;

import java.util.List;

import org.sofumar.portal.data.dto.MemberDto;
import org.sofumar.portal.data.dto.response.MemberLookupDto;
import org.sofumar.portal.data.dto.response.MemberSummaryDto;
import org.sofumar.portal.core.vo.MemberVO;
import org.sofumar.portal.framework.bl.BusinessLogic;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.sofumar.portal.data.dto.request.MemberSearchRequestDto;
import org.springframework.lang.NonNull;
import org.springframework.http.ResponseEntity;

public interface Member extends BusinessLogic<MemberVO> {

    ResponseEntity<GlobalResponse<Integer>> addMember(MemberDto requestDto);

    ResponseEntity<GlobalResponse<Void>> updateMember(MemberDto requestDto);

    ResponseEntity<GlobalResponse<Void>> deleteMember(Integer memberID);

    ResponseEntity<GlobalResponse<MemberDto>> getMember(Integer memberID);

    ResponseEntity<GlobalResponse<List<MemberDto>>> searchMembers(MemberSearchRequestDto request);

    ResponseEntity<GlobalResponse<List<MemberLookupDto>>> lookupMembers(String query);

    ResponseEntity<GlobalResponse<MemberSummaryDto>> getMemberSummary(@NonNull Integer memberID);

    long countActiveMembers();

    List<MemberVO> findAllActiveMembers();

}