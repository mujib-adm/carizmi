package io.carizmi.domain.membership.validation

import io.carizmi.domain.membership.model.MemberMembershipPeriodVO
import io.carizmi.shared.constants.FieldConstants
import io.carizmi.testbase.BaseSpecification
import spock.lang.Subject

import java.time.LocalDate

class MemberMembershipPeriodValidatorSpec extends BaseSpecification {

    @Subject
    MemberMembershipPeriodValidator validator = new MemberMembershipPeriodValidator()

    def "test - validate: Passes for valid VO"() {
        given: "A valid MemberMembershipPeriodVO"
        MemberMembershipPeriodVO vo = MemberMembershipPeriodVO.builder()
                .memberID(10)
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .build()

        when:
        validator.validate(vo)

        then:
        0 * _

        and:
        !vo.hasErrors()
        noExceptionThrown()
    }

    def "test - validate: Fails when memberID is null"() {
        given: "A MemberMembershipPeriodVO with null memberID"
        MemberMembershipPeriodVO vo = MemberMembershipPeriodVO.builder()
                .memberID(null)
                .startDate(LocalDate.of(2026, 1, 1))
                .build()

        when: "Validated"
        validator.validate(vo)

        then:
        0 * _

        and:
        vo.hasErrors()
        vo.getFieldMessages().containsKey(FieldConstants.MEMBER_ID)
        noExceptionThrown()
    }

    def "test - validate: Fails when startDate is null"() {
        given: "A MemberMembershipPeriodVO with null startDate"
        MemberMembershipPeriodVO vo = MemberMembershipPeriodVO.builder()
                .memberID(10)
                .startDate(null)
                .build()

        when: "Validated"
        validator.validate(vo)

        then:
        0 * _

        and:
        vo.hasErrors()
        vo.getFieldMessages().containsKey(FieldConstants.START_DATE)
        noExceptionThrown()
    }

    def "test - validate: Fails when endDate is before startDate"() {
        given: "A MemberMembershipPeriodVO with endDate before startDate"
        MemberMembershipPeriodVO vo = MemberMembershipPeriodVO.builder()
                .memberID(10)
                .startDate(LocalDate.of(2026, 6, 30))
                .endDate(LocalDate.of(2026, 1, 1))
                .build()

        when: "Validated"
        validator.validate(vo)

        then:
        0 * _

        and:
        vo.hasErrors()
        vo.getFieldMessages().containsKey(FieldConstants.END_DATE)
        noExceptionThrown()
    }

    def "test - validateForUpdate: Fails when periodID is null"() {
        given: "A MemberMembershipPeriodVO with null periodID"
        MemberMembershipPeriodVO vo = MemberMembershipPeriodVO.builder()
                .periodID(null)
                .memberID(10)
                .startDate(LocalDate.of(2026, 1, 1))
                .build()

        when:
        validator.validateForUpdate(vo)

        then:
        0 * _

        and:
        vo.hasErrors()
        vo.getFieldMessages().containsKey(FieldConstants.PERIOD_ID)
        noExceptionThrown()
    }

    def "test - validateForUpdate: Passes with periodID and valid fields"() {
        given: "A valid MemberMembershipPeriodVO with periodID"
        MemberMembershipPeriodVO vo = MemberMembershipPeriodVO.builder()
                .periodID(1)
                .memberID(10)
                .startDate(LocalDate.of(2026, 1, 1))
                .build()

        when:
        validator.validateForUpdate(vo)

        then:
        0 * _

        and:
        !vo.hasErrors()
        noExceptionThrown()
    }
}