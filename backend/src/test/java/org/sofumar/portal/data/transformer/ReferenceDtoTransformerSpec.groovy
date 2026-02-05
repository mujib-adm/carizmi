package org.sofumar.portal.data.transformer

import org.sofumar.portal.core.vo.ReferenceVO
import org.sofumar.portal.data.dto.ReferenceDto
import org.sofumar.portal.data.dto.response.ReferenceDataDto
import org.sofumar.portal.testsupport.BaseSpecification

class ReferenceDtoTransformerSpec extends BaseSpecification {

    ReferenceDtoTransformer transformer = new ReferenceDtoTransformer()

    def "test - transform: Should transform ReferenceVO to ReferenceDto"() {
        given: "TestData setup"
        ReferenceVO vo = new ReferenceVO(
                referenceID: 1,
                referenceName: "FEE_TYPE",
                referenceCode: "MEMBERSHIP",
                referenceDisplay: "Membership Fee",
                active: true
        )

        when: "The target method executed"
        ReferenceDto result = transformer.transform(vo)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result != null
        result.referenceID == vo.referenceID
        result.referenceName == vo.referenceName
        result.referenceCode == vo.referenceCode
        result.referenceDisplay == vo.referenceDisplay
        result.active == vo.active
        noExceptionThrown()
    }

    def "test - transform: Should return null when input is null"() {
        when: "The target method executed"
        ReferenceDto result = transformer.transform(null)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result == null
        noExceptionThrown()
    }

    def "test - transformData: Should transform ReferenceVO to ReferenceDataDto"() {
        given: "TestData setup"
        ReferenceVO vo = new ReferenceVO(
                referenceCode: "CASH",
                referenceDisplay: "Cash Payment"
        )

        when: "The target method executed"
        ReferenceDataDto result = transformer.transformData(vo)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result != null
        result.referenceCode == vo.referenceCode
        result.referenceDisplay == vo.referenceDisplay
        noExceptionThrown()
    }

    def "test - transformData: Should return null when input is null"() {
        when: "The target method executed"
        ReferenceDataDto result = transformer.transformData(null)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result == null
        noExceptionThrown()
    }

    def "test - transformDataList: Should transform list of ReferenceVOs to ReferenceDataDtos"() {
        given: "TestData setup"
        List<ReferenceVO> list = [new ReferenceVO(referenceCode: "A"), null, new ReferenceVO(referenceCode: "B")]

        when: "The target method executed"
        List<ReferenceDataDto> result = transformer.transformDataList(list)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result.size() == 2
        result[0].referenceCode == "A"
        result[1].referenceCode == "B"
        noExceptionThrown()
    }

    def "test - transformDataList: Should return empty list when input is null"() {
        when: "The target method executed"
        List<ReferenceDataDto> result = transformer.transformDataList(null)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result != null
        result.isEmpty()
        noExceptionThrown()
    }

    def "test - transformList: Should transform list of ReferenceVOs"() {
        given: "TestData setup"
        ReferenceVO vo1 = new ReferenceVO(referenceID: 1, referenceCode: "A")
        ReferenceVO vo2 = new ReferenceVO(referenceID: 2, referenceCode: "B")
        List<ReferenceVO> list = [vo1, null, vo2]

        when: "The target method executed"
        List<ReferenceDto> result = transformer.transformList(list)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result.size() == 2
        result[0].referenceID == 1
        result[1].referenceID == 2
        noExceptionThrown()
    }

    def "test - transformList: Should return empty list when input is null"() {
        when: "The target method executed"
        List<ReferenceDto> result = transformer.transformList(null)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result != null
        result.isEmpty()
        noExceptionThrown()
    }
}