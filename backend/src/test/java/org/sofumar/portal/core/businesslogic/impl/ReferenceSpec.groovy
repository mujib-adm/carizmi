package org.sofumar.portal.core.businesslogic.impl

import org.sofumar.portal.data.dto.ReferenceDto
import org.sofumar.portal.constants.FieldConstants
import org.sofumar.portal.data.transformer.ReferenceDtoTransformer
import org.sofumar.portal.core.vo.ReferenceVO
import org.sofumar.portal.framework.data.response.GlobalResponse
import org.sofumar.portal.framework.exception.RecordNotFoundException
import org.sofumar.portal.core.repo.ReferenceRepository
import org.sofumar.portal.testsupport.BaseSpecification
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification as JpaSpecification
import org.springframework.http.ResponseEntity
import spock.lang.Subject
import spock.lang.Unroll

class ReferenceSpec extends BaseSpecification {

    ReferenceRepository referenceRepo = Mock()
    ReferenceDtoTransformer dtoTransformer = Mock()

    @Subject
    ReferenceImpl referenceService = new ReferenceImpl(referenceRepo, dtoTransformer)

    def "test - getReference: Success"() {
        given: "An existing reference VO and DTO"
        Integer id = 1
        ReferenceVO vo = new ReferenceVO(referenceID: id)
        ReferenceDto dto = new ReferenceDto(referenceID: id)
        ResponseEntity<GlobalResponse<ReferenceDto>> response

        when: "The target method executed"
        response = referenceService.getReference(id)

        then: "The expected calls are made"
        1 * referenceRepo.findById(1) >> Optional.of(vo)
        1 * dtoTransformer.transform(vo) >> dto
        0 * _

        and: "The expected result"
        response.body.responseData == dto
        noExceptionThrown()
    }

    def "test - getReference: Not Found"() {
        given: "A missing reference ID"
        Integer id = 99

        when: "The target method executed"
        referenceService.getReference(id)

        then: "The expected calls are made"
        1 * referenceRepo.findById(99) >> Optional.empty()
        0 * _

        and: "The expected result"
        thrown(RecordNotFoundException)
    }

    def "test - findByNameAndCode: Should delegate to repo with correct spec"() {
        given: "Search criteria"
        String name = "Test Name"
        String code = "TEST_CODE"
        ReferenceVO vo = new ReferenceVO(referenceID: 1)
        JpaSpecification capturedSpec

        when: "The target method executed"
        Optional<ReferenceVO> result = referenceService.findByNameAndCode(name, code)

        then: "The expected calls are made"
        1 * referenceRepo.findOne(_ as JpaSpecification) >> { JpaSpecification spec ->
            capturedSpec = spec
            Optional.of(vo)
        }
        0 * _

        and: "The expected result"
        result.isPresent()
        result.get() == vo

        capturedSpec != null
        Map<String, List> inspection = inspectSpecification(capturedSpec)
        inspection.filters.containsAll([FieldConstants.REFERENCE_NAME, FieldConstants.REFERENCE_CODE])
        inspection.values.containsAll([name, code])
    }

    def "test - getReferencesByName: Should return list of reference data"() {
        given: "A reference name"
        String name = "NAME"

        when: "The target method executed"
        referenceService.getReferencesByName(name)

        then: "The expected calls are made"
        1 * referenceRepo.findAll(_ as JpaSpecification, _ as Sort) >> []
        1 * dtoTransformer.transformDataList(_) >> []
        0 * _

        and: "The expected result"
        noExceptionThrown()
    }

    @Unroll
    def "test - searchReferences: Coverage for #desc"() {
        given: "Mock page and search setup"
        Page<ReferenceVO> mockPage = Mock(Page)
        JpaSpecification capturedSpec

        when: "The target method executed"
        referenceService.searchReferences(name, code, active, 0, 10, "code", "ASC")

        then: "The expected calls are made"
        1 * referenceRepo.findAll(_ as JpaSpecification, _ as PageRequest) >> { JpaSpecification spec, PageRequest page ->
            capturedSpec = spec
            return mockPage
        }
        1 * mockPage.toList() >> []
        1 * dtoTransformer.transformList([]) >> []
        // Metadata calls - use wildcards effectively but strictly
        _ * mockPage.getNumber() >> 0
        _ * mockPage.getSize() >> 10
        _ * mockPage.getTotalElements() >> 0
        _ * mockPage.getTotalPages() >> 0

        0 * _

        and: "The expected result"
        noExceptionThrown()
        capturedSpec != null
        Map<String, List> inspection = inspectSpecification(capturedSpec)
        if (expectedFilters) {
            inspection.filters.containsAll(expectedFilters)
            inspection.values.containsAll(expectedValues)
        } else {
            inspection.filters.isEmpty()
        }

        where:
        desc          | name   | code   | active || expectedFilters                                                                       | expectedValues
        "All filters" | "Name" | "Code" | true   || [FieldConstants.REFERENCE_NAME, FieldConstants.REFERENCE_CODE, FieldConstants.ACTIVE] | ["Name", "Code", true]
        "Name only"   | "Name" | null   | null   || [FieldConstants.REFERENCE_NAME]                                                       | ["Name"]
        "Code only"   | null   | "Code" | null   || [FieldConstants.REFERENCE_CODE]                                                       | ["Code"]
        "Active only" | null   | null   | true   || [FieldConstants.ACTIVE]                                                               | [true]
        "No filters"  | null   | null   | null   || []                                                                                    | []
    }

    def "test - isValidReference - happy path"() {
        given: "Reference criteria"
        String name = "Name"
        String code = "Code"
        JpaSpecification capturedSpec = null

        when: "The target method executed"
        boolean result = referenceService.isValidReference(name, code)

        then: "The expected calls are made"
        1 * referenceRepo.exists(_ as JpaSpecification) >> { JpaSpecification spec -> capturedSpec = spec; true }
        0 * _

        and: "The expected result"
        result
        Map<String, List> inspection = inspectSpecification(capturedSpec)
        inspection.filters.containsAll([FieldConstants.REFERENCE_NAME, FieldConstants.REFERENCE_CODE, FieldConstants.ACTIVE])
        inspection.values.containsAll([name, code, true])
        noExceptionThrown()
    }

    def "test - isValidReference - negative path"() {
        given: "Reference criteria"
        String name = "Name"
        String code = "Code"
        JpaSpecification capturedSpec = null

        when: "The target method executed"
        boolean result = referenceService.isValidReference(name, code)

        then: "The expected calls are made"
        1 * referenceRepo.exists(_ as JpaSpecification) >> { JpaSpecification spec -> capturedSpec = spec; false }
        0 * _

        and: "The expected result"
        !result
        Map<String, List> inspection = inspectSpecification(capturedSpec)
        inspection.filters.containsAll([FieldConstants.REFERENCE_NAME, FieldConstants.REFERENCE_CODE, FieldConstants.ACTIVE])
        inspection.values.containsAll([name, code, true])
        noExceptionThrown()
    }

}