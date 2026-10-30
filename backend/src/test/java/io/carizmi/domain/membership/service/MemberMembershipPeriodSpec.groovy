package io.carizmi.domain.membership.service

import io.carizmi.domain.membership.model.MemberMembershipPeriodVO
import io.carizmi.domain.membership.repository.MemberMembershipPeriodRepository
import io.carizmi.domain.membership.repository.spec.MemberMembershipPeriodSpecifications
import io.carizmi.domain.membership.validation.MemberMembershipPeriodValidator
import io.carizmi.testbase.BaseSpecification
import org.springframework.data.jpa.domain.Specification
import spock.lang.Subject

import java.time.LocalDate

class MemberMembershipPeriodSpec extends BaseSpecification {

    MemberMembershipPeriodRepository repo = Mock()
    MemberMembershipPeriodValidator validator = Mock()

    @Subject
    MemberMembershipPeriodImpl periodService = new MemberMembershipPeriodImpl(repo, validator)

    def "test - findByMemberIDOrderByStartDateAsc delegates to repo"() {
        given:
        Integer memberID = 10
        List<MemberMembershipPeriodVO> periods = [
                MemberMembershipPeriodVO.builder().periodID(1).memberID(10).startDate(LocalDate.of(2024, 1, 1)).build()
        ]

        when:
        List<MemberMembershipPeriodVO> results = periodService.findByMemberIDOrderByStartDateAsc(memberID)

        then:
        1 * repo.findByMemberIDOrderByStartDateAsc(memberID) >> periods
        0 * _

        and:
        results == periods
        noExceptionThrown()
    }

    def "test - findByMemberIDInOrderByStartDateAsc delegates to repo"() {
        given:
        List<Integer> ids = [10, 20]
        List<MemberMembershipPeriodVO> periods = [
                MemberMembershipPeriodVO.builder().periodID(1).memberID(10).startDate(LocalDate.of(2024, 1, 1)).build(),
                MemberMembershipPeriodVO.builder().periodID(2).memberID(20).startDate(LocalDate.of(2025, 1, 1)).build()
        ]

        when:
        List<MemberMembershipPeriodVO> results = periodService.findByMemberIDInOrderByStartDateAsc(ids)

        then:
        1 * repo.findByMemberIDInOrderByStartDateAsc(ids) >> periods
        0 * _

        and:
        results == periods
        noExceptionThrown()
    }

    def "test - findFirstByMemberIDAndEndDateIsNullOrderByStartDateDesc delegates to repo"() {
        given:
        Integer memberID = 10
        MemberMembershipPeriodVO openPeriod = MemberMembershipPeriodVO.builder()
                .periodID(1).memberID(10).startDate(LocalDate.of(2024, 1, 1)).endDate(null).build()

        when:
        Optional<MemberMembershipPeriodVO> resultOpt = periodService.findFirstByMemberIDAndEndDateIsNullOrderByStartDateDesc(memberID)

        then:
        1 * repo.findFirstByMemberIDAndEndDateIsNullOrderByStartDateDesc(memberID) >> Optional.of(openPeriod)
        0 * _

        and:
        resultOpt.isPresent()
        resultOpt.get() == openPeriod
        noExceptionThrown()
    }

    def "test - isMemberActiveInDateRange delegates to repo"() {
        given:
        Integer memberID = 10
        LocalDate start = LocalDate.of(2026, 1, 1)
        LocalDate end = LocalDate.of(2026, 3, 31)

        when:
        boolean active = periodService.isMemberActiveInDateRange(memberID, start, end)

        then:
        1 * repo.isMemberActiveInDateRange(memberID, start, end) >> true
        active
        0 * _

        and:
        noExceptionThrown()
    }

    def "test - isMemberActiveInQuarter delegates to repo"() {
        given:
        Integer memberID = 10
        LocalDate start = LocalDate.of(2026, 4, 1)
        LocalDate end = LocalDate.of(2026, 6, 30)

        when:
        boolean active = periodService.isMemberActiveInQuarter(memberID, 2026, 2)

        then:
        1 * repo.isMemberActiveInDateRange(memberID, start, end) >> true
        0 * _

        and:
        active
        noExceptionThrown()
    }

    def "test - findActiveMemberIDsInDateRange delegates to repo"() {
        given:
        LocalDate start = LocalDate.of(2026, 1, 1)
        LocalDate end = LocalDate.of(2026, 3, 31)

        when:
        List<Integer> ids = periodService.findActiveMemberIDsInDateRange(start, end)

        then:
        1 * repo.findActiveMemberIDsInDateRange(start, end) >> [10, 20]
        0 * _

        and:
        ids == [10, 20]
        noExceptionThrown()
    }

    def "test - exists with specification delegates to repo"() {
        given:
        Specification<MemberMembershipPeriodVO> spec = MemberMembershipPeriodSpecifications.hasMemberID(10)

        when:
        boolean exists = periodService.exists(spec)

        then:
        1 * repo.exists(spec) >> true
        0 * _

        and:
        exists
        noExceptionThrown()
    }

    def "test - add executes AbstractBusinessLogic lifecycle"() {
        given:
        MemberMembershipPeriodVO vo = MemberMembershipPeriodVO.builder()
                .memberID(10).startDate(LocalDate.of(2026, 1, 1)).build()

        when:
        MemberMembershipPeriodVO added = periodService.add(vo)

        then:
        1 * validator.validate(vo)
        1 * repo.save(vo) >> vo
        0 * _

        and:
        added == vo
        noExceptionThrown()
    }

    def "test - update executes AbstractBusinessLogic lifecycle"() {
        given:
        MemberMembershipPeriodVO vo = MemberMembershipPeriodVO.builder()
                .periodID(1).memberID(10).startDate(LocalDate.of(2026, 1, 1)).endDate(LocalDate.of(2026, 6, 30)).build()

        when:
        MemberMembershipPeriodVO updated = periodService.update(vo)

        then:
        1 * validator.validateForUpdate(vo)
        1 * repo.save(vo) >> vo
        0 * _

        and:
        updated == vo
        noExceptionThrown()
    }

    def "test - delete and batch delete execute AbstractBusinessLogic lifecycle"() {
        given:
        MemberMembershipPeriodVO vo = MemberMembershipPeriodVO.builder()
                .periodID(1).memberID(10).startDate(LocalDate.of(2026, 1, 1)).build()

        when:
        periodService.delete(vo)
        periodService.delete([vo])

        then:
        1 * validator.validateForDelete(vo)
        1 * repo.delete(vo)
        1 * validator.validateForDelete(vo)
        1 * repo.deleteAll([vo])
        0 * _

        and:
        noExceptionThrown()
    }
}