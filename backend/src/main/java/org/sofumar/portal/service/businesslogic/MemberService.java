package org.sofumar.portal.service.businesslogic;

import org.sofumar.portal.data.dto.MemberDto;
import org.sofumar.portal.data.dto.MemberLookupDto;
import org.sofumar.portal.framework.bl.BusinessLogic;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.sofumar.portal.data.vo.MemberVO;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

public interface MemberService extends BusinessLogic<MemberVO> {

    ResponseEntity<GlobalResponse<Void>> addMember(MemberDto requestDto);

    ResponseEntity<GlobalResponse<Void>> updateMember(MemberDto requestDto);

    ResponseEntity<GlobalResponse<Void>> deleteMember(Integer memberID);

    ResponseEntity<GlobalResponse<MemberDto>> getMember(Integer memberID);

    ResponseEntity<GlobalResponse<List<MemberDto>>> searchMembers(String firstName, String lastName, String phone, String email,
                                                                  String status, String city, String state, String zip,
                                                                  LocalDate joinDateFrom, LocalDate joinDateTo, int page, int size, String sortField, String sortOrder);

    ResponseEntity<GlobalResponse<List<MemberLookupDto>>> lookupMembers(String query);

}