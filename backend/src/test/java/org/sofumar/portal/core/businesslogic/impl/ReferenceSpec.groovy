package org.sofumar.portal.core.businesslogic.impl

import org.sofumar.portal.data.dto.ReferenceDto
import org.sofumar.portal.data.dto.request.ReferenceSearchRequestDto
import org.sofumar.portal.constants.FieldConstants
import org.sofumar.portal.data.dto.request.SortOrder
import org.sofumar.portal.data.transformer.ReferenceDtoTransformer
import org.sofumar.portal.core.vo.ReferenceVO
import org.sofumar.portal.framework.data.response.PagedResult
import org.sofumar.portal.framework.exception.RecordNotFoundException
import org.sofumar.portal.core.repo.ReferenceRepository
import org.sofumar.portal.testbase.BaseSpecification
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification as JpaSpecification
import spock.lang.Subject
import spock.lang.Unroll

class ReferenceSpec extends BaseSpecification {

    ReferenceRepository referenceRepo = Mock()
    ReferenceDtoTransformer dtoTransformer = Mock()

    @Subject
    ReferenceImpl referenceImpl = new ReferenceImpl(referenceRepo, dtoTransformer)

    def "test - getReference: Success"() {
        given: "An existing reference VO and DTO"
        Integer id = 1
        ReferenceVO vo = new ReferenceVO(referenceID: id)
        ReferenceDto dto = new ReferenceDto(referenceID: id)

        when: "The target method executed"
        ReferenceDto result = referenceImpl.getReference(id)

        then: "The expected calls are made"
        1 * referenceRepo.findById(1) >> Optional.of(vo)
        1 * dtoTransformer.transform(vo) >> dto
        0 * _

        and: "The expected result"
        result == dto
        noExceptionThrown()
    }

    def "test - getReference: Not Found"() {
        given: "A missing reference ID"
        Integer id = 99

        when: "The target method executed"
        referenceImpl.getReference(id)

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
        Optional<ReferenceVO> result = referenceImpl.findByNameAndCode(name, code)

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
        referenceImpl.getReferencesByName(name)

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

        ReferenceSearchRequestDto request = new ReferenceSearchRequestDto(referenceName: name)
        request.setPage(0)
        request.setSize(10)
        request.setSortField(FieldConstants.REFERENCE_CODE)
        request.setSortOrder(SortOrder.asc)

        when: "The target method executed"
        PagedResult<ReferenceDto> result = referenceImpl.searchReferences(request)

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
        result != null
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
        desc         | name   || expectedFilters                 | expectedValues
        "Name only"  | "Name" || [FieldConstants.REFERENCE_NAME] | ["Name"]
        "No filters" | null   || []                              | []
    }

    def "test - isValidReference - happy path"() {
        given: "Reference criteria"
        String name = "Name"
        String code = "Code"
        JpaSpecification capturedSpec = null

        when: "The target method executed"
        boolean result = referenceImpl.isValidReference(name, code)

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
        boolean result = referenceImpl.isValidReference(name, code)

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