package org.sofumar.portal.controller

import org.sofumar.portal.core.businesslogic.Reference
import org.sofumar.portal.data.dto.ReferenceDto
import org.sofumar.portal.data.dto.request.ReferenceSearchRequestDto
import org.sofumar.portal.data.dto.response.ReferenceDescDto
import org.sofumar.portal.framework.data.response.GlobalResponse
import org.sofumar.portal.testbase.BaseSpecification
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Subject

class ReferenceControllerSpec extends BaseSpecification {

    Reference referenceService = Mock()

    @Subject
    ReferenceController referenceController = new ReferenceController(referenceService)

    def "test - getReference: Should delegate to reference service"() {
        given: "A reference ID"
        Integer referenceID = 1
        ReferenceDto dto = new ReferenceDto(referenceID: referenceID)
        GlobalResponse<ReferenceDto> responseBody = GlobalResponse.withResponseData(dto)
        ResponseEntity<GlobalResponse<ReferenceDto>> expectedResponse = new ResponseEntity<>(responseBody, HttpStatus.OK)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<ReferenceDto>> result = referenceController.getReference(referenceID)

        then: "The expected calls are made"
        1 * referenceService.getReference(referenceID) >> expectedResponse
        0 * _

        and: "The expected result"
        result == expectedResponse
        result.body.responseData.referenceID == referenceID
        noExceptionThrown()
    }

    def "test - searchReferences: Should delegate to reference service"() {
        given: "A search request"
        ReferenceSearchRequestDto request = new ReferenceSearchRequestDto()
        Integer referenceID = 1
        Integer expectedSize = 1
        List<ReferenceDto> dtoList = [new ReferenceDto(referenceID: referenceID)]
        GlobalResponse<List<ReferenceDto>> responseBody = GlobalResponse.withResponseData(dtoList)
        ResponseEntity<GlobalResponse<List<ReferenceDto>>> expectedResponse = new ResponseEntity<>(responseBody, HttpStatus.OK)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<List<ReferenceDto>>> result = referenceController.searchReferences(request)

        then: "The expected calls are made"
        1 * referenceService.searchReferences(request) >> expectedResponse
        0 * _

        and: "The expected result"
        result == expectedResponse
        result.body.responseData.size() == expectedSize
        result.body.responseData[0].referenceID == referenceID
        noExceptionThrown()
    }

    def "test - getReferencesByName: Should delegate to reference service"() {
        given: "A reference name"
        String referenceName = "feeType"
        String referenceCode = "F1"
        Integer expectedSize = 1
        List<ReferenceDescDto> dtoList = [new ReferenceDescDto(referenceCode: referenceCode)]
        GlobalResponse<List<ReferenceDescDto>> responseBody = GlobalResponse.withResponseData(dtoList)
        ResponseEntity<GlobalResponse<List<ReferenceDescDto>>> expectedResponse = new ResponseEntity<>(responseBody, HttpStatus.OK)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<List<ReferenceDescDto>>> result = referenceController.getReferencesByName(referenceName)

        then: "The expected calls are made"
        1 * referenceService.getReferencesByName(referenceName) >> expectedResponse
        0 * _

        and: "The expected result"
        result == expectedResponse
        result.body.responseData.size() == expectedSize
        result.body.responseData[0].referenceCode == referenceCode
        noExceptionThrown()
    }
}