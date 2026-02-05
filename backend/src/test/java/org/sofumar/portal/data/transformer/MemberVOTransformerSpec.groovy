package org.sofumar.portal.data.transformer

import org.sofumar.portal.core.vo.MemberVO
import org.sofumar.portal.data.dto.MemberDto
import org.sofumar.portal.testsupport.BaseSpecification

import java.time.LocalDate

class MemberVOTransformerSpec extends BaseSpecification {

    MemberVOTransformer transformer = new MemberVOTransformer()

    def "test - transform: Should transform MemberDto to MemberVO"() {
        given: "TestData setup"
        MemberDto dto = new MemberDto(
                firstName: "Jane",
                lastName: "Smith",
                phone: "0987654321",
                email: "jane@example.com",
                status: "PENDING",
                joinDate: LocalDate.of(2025, 2, 1),
                address1: "456 Oak Ave",
                city: "Metropolis",
                state: "NY",
                zip: "10001"
        )

        when: "The target method executed"
        MemberVO result = transformer.transform(dto)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result != null
        result.firstName == dto.firstName
        result.lastName == dto.lastName
        result.phone == dto.phone
        result.email == dto.email
        result.status == dto.status
        result.joinDate == dto.joinDate
        result.address1 == dto.address1
        result.address2 == dto.address2
        result.city == dto.city
        result.state == dto.state
        result.zip == dto.zip
        noExceptionThrown()
    }

    def "test - transform: Should return null when input is null"() {
        when: "The target method executed"
        MemberVO result = transformer.transform(null)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result == null
        noExceptionThrown()
    }

    def "test - transformForUpdate: Should update existing MemberVO from DTO"() {
        given: "TestData setup"
        MemberVO existing = new MemberVO(memberID: 1L, firstName: "OldName")
        MemberDto dto = new MemberDto(
                firstName: "UpdatedName",
                lastName: "UpdatedLast",
                phone: "5555555555"
        )

        when: "The target method executed"
        MemberVO result = transformer.transformForUpdate(dto, existing)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result != null
        result == existing
        result.firstName == dto.firstName
        result.lastName == dto.lastName
        result.phone == dto.phone
        noExceptionThrown()
    }

    def "test - transformForUpdate: Should return existing when DTO is null"() {
        given: "TestData setup"
        MemberVO existing = new MemberVO(memberID: 1L, firstName: "OldName")

        when: "The target method executed"
        MemberVO result = transformer.transformForUpdate(null, existing)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result == existing
        noExceptionThrown()
    }

    def "test - transformForUpdate: Should return null when existing is null"() {
        given: "TestData setup"
        MemberDto dto = new MemberDto(firstName: "NewName")

        when: "The target method executed"
        MemberVO result = transformer.transformForUpdate(dto, null)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result == null
        noExceptionThrown()
    }

    def "test - transformList: Should transform list of MemberDtos"() {
        given: "TestData setup"
        MemberDto dto1 = new MemberDto(firstName: "John")
        MemberDto dto2 = new MemberDto(firstName: "Jane")
        List<MemberDto> list = [dto1, null, dto2]

        when: "The target method executed"
        List<MemberVO> result = transformer.transformList(list)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result.size() == 2
        result[0].firstName == "John"
        result[1].firstName == "Jane"
        noExceptionThrown()
    }

    def "test - transformList: Should return empty list when input is null"() {
        when: "The target method executed"
        List<MemberVO> result = transformer.transformList(null)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result != null
        result.isEmpty()
        noExceptionThrown()
    }
}