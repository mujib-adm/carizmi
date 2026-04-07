package org.sofumar.portal.controller

import org.sofumar.portal.core.businesslogic.Reference
import org.sofumar.portal.data.dto.ReferenceDto
import org.sofumar.portal.data.dto.request.ReferenceSearchRequestDto
import org.sofumar.portal.data.dto.response.ReferenceDescDto
import org.sofumar.portal.framework.data.response.GlobalResponse
import org.sofumar.portal.framework.data.response.PagedResult
import org.sofumar.portal.framework.data.response.PaginationMeta
import org.sofumar.portal.testbase.BaseSpecification
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Subject

class ReferenceControllerSpec extends BaseSpecification {

    Reference referenceService = Mock()

    @Subject
    ReferenceController referenceController = new ReferenceController(referenceService)

    def "test - getReference: Should delegate to reference service and wrap result"() {
        given: "A reference ID"
        Integer referenceID = 1
        ReferenceDto dto = new ReferenceDto(referenceID: referenceID)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<ReferenceDto>> result = referenceController.getReference(referenceID)

        then: "The expected calls are made"
        1 * referenceService.getReference(referenceID) >> dto
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.OK
        result.body.responseData.referenceID == referenceID
        noExceptionThrown()
    }

    def "test - searchReferences: Should delegate to reference service and wrap result"() {
        given: "A search request"
        ReferenceSearchRequestDto request = new ReferenceSearchRequestDto()
        List<ReferenceDto> dtoList = [new ReferenceDto(referenceID: 1)]
        PaginationMeta meta = PaginationMeta.of(0, 10, 1, 1)
        PagedResult<ReferenceDto> pagedResult = PagedResult.of(dtoList, meta)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<List<ReferenceDto>>> result = referenceController.searchReferences(request)

        then: "The expected calls are made"
        1 * referenceService.searchReferences(request) >> pagedResult
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.OK
        result.body.responseData.size() == 1
        result.body.responseData[0].referenceID == 1
        noExceptionThrown()
    }

    def "test - getReferencesByName: Should delegate to reference service and wrap result"() {
        given: "A reference name"
        String referenceName = "feeType"
        List<ReferenceDescDto> dtoList = [new ReferenceDescDto(referenceCode: "F1")]

        when: "The target method executed"
        ResponseEntity<GlobalResponse<List<ReferenceDescDto>>> result = referenceController.getReferencesByName(referenceName)

        then: "The expected calls are made"
        1 * referenceService.getReferencesByName(referenceName) >> dtoList
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.OK
        result.body.responseData.size() == 1
        result.body.responseData[0].referenceCode == "F1"
        noExceptionThrown()
    }
}