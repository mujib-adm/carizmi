package org.sofumar.portal.controller

import org.sofumar.portal.core.businesslogic.Member
import org.sofumar.portal.data.dto.MemberDto
import org.sofumar.portal.data.dto.request.MemberSearchRequestDto
import org.sofumar.portal.data.dto.response.MemberLookupDto
import org.sofumar.portal.data.dto.response.MemberSummaryDto
import org.sofumar.portal.framework.data.response.GlobalResponse
import org.sofumar.portal.framework.data.response.PagedResult
import org.sofumar.portal.framework.data.response.PaginationMeta
import org.sofumar.portal.testbase.BaseSpecification
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Subject

class MemberControllerSpec extends BaseSpecification {

    Member memberService = Mock()

    @Subject
    MemberController memberController = new MemberController(memberService)

    def "test - addMember: Should delegate to member service and wrap result"() {
        given: "A member request"
        MemberDto requestDto = new MemberDto(firstName: "John", lastName: "Doe")

        when: "The target method executed"
        ResponseEntity<GlobalResponse<Integer>> result = memberController.addMember(requestDto)

        then: "The expected calls are made"
        1 * memberService.addMember(requestDto) >> 1
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.OK
        result.body.responseData == 1
        noExceptionThrown()
    }

    def "test - updateMember: Should delegate to member service and wrap result"() {
        given: "A member update request"
        MemberDto requestDto = new MemberDto(memberID: 1, firstName: "John", lastName: "Smith")

        when: "The target method executed"
        ResponseEntity<GlobalResponse<Void>> result = memberController.updateMember(requestDto)

        then: "The expected calls are made"
        1 * memberService.updateMember(requestDto)
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.OK
        noExceptionThrown()
    }

    def "test - deleteMember: Should delegate to member service and wrap result"() {
        given: "A member ID"
        Integer memberID = 1

        when: "The target method executed"
        ResponseEntity<GlobalResponse<Void>> result = memberController.deleteMember(memberID)

        then: "The expected calls are made"
        1 * memberService.deleteMember(memberID)
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.OK
        noExceptionThrown()
    }

    def "test - getMember: Should delegate to member service and wrap result"() {
        given: "A member ID"
        Integer memberID = 1
        MemberDto dto = new MemberDto(memberID: memberID)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<MemberDto>> result = memberController.getMember(memberID)

        then: "The expected calls are made"
        1 * memberService.getMember(memberID) >> dto
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.OK
        result.body.responseData.memberID == memberID
        noExceptionThrown()
    }

    def "test - searchMembers: Should delegate to member service and wrap result"() {
        given: "A search request"
        MemberSearchRequestDto request = new MemberSearchRequestDto()
        List<MemberDto> dtoList = [new MemberDto(memberID: 1)]
        PaginationMeta meta = PaginationMeta.of(0, 10, 1, 1)
        PagedResult<MemberDto> pagedResult = PagedResult.of(dtoList, meta)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<List<MemberDto>>> result = memberController.searchMembers(request)

        then: "The expected calls are made"
        1 * memberService.searchMembers(request) >> pagedResult
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.OK
        result.body.responseData.size() == 1
        result.body.responseData[0].memberID == 1
        result.body.meta.totalRecords == 1
        noExceptionThrown()
    }

    def "test - lookupMembers: Should delegate to member service and wrap result"() {
        given: "A query string"
        String query = "John"
        List<MemberLookupDto> dtoList = [new MemberLookupDto(memberID: 1, firstName: query)]

        when: "The target method executed"
        ResponseEntity<GlobalResponse<List<MemberLookupDto>>> result = memberController.lookupMembers(query)

        then: "The expected calls are made"
        1 * memberService.lookupMembers(query) >> dtoList
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.OK
        result.body.responseData.size() == 1
        result.body.responseData[0].firstName == query
        noExceptionThrown()
    }

    def "test - getMemberSummary: Should delegate to member service and wrap result"() {
        given: "A member ID"
        Integer memberID = 1
        MemberSummaryDto summary = new MemberSummaryDto(totalPaid: 1000.0)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<MemberSummaryDto>> result = memberController.getMemberSummary(memberID)

        then: "The expected calls are made"
        1 * memberService.getMemberSummary(memberID) >> summary
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.OK
        result.body.responseData.totalPaid == 1000.0
        noExceptionThrown()
    }
}