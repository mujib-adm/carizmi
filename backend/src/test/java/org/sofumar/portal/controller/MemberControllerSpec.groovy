package org.sofumar.portal.controller

import org.sofumar.portal.core.businesslogic.Member
import org.sofumar.portal.data.dto.MemberDto
import org.sofumar.portal.data.dto.request.MemberSearchRequestDto
import org.sofumar.portal.data.dto.response.MemberLookupDto
import org.sofumar.portal.data.dto.response.MemberSummaryDto
import org.sofumar.portal.framework.data.response.GlobalResponse
import org.sofumar.portal.testsupport.BaseSpecification
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Subject

class MemberControllerSpec extends BaseSpecification {

    Member memberService = Mock()

    @Subject
    MemberController memberController = new MemberController(memberService)

    def "test - addMember: Should delegate to member service"() {
        given: "A member request"
        String firstName = "John"
        String lastName = "Doe"
        MemberDto requestDto = new MemberDto(firstName: firstName, lastName: lastName)
        Integer memberID = 1
        GlobalResponse<Integer> responseBody = GlobalResponse.withResponseData(memberID)
        ResponseEntity<GlobalResponse<Integer>> expectedResponse = new ResponseEntity<>(responseBody, HttpStatus.OK)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<Integer>> result = memberController.addMember(requestDto)

        then: "The expected calls are made"
        1 * memberService.addMember(requestDto) >> expectedResponse
        0 * _

        and: "The expected result"
        result == expectedResponse
        result.body.responseData == memberID
        noExceptionThrown()
    }

    def "test - updateMember: Should delegate to member service"() {
        given: "A member update request"
        Integer memberID = 1
        String firstName = "John"
        String lastName = "Smith"
        MemberDto requestDto = new MemberDto(memberID: memberID, firstName: firstName, lastName: lastName)
        ResponseEntity<GlobalResponse<Void>> expectedResponse = new ResponseEntity<>(HttpStatus.OK)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<Void>> result = memberController.updateMember(requestDto)

        then: "The expected calls are made"
        1 * memberService.updateMember(requestDto) >> expectedResponse
        0 * _

        and: "The expected result"
        result == expectedResponse
        result.statusCode == HttpStatus.OK
        noExceptionThrown()
    }

    def "test - deleteMember: Should delegate to member service"() {
        given: "A member ID"
        Integer memberID = 1
        ResponseEntity<GlobalResponse<Void>> expectedResponse = new ResponseEntity<>(HttpStatus.OK)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<Void>> result = memberController.deleteMember(memberID)

        then: "The expected calls are made"
        1 * memberService.deleteMember(memberID) >> expectedResponse
        0 * _

        and: "The expected result"
        result == expectedResponse
        result.statusCode == HttpStatus.OK
        noExceptionThrown()
    }

    def "test - getMember: Should delegate to member service"() {
        given: "A member ID"
        Integer memberID = 1
        MemberDto dto = new MemberDto(memberID: memberID)
        GlobalResponse<MemberDto> responseBody = GlobalResponse.withResponseData(dto)
        ResponseEntity<GlobalResponse<MemberDto>> expectedResponse = new ResponseEntity<>(responseBody, HttpStatus.OK)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<MemberDto>> result = memberController.getMember(memberID)

        then: "The expected calls are made"
        1 * memberService.getMember(memberID) >> expectedResponse
        0 * _

        and: "The expected result"
        result == expectedResponse
        result.body.responseData.memberID == memberID
        noExceptionThrown()
    }

    def "test - searchMembers: Should delegate to member service"() {
        given: "A search request"
        MemberSearchRequestDto request = new MemberSearchRequestDto()
        Integer memberID = 1
        Integer expectedSize = 1
        List<MemberDto> dtoList = [new MemberDto(memberID: memberID)]
        GlobalResponse<List<MemberDto>> responseBody = GlobalResponse.withResponseData(dtoList)
        ResponseEntity<GlobalResponse<List<MemberDto>>> expectedResponse = new ResponseEntity<>(responseBody, HttpStatus.OK)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<List<MemberDto>>> result = memberController.searchMembers(request)

        then: "The expected calls are made"
        1 * memberService.searchMembers(request) >> expectedResponse
        0 * _

        and: "The expected result"
        result == expectedResponse
        result.body.responseData.size() == expectedSize
        result.body.responseData[0].memberID == memberID
        noExceptionThrown()
    }

    def "test - lookupMembers: Should delegate to member service"() {
        given: "A query string"
        String query = "John"
        Integer memberID = 1
        Integer expectedSize = 1
        List<MemberLookupDto> dtoList = [new MemberLookupDto(memberID: memberID, firstName: query)]
        GlobalResponse<List<MemberLookupDto>> responseBody = GlobalResponse.withResponseData(dtoList)
        ResponseEntity<GlobalResponse<List<MemberLookupDto>>> expectedResponse = new ResponseEntity<>(responseBody, HttpStatus.OK)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<List<MemberLookupDto>>> result = memberController.lookupMembers(query)

        then: "The expected calls are made"
        1 * memberService.lookupMembers(query) >> expectedResponse
        0 * _

        and: "The expected result"
        result == expectedResponse
        result.body.responseData.size() == expectedSize
        result.body.responseData[0].firstName == query
        noExceptionThrown()
    }

    def "test - getMemberSummary: Should delegate to member service"() {
        given: "A member ID"
        Integer memberID = 1
        BigDecimal paid = 1000.0
        MemberSummaryDto summary = new MemberSummaryDto(totalPaid: paid)
        GlobalResponse<MemberSummaryDto> responseBody = GlobalResponse.withResponseData(summary)
        ResponseEntity<GlobalResponse<MemberSummaryDto>> expectedResponse = new ResponseEntity<>(responseBody, HttpStatus.OK)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<MemberSummaryDto>> result = memberController.getMemberSummary(memberID)

        then: "The expected calls are made"
        1 * memberService.getMemberSummary(memberID) >> expectedResponse
        0 * _

        and: "The expected result"
        result == expectedResponse
        result.body.responseData.totalPaid == paid
        noExceptionThrown()
    }
}