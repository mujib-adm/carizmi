package io.carizmi.domain.membership.data.transformer

import io.carizmi.domain.membership.model.MemberVO
import io.carizmi.domain.membership.data.dto.MemberDto
import io.carizmi.testbase.BaseSpecification

import java.time.LocalDate

class MemberDtoTransformerSpec extends BaseSpecification {

    MemberDtoTransformer transformer = new MemberDtoTransformer()

    def "test - transform: Should transform MemberVO to MemberDto"() {
        given: "TestData setup"
        MemberVO vo = new MemberVO(
                memberID: 1L,
                firstName: "John",
                lastName: "Doe",
                phone: "1234567890",
                email: "john@example.com",
                status: "ACTIVE",
                joinDate: LocalDate.of(2025, 1, 1),
                address1: "123 Main St",
                address2: "Apt 4B",
                city: "Springfield",
                state: "IL",
                zip: "62704"
        )

        when: "The target method executed"
        MemberDto result = transformer.transform(vo)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result != null
        result.memberID == vo.memberID
        result.firstName == vo.firstName
        result.lastName == vo.lastName
        result.phone == vo.phone
        result.email == vo.email
        result.status == vo.status
        result.joinDate == vo.joinDate
        result.address1 == vo.address1
        result.address2 == vo.address2
        result.city == vo.city
        result.state == vo.state
        result.zip == vo.zip
        noExceptionThrown()
    }

    def "test - transform: Should return null when input is null"() {
        when: "The target method executed"
        MemberDto result = transformer.transform(null)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result == null
        noExceptionThrown()
    }

    def "test - transformList: Should transform list of MemberVOs"() {
        given: "TestData setup"
        MemberVO vo1 = new MemberVO(memberID: 1L, firstName: "John")
        MemberVO vo2 = new MemberVO(memberID: 2L, firstName: "Jane")
        List<MemberVO> list = [vo1, null, vo2]

        when: "The target method executed"
        List<MemberDto> result = transformer.transformList(list)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result.size() == 2
        result[0].memberID == 1L
        result[1].memberID == 2L
        noExceptionThrown()
    }

    def "test - transformList: Should return empty list when input is null"() {
        when: "The target method executed"
        List<MemberDto> result = transformer.transformList(null)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result != null
        result.isEmpty()
        noExceptionThrown()
    }
}