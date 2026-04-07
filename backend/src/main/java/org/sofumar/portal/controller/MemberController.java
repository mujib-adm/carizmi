package org.sofumar.portal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sofumar.portal.data.dto.MemberDto;
import org.sofumar.portal.data.dto.request.MemberSearchRequestDto;
import org.sofumar.portal.data.dto.response.MemberLookupDto;
import org.sofumar.portal.data.dto.response.MemberSummaryDto;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.sofumar.portal.framework.data.response.PagedResult;
import org.sofumar.portal.framework.util.ResponseUtils;
import org.sofumar.portal.core.businesslogic.Member;
import org.sofumar.portal.security.annotation.IsAuthenticated;
import org.sofumar.portal.security.annotation.IsAdminOrManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.sofumar.portal.message.ValidationMessages.*;

@RestController
@RequestMapping("/members")
@Tag(name = "Members", description = "Member management APIs")
@RequiredArgsConstructor
public class MemberController {

    private final Member member;

    @PostMapping("/add")
    @Operation(summary = "Add a new member")
    @IsAdminOrManager
    public ResponseEntity<GlobalResponse<Integer>> addMember(@Valid @RequestBody MemberDto requestDto) {
        Integer id = member.addMember(requestDto);
        return ResponseUtils.okWithData(id, RECORD_ADDED.addMessageArgs("Member").getMessageString());
    }

    @PutMapping("/update")
    @Operation(summary = "Update an existing member")
    @IsAdminOrManager
    public ResponseEntity<GlobalResponse<Void>> updateMember(@Valid @RequestBody MemberDto requestDto) {
        member.updateMember(requestDto);
        return ResponseUtils.ok(RECORD_UPDATED.addMessageArgs("Member").getMessageString());
    }

    @DeleteMapping("/delete/{memberID}")
    @Operation(summary = "Delete member by ID")
    @IsAdminOrManager
    public ResponseEntity<GlobalResponse<Void>> deleteMember(@PathVariable Integer memberID) {
        member.deleteMember(memberID);
        return ResponseUtils.ok(RECORD_DELETED.addMessageArgs("Member").getMessageString());
    }

    @GetMapping("/get/{memberID}")
    @Operation(summary = "Get member by ID")
    @IsAuthenticated
    public ResponseEntity<GlobalResponse<MemberDto>> getMember(@PathVariable Integer memberID) {
        return ResponseUtils.okWithData(member.getMember(memberID));
    }

    @PostMapping("/search")
    @Operation(summary = "Search members")
    @IsAuthenticated
    public ResponseEntity<GlobalResponse<List<MemberDto>>> searchMembers(@RequestBody MemberSearchRequestDto request) {
        PagedResult<MemberDto> result = member.searchMembers(request);
        return ResponseUtils.okWithDataPageable(result.items(), result.meta());
    }

    @GetMapping("/lookup")
    @Operation(summary = "Lookup members - fuzzy search by first name or last name or ID")
    @IsAuthenticated
    public ResponseEntity<GlobalResponse<List<MemberLookupDto>>> lookupMembers(@RequestParam String query) {
        return ResponseUtils.okWithData(member.lookupMembers(query));
    }

    @GetMapping("/{memberID}/summary")
    @Operation(summary = "Get a membership fee summary (total paid, outstanding, overdue)")
    @IsAuthenticated
    public ResponseEntity<GlobalResponse<MemberSummaryDto>> getMemberSummary(@PathVariable Integer memberID) {
        return ResponseUtils.okWithData(member.getMemberSummary(memberID));
    }
}