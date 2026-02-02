package org.sofumar.portal.controller;

import java.util.List;

import org.sofumar.portal.data.dto.MemberDto;
import org.sofumar.portal.data.dto.response.MemberLookupDto;
import org.sofumar.portal.data.dto.response.MemberSummaryDto;
import org.sofumar.portal.data.dto.request.MemberSearchRequestDto;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.sofumar.portal.core.businesslogic.Member;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/members")
@Tag(name = "Members", description = "Member management APIs")
@RequiredArgsConstructor
public class MemberController {

    private final Member member;

    @PostMapping("/add")
    @Operation(summary = "Add a new member")
    @PreAuthorize("hasRole(T(org.sofumar.portal.constants.RoleConstants).ROLE_ADMIN) or hasRole(T(org.sofumar.portal.constants.RoleConstants).ROLE_MANAGER)")
    public ResponseEntity<GlobalResponse<Integer>> addMember(@RequestBody MemberDto requestDto) {
        return member.addMember(requestDto);
    }

    @PutMapping("/update")
    @Operation(summary = "Update an existing member")
    @PreAuthorize("hasRole(T(org.sofumar.portal.constants.RoleConstants).ROLE_ADMIN) or hasRole(T(org.sofumar.portal.constants.RoleConstants).ROLE_MANAGER)")
    public ResponseEntity<GlobalResponse<Void>> updateMember(@RequestBody MemberDto requestDto) {
        return member.updateMember(requestDto);
    }

    @DeleteMapping("delete/{memberID}")
    @Operation(summary = "Delete member by ID")
    @PreAuthorize("hasRole(T(org.sofumar.portal.constants.RoleConstants).ROLE_ADMIN) or hasRole(T(org.sofumar.portal.constants.RoleConstants).ROLE_MANAGER)")
    public ResponseEntity<GlobalResponse<Void>> deleteMember(@PathVariable Integer memberID) {
        return member.deleteMember(memberID);
    }

    @GetMapping("get/{memberID}")
    @Operation(summary = "Get member by ID")
    public ResponseEntity<GlobalResponse<MemberDto>> getMember(@PathVariable Integer memberID) {
        return member.getMember(memberID);
    }

    @PostMapping("/search")
    @Operation(summary = "Search members")
    public ResponseEntity<GlobalResponse<List<MemberDto>>> searchMembers(
            @RequestBody MemberSearchRequestDto request) {
        return member.searchMembers(request);
    }

    @GetMapping("/lookup")
    @Operation(summary = "Lookup members by name or ID (fuzzy search)")
    public ResponseEntity<GlobalResponse<List<MemberLookupDto>>> lookupMembers(
            @RequestParam String query) {
        return member.lookupMembers(query);
    }

    @GetMapping("/{memberID}/summary")
    @Operation(summary = "Get financial summary for a member")
    public ResponseEntity<GlobalResponse<MemberSummaryDto>> getMemberSummary(@PathVariable Integer memberID) {
        return member.getMemberSummary(memberID);
    }
}
