package org.sofumar.portal.controller;

import java.time.LocalDate;
import java.util.List;

import org.sofumar.portal.data.dto.MemberDto;
import org.sofumar.portal.data.dto.MemberLookupDto;
import org.sofumar.portal.data.dto.MemberSummaryDto;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.sofumar.portal.core.businesslogic.Member;
import org.springframework.format.annotation.DateTimeFormat;
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

    @GetMapping("/search")
    @Operation(summary = "Search members")
    public ResponseEntity<GlobalResponse<List<MemberDto>>> searchMembers(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String zip,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate joinDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate joinDateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortOrder) {
        return member.searchMembers(firstName, lastName, phone, email, status, city, state, zip, joinDateFrom, joinDateTo, page, size, sortField, sortOrder);
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
