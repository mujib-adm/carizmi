package io.carizmi.domain.membership.service

import io.carizmi.framework.event.DomainEventPublisher
import org.mockito.MockedStatic
import org.mockito.Mockito
import io.carizmi.shared.constants.FieldConstants
import io.carizmi.shared.constants.ReferenceConstants
import io.carizmi.domain.finance.service.Payment
import io.carizmi.domain.platform.service.SystemSetting
import io.carizmi.domain.membership.repository.MemberRepository
import io.carizmi.domain.membership.model.MemberVO
import io.carizmi.domain.membership.data.dto.MemberDto
import io.carizmi.domain.membership.data.dto.request.MemberSearchRequestDto
import io.carizmi.domain.membership.data.dto.response.MemberLookupDto
import io.carizmi.domain.membership.data.dto.response.MemberSummaryDto
import io.carizmi.shared.data.dto.PaymentSummary
import io.carizmi.domain.membership.data.transformer.MemberDtoTransformer
import io.carizmi.domain.membership.data.transformer.MemberVOTransformer
import io.carizmi.framework.data.response.PagedResult
import io.carizmi.framework.exception.DuplicateRecordException
import io.carizmi.framework.exception.RecordNotFoundException
import io.carizmi.framework.util.MySQLConstraintResolver
import io.carizmi.domain.membership.validation.MemberValidator
import io.carizmi.testbase.BaseSpecification
import org.springframework.dao.DataAccessException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification as JpaSpecification
import org.springframework.test.util.ReflectionTestUtils
import spock.lang.Subject
import spock.lang.Unroll

import java.time.LocalDate

class MemberSpec extends BaseSpecification {

    static final BigDecimal QUARTERLY_FEE = 60

    MemberRepository memberRepo = Mock()
    Payment payment = Mock()
    SystemSetting systemSetting = Mock()
    MemberVOTransformer voTransformer = Mock()
    MemberDtoTransformer dtoTransformer = Mock()
    MemberValidator validator = Mock()
    MySQLConstraintResolver constraintResolver = Mock()
    DomainEventPublisher domainEventPublisher = Mock()

    @Subject
    MemberImpl memberImpl = new MemberImpl(memberRepo, payment, systemSetting, voTransformer, dtoTransformer, validator)

    void setup() {
        ReflectionTestUtils.setField(memberImpl, "constraintResolver", constraintResolver)
        ReflectionTestUtils.setField(memberImpl, "domainEventPublisher", domainEventPublisher)
        systemSetting.getQuarterlyFeeAmount() >> new BigDecimal("60")
    }

    def "test - addMember: Should transform, validate, and save member"() {
        given: "A member DTO and capture variables"
        String firstName = "John"
        Integer id = 1
        MemberDto requestDto = new MemberDto(firstName: firstName)
        MemberVO transformedVo = new MemberVO(firstName: firstName)
        MemberVO savedVo = new MemberVO(memberID: id, firstName: firstName)
        MemberDto capturedDto = null
        MemberVO capturedVo = null

        when: "The target method executed"
        Integer result = memberImpl.addMember(requestDto)

        then: "The expected calls are made"
        1 * voTransformer.transform(_) >> { MemberDto dto -> capturedDto = dto; transformedVo }
        1 * validator.validate(transformedVo)
        1 * memberRepo.save(_) >> { MemberVO vo -> capturedVo = vo; savedVo }
        1 * domainEventPublisher.publish("CREATED", savedVo, id)
        0 * _

        and: "The expected result"
        result == 1
        capturedDto == requestDto
        capturedVo == transformedVo
        noExceptionThrown()
    }

    def "test - addMember: Duplicate Handling"() {
        given: "A duplicate member scenario"
        String firstName = "J"
        MemberVO vo = new MemberVO(firstName: firstName)
        MemberDto requestDto = new MemberDto()

        when: "The target method executed"
        memberImpl.addMember(requestDto)

        then: "The expected calls are made"
        1 * voTransformer.transform(_) >> vo
        1 * validator.validate(_)
        1 * memberRepo.save(_) >> { throw new DataIntegrityViolationException("Dup", new RuntimeException("Duplicate entry '1' for key 'PRIMARY'")) }
        1 * constraintResolver.resolveFields(_) >> ["memberID"]
        0 * _

        and: "The expected result"
        thrown(DuplicateRecordException)
    }

    def "test - addMember: General DB Error"() {
        given: "A DB error scenario"
        MemberVO vo = new MemberVO(firstName: "J")
        MemberDto requestDto = new MemberDto()

        when: "The target method executed"
        memberImpl.addMember(requestDto)

        then: "The expected calls are made"
        1 * voTransformer.transform(_) >> vo
        1 * validator.validate(_)
        1 * memberRepo.save(_) >> { throw new DataAccessException("DB error", new RuntimeException("root")) {} }
        0 * _

        and: "The expected result"
        vo.hasErrors()
        noExceptionThrown()
    }

    def "test - updateMember: Success"() {
        given: "A valid update request"
        Integer id = 1
        String firstName = "John"
        MemberDto dto = new MemberDto(memberID: id, firstName: firstName)
        MemberVO vo = new MemberVO(memberID: id)

        when: "The target method executed"
        memberImpl.updateMember(dto)

        then: "The expected calls are made"
        1 * memberRepo.findById(1) >> Optional.of(vo)
        1 * voTransformer.transformForUpdate(dto, vo) >> vo
        1 * validator.validateForUpdate(vo)
        1 * memberRepo.save(vo) >> vo
        1 * domainEventPublisher.publish("UPDATED", vo, id)
        0 * _

        and: "The expected result"
        noExceptionThrown()
    }

    def "test - updateMember: DB Error"() {
        given: "A DB error during update"
        Integer id = 1
        MemberDto dto = new MemberDto(memberID: id)
        MemberVO vo = new MemberVO(memberID: id)

        when: "The target method executed"
        memberImpl.updateMember(dto)

        then: "The expected calls are made"
        1 * memberRepo.findById(1) >> Optional.of(vo)
        1 * voTransformer.transformForUpdate(dto, vo) >> vo
        1 * validator.validateForUpdate(vo)
        1 * memberRepo.save(vo) >> { throw new DataAccessException("error", new RuntimeException("root")) {} }
        0 * _

        and: "The expected result"
        vo.hasErrors()
        noExceptionThrown()
    }

    def "test - updateMember: Not Found"() {
        given: "A non-existent member ID"
        Integer id = 99
        MemberDto dto = new MemberDto(memberID: id)

        when: "The target method executed"
        memberImpl.updateMember(dto)

        then: "The expected calls are made"
        1 * memberRepo.findById(99) >> Optional.empty()
        0 * _

        and: "The expected result - RecordNotFoundException is thrown"
        thrown(RecordNotFoundException)
    }

    def "test - updateMember: Missing ID"() {
        given: "An empty member DTO"

        when: "The target method executed"
        memberImpl.updateMember(new MemberDto())

        then: "The expected calls are made"
        1 * memberRepo.findById(null) >> Optional.empty()
        0 * _

        and: "The expected result - RecordNotFoundException is thrown"
        thrown(RecordNotFoundException)
    }

    def "test - deleteMember: Success"() {
        given: "An existing member VO"
        Integer id = 1
        MemberVO vo = new MemberVO(memberID: id)

        when: "The target method executed"
        memberImpl.deleteMember(id)

        then: "The expected calls are made"
        1 * memberRepo.findById(1) >> Optional.of(vo)
        1 * memberRepo.delete(vo)
        1 * domainEventPublisher.publish("DELETED", vo, id)
        0 * _

        and: "The expected result"
        noExceptionThrown()
    }

    def "test - deleteMember: DB Error"() {
        given: "A DB error during deletion"
        Integer id = 1
        MemberVO vo = new MemberVO(memberID: id)

        when: "The target method executed"
        memberImpl.deleteMember(id)

        then: "The expected calls are made"
        1 * memberRepo.findById(1) >> Optional.of(vo)
        1 * memberRepo.delete(vo) >> { throw new DataAccessException("error", new RuntimeException("root")) {} }
        0 * _

        and: "The expected result"
        vo.hasErrors()
        noExceptionThrown()
    }

    def "test - deleteMember: Not Found"() {
        given: "A missing member ID"
        Integer memberID = 0

        when: "The target method executed"
        memberImpl.deleteMember(memberID)

        then: "The expected calls are made"
        1 * memberRepo.findById(memberID) >> Optional.empty()
        0 * _

        and: "The expected result"
        thrown(RecordNotFoundException)
    }

    @Unroll
    def "test - searchMembers: Applying various filter combinations [f: #f, l: #l, p: #p, s: #s]"() {
        given: "Search parameters and mock"
        Page<MemberVO> mockPage = Mock(Page)
        MemberSearchRequestDto request = new MemberSearchRequestDto(firstName: f, lastName: l, phone: p, status: s)
        request.setPage(0)
        request.setSize(10)

        when: "The target method executed"
        PagedResult<MemberDto> result = memberImpl.searchMembers(request)

        then: "The expected calls are made"
        1 * memberRepo.findAll(_, _ as PageRequest) >> mockPage
        1 * mockPage.toList() >> []
        1 * dtoTransformer.transformList(_) >> []
        // Metadata calls - use wildcards effectively but strictly
        _ * mockPage.getNumber() >> 0
        _ * mockPage.getSize() >> 10
        _ * mockPage.getTotalElements() >> 0
        _ * mockPage.getTotalPages() >> 0
        0 * _

        and: "The expected result"
        result != null
        result.items() == []
        noExceptionThrown()

        where:
        f    | l    | p     | s
        "J"  | "D"  | "123" | "ACT"
        null | null | null  | null
    }

    def "test - getMember: Success"() {
        given: "An existing member ID"
        Integer id = 1
        MemberVO vo = new MemberVO(memberID: id)
        MemberDto expectedDto = new MemberDto()

        when: "The target method executed"
        MemberDto result = memberImpl.getMember(id)

        then: "The expected calls are made"
        1 * memberRepo.findById(1) >> Optional.of(vo)
        1 * dtoTransformer.transform(vo) >> expectedDto
        0 * _

        and: "The expected result"
        result == expectedDto
        noExceptionThrown()
    }

    def "test - getMember: Not Found"() {
        given: "A non-existent member ID"
        Integer id = 99

        when: "The target method executed"
        memberImpl.getMember(id)

        then: "The expected calls are made"
        1 * memberRepo.findById(99) >> Optional.empty()
        0 * _

        and: "The expected result"
        thrown(RecordNotFoundException)
    }

    def "test - lookupMembers: Success"() {
        given: "A search query and results"
        String query = "Joh"
        Integer id = 1
        String firstName = "John"
        String lastName = "Doe"
        String phone = "1234567890"
        Page<MemberVO> mockPage = Mock(Page)
        MemberVO m1 = new MemberVO(memberID: id, firstName: firstName, lastName: lastName, phone: phone)

        when: "The target method executed"
        List<MemberLookupDto> result = memberImpl.lookupMembers(query)

        then: "The expected calls are made"
        1 * memberRepo.findAll(_ as JpaSpecification, _ as PageRequest) >> mockPage
        1 * mockPage.getContent() >> [m1]
        0 * _

        and: "The expected result"
        result.size() == 1
        result[0].firstName == firstName
        result[0].phone == phone
        noExceptionThrown()
    }

    @Unroll
    def "test - getMemberSummary: Should calculate correct totals across time contexts [id: #id, found: #found, joinType: #joinType, paidTotal: #paidTotal, paidCurrent: #paidCurrent, paidPast: #paidPast]"() {
        given: "A mocked date and member data using dynamic current year"
        int currentYear = LocalDate.now().year
        LocalDate fixedNow = LocalDate.of(currentYear, 5, 15) // Q2
        LocalDate joinYearStart = LocalDate.of(currentYear, 1, 1)
        LocalDate joinYearQ2Start = LocalDate.of(currentYear, 4, 1)
        LocalDate joinYearNext = LocalDate.of(currentYear + 1, 1, 1)

        LocalDate join = null
        if (joinType == 'START') join = joinYearStart
        else if (joinType == 'Q2') join = joinYearQ2Start
        else if (joinType == 'NEXT') join = joinYearNext

        MockedStatic<LocalDate> localDateMock = Mockito.mockStatic(LocalDate, Mockito.CALLS_REAL_METHODS)
        localDateMock.when(LocalDate::now).thenReturn(fixedNow)

        MemberVO member = new MemberVO(memberID: id, joinDate: join)
        PaymentSummary psQ1 = Mock(PaymentSummary)

        when: "The target method executed"
        MemberSummaryDto result = memberImpl.getMemberSummary(id)

        then: "The expected calls are made"
        1 * memberRepo.findById(id) >> (found ? Optional.of(member) : Optional.empty())
        (found ? 1 : 0) * payment.sumAmountByMemberID(id) >> paidTotal
        (found ? 1 : 0) * payment.sumAmountByMemberIDAndYearAndQuarter(id, currentYear, 2) >> paidCurrent
        (found ? 1 : 0) * payment.findMemberPaymentSummaries(id, ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE) >> (paidPast != null ? [psQ1] : [])
        _ * psQ1.getYear() >> currentYear
        _ * psQ1.getQuarter() >> 1
        _ * psQ1.getTotalPaid() >> paidPast
        (found ? 1 : 0) * systemSetting.getQuarterlyFeeAmount() >> new BigDecimal("60")
        0 * _

        and: "The expected result"
        if (found) {
            result.totalPaid == (paidTotal ?: 0)
            result.outstanding == (BigDecimal) Math.max(0, QUARTERLY_FEE - (paidCurrent ?: 0) as double)
            if (join == null || join.year > currentYear) {
                result.overdue == 0
            } else if (join == joinYearStart) {
                result.overdue == (BigDecimal) Math.max(0, QUARTERLY_FEE - (paidPast ?: 0) as double)
            }
        } else {
            result.totalPaid == 0
        }
        noExceptionThrown()

        cleanup:
        localDateMock.close()

        where:
        id | found | joinType | paidTotal | paidCurrent | paidPast
        1  | true  | 'START'  | 90.00     | 30.00       | 60.00
        2  | true  | 'Q2'     | 0         | 0           | null
        3  | true  | 'NULL'   | 100.00    | 20.00       | null
        4  | false | 'NULL'   | null      | null        | null
        5  | true  | 'NEXT'   | 0         | 0           | null
        6  | true  | 'START'  | 50.00     | 10.00       | 40.00
    }

    @Unroll
    def "test - countActiveMembers: Should delegate to repo with active status criteria [expectedCount: #expectedCount]"() {
        given: "Setup"
        JpaSpecification capturedSpec = null

        when: "The target method executed"
        long result = memberImpl.countActiveMembers()

        then: "The expected calls are made"
        1 * memberRepo.count(_ as JpaSpecification) >> { JpaSpecification spec -> capturedSpec = spec; expectedCount }
        0 * _

        and: "The expected result"
        result == expectedCount
        Map<String, List> inspection = inspectSpecification(capturedSpec)
        inspection.filters.contains(FieldConstants.STATUS)
        inspection.values.contains(ReferenceConstants.MEMBER_STATUS.ACTIVE)
        noExceptionThrown()

        where:
        expectedCount << [0L, 5L, 100L]
    }

    @Unroll
    def "test - findAllActiveMembers: Should delegate to repo with active status criteria [listSize: #expectedList.size()]"() {
        given: "Setup"
        JpaSpecification capturedSpec = null

        when: "The target method executed"
        List<MemberVO> result = memberImpl.findAllActiveMembers()

        then: "The expected calls are made"
        1 * memberRepo.findAll(_ as JpaSpecification) >> { JpaSpecification spec -> capturedSpec = spec; expectedList }
        0 * _

        and: "The expected result"
        result == expectedList
        Map<String, List> inspection = inspectSpecification(capturedSpec)
        inspection.filters.contains(FieldConstants.STATUS)
        inspection.values.contains(ReferenceConstants.MEMBER_STATUS.ACTIVE)
        noExceptionThrown()

        where:
        expectedList << [
                [],
                [new MemberVO(memberID: 1)],
                [new MemberVO(memberID: 1), new MemberVO(memberID: 2)]
        ]
    }

    @Unroll
    def "test - findActiveMembers: Should delegate to repo with active status criteria and pageable [listSize: #expectedList.size()]"() {
        given: "Setup"
        JpaSpecification capturedSpec = null
        Page<MemberVO> mockPage = Mock(Page)
        PageRequest pageable = PageRequest.of(0, 10)

        when: "The target method executed"
        Page<MemberVO> result = memberImpl.findActiveMembers(pageable)

        then: "The expected calls are made"
        1 * memberRepo.findAll(_ as JpaSpecification, pageable) >> { JpaSpecification spec, PageRequest pr ->
            capturedSpec = spec
            mockPage
        }
        0 * _

        and: "The expected result"
        result == mockPage
        Map<String, List> inspection = inspectSpecification(capturedSpec)
        inspection.filters.contains(FieldConstants.STATUS)
        inspection.values.contains(ReferenceConstants.MEMBER_STATUS.ACTIVE)
        noExceptionThrown()

        where:
        expectedList << [
                [],
                [new MemberVO(memberID: 1)],
                [new MemberVO(memberID: 1), new MemberVO(memberID: 2)]
        ]
    }
}