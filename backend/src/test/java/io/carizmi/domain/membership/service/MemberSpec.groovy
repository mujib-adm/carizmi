package io.carizmi.domain.membership.service

import io.carizmi.framework.event.DomainEventPublisher
import org.mockito.MockedStatic
import org.mockito.Mockito
import io.carizmi.shared.constants.FieldConstants
import io.carizmi.shared.constants.ReferenceConstants
import io.carizmi.domain.finance.service.Payment
import io.carizmi.domain.platform.service.SystemSetting
import io.carizmi.domain.membership.service.MemberMembershipPeriod
import io.carizmi.domain.membership.model.MemberMembershipPeriodVO
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
import io.carizmi.framework.exception.ValidationException
import io.carizmi.shared.message.ValidationMessages
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
    MemberMembershipPeriod memberMembershipPeriod = Mock()

    @Subject
    MemberImpl memberImpl = new MemberImpl(memberRepo, payment, systemSetting, voTransformer, dtoTransformer, validator, memberMembershipPeriod)

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
        1 * validator.validateForDelete(vo)
        1 * memberRepo.delete(vo)
        1 * domainEventPublisher.publish("DELETED", vo, id)
        0 * _

        and: "The expected result"
        noExceptionThrown()
    }

    def "test - deleteMember: Blocked when member has historical standing"() {
        given: "An existing member with historical standing"
        Integer id = 1
        MemberVO vo = new MemberVO(memberID: id)

        when: "The target method executed"
        memberImpl.deleteMember(id)

        then: "Validator marks error and exception is thrown"
        1 * memberRepo.findById(1) >> Optional.of(vo)
        1 * validator.validateForDelete(vo) >> { MemberVO m -> m.addGlobalMessage(ValidationMessages.ERR_MEMBER_HISTORICAL_STANDING) }
        0 * _

        and:
        thrown(ValidationException)
    }

    def "test - deleteMember: DB Error"() {
        given: "A DB error during deletion"
        Integer id = 1
        MemberVO vo = new MemberVO(memberID: id)

        when: "The target method executed"
        memberImpl.deleteMember(id)

        then: "The expected calls are made"
        1 * memberRepo.findById(1) >> Optional.of(vo)
        1 * validator.validateForDelete(vo)
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
        JpaSpecification<MemberVO> capturedSpec = null
        MemberSearchRequestDto request = new MemberSearchRequestDto(firstName: f, lastName: l, phone: p, status: s)
        request.setPage(0)
        request.setSize(10)

        when: "The target method executed"
        PagedResult<MemberDto> result = memberImpl.searchMembers(request)
        Map<String, List> inspection = inspectSpecification(capturedSpec)

        then: "The expected calls are made"
        1 * memberRepo.findAll(_ as JpaSpecification, _ as PageRequest) >> { JpaSpecification<MemberVO> spec, PageRequest pr ->
            capturedSpec = spec
            return mockPage
        }
        1 * mockPage.toList() >> []
        1 * dtoTransformer.transformList(_) >> []
        // Metadata calls - use wildcards effectively but strictly
        _ * mockPage.getNumber() >> 0
        _ * mockPage.getSize() >> 10
        _ * mockPage.getTotalElements() >> 0
        _ * mockPage.getTotalPages() >> 0
        0 * _

        and: "The expected specification filters and result are verified"
        result != null
        result.items() == []
        inspection.filters == expectedFilters
        if (s) {
            inspection.values.contains(s)
        }
        noExceptionThrown()

        where:
        f    | l    | p     | s                                         || expectedFilters
        "J"  | "D"  | "123" | ReferenceConstants.MEMBER_STATUS.ACTIVE   || [FieldConstants.FIRST_NAME, FieldConstants.LAST_NAME, FieldConstants.PHONE, FieldConstants.STATUS]
        null | null | null  | ReferenceConstants.MEMBER_STATUS.INACTIVE || [FieldConstants.STATUS]
        null | null | null  | ReferenceConstants.MEMBER_STATUS.PENDING  || [FieldConstants.STATUS]
        null | null | null  | null                                      || []
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
        (found ? 1 : 0) * memberMembershipPeriod.findByMemberIDOrderByStartDateAsc(id) >> []
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

    def "test - findMembersActiveInYear: Queries repository with temporal specification and pageable"() {
        given: "Pageable and year"
        PageRequest pageable = PageRequest.of(0, 10)
        Page<MemberVO> mockPage = Mock(Page)

        when: "findMembersActiveInYear is called"
        Page<MemberVO> result = memberImpl.findMembersActiveInYear(2026, pageable)

        then: "Repository is queried"
        1 * memberRepo.findAll(_ as JpaSpecification, pageable) >> mockPage
        0 *_

        and:
        result == mockPage
    }

    def "test - findAllMembersActiveInYear: Queries repository with temporal specification"() {
        given: "Year"
        List<MemberVO> expected = [new MemberVO(memberID: 1)]

        when: "findAllMembersActiveInYear is called"
        List<MemberVO> result = memberImpl.findAllMembersActiveInYear(2026)

        then: "Repository is queried"
        1 * memberRepo.findAll(_ as JpaSpecification) >> expected
        0 * _

        and:
        result == expected
    }

    def "test - addMember: member creates initial membership period"() {
        given:
        Integer id = 10
        LocalDate joinDate = LocalDate.of(2026, 1, 15)
        MemberDto requestDto = new MemberDto(firstName: "Active", lastName: "Member", status: ReferenceConstants.MEMBER_STATUS.ACTIVE, joinDate: joinDate)
        MemberVO transformedVo = new MemberVO(firstName: "Active", lastName: "Member", status: ReferenceConstants.MEMBER_STATUS.ACTIVE, joinDate: joinDate)
        MemberVO savedVo = new MemberVO(memberID: id, firstName: "Pending", lastName: "Member", status: ReferenceConstants.MEMBER_STATUS.PENDING, joinDate: null)

        when: "addMember is executed"
        Integer result = memberImpl.addMember(requestDto)

        then: "Member is systematically saved as PENDING and no period is created"
        1 * voTransformer.transform(_) >> transformedVo
        1 * validator.validate(transformedVo)
        1 * memberRepo.save({ MemberVO vo -> vo.status == ReferenceConstants.MEMBER_STATUS.PENDING && vo.joinDate == null; savedVo }) >> savedVo
        1 * domainEventPublisher.publish("CREATED", savedVo, id)
        0 * memberMembershipPeriod.add(_)
        0 * _

        and:
        result == id
    }

    def "test - updateMember: Transition PENDING to ACTIVE creates membership period"() {
        given: "A member transitioning from PENDING to ACTIVE"
        Integer id = 20
        LocalDate actionDate = LocalDate.of(2026, 3, 1)
        MemberVO existingVo = new MemberVO(memberID: id, status: ReferenceConstants.MEMBER_STATUS.PENDING, joinDate: null)
        MemberDto updateDto = new MemberDto(memberID: id, status: ReferenceConstants.MEMBER_STATUS.ACTIVE, joinDate: actionDate)
        MemberVO updatedVo = new MemberVO(memberID: id, status: ReferenceConstants.MEMBER_STATUS.ACTIVE, joinDate: actionDate)

        when: "updateMember is executed"
        memberImpl.updateMember(updateDto)

        then: "Member is updated and new open period is created"
        1 * memberRepo.findById(id) >> Optional.of(existingVo)
        1 * voTransformer.transformForUpdate(updateDto, existingVo) >> updatedVo
        1 * validator.validateForUpdate(updatedVo)
        1 * memberRepo.save(updatedVo) >> updatedVo
        1 * domainEventPublisher.publish("UPDATED", updatedVo, id)
        1 * memberMembershipPeriod.add({ MemberMembershipPeriodVO p ->
            p.memberID == id && p.startDate == actionDate && p.endDate == null
        })
        0 * _

        and:
        noExceptionThrown()
    }

    def "test - updateMember: Transition ACTIVE to INACTIVE closes existing open period"() {
        given: "An active member transitioning to INACTIVE"
        Integer id = 30
        LocalDate actionDate = LocalDate.of(2026, 6, 30)
        MemberVO existingVo = new MemberVO(memberID: id, status: ReferenceConstants.MEMBER_STATUS.ACTIVE, joinDate: LocalDate.of(2025, 1, 1))
        MemberDto updateDto = new MemberDto(memberID: id, status: ReferenceConstants.MEMBER_STATUS.INACTIVE, reactivationDate: actionDate)
        MemberVO updatedVo = new MemberVO(memberID: id, status: ReferenceConstants.MEMBER_STATUS.INACTIVE, joinDate: LocalDate.of(2025, 1, 1))

        MemberMembershipPeriodVO openPeriod = MemberMembershipPeriodVO.builder()
                .periodID(100)
                .memberID(id)
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(null)
                .build()

        when: "updateMember is executed"
        memberImpl.updateMember(updateDto)

        then: "Existing period is closed with actionDate"
        1 * memberRepo.findById(id) >> Optional.of(existingVo)
        1 * voTransformer.transformForUpdate(updateDto, existingVo) >> updatedVo
        1 * validator.validateForUpdate(updatedVo)
        1 * memberRepo.save(updatedVo) >> updatedVo
        1 * domainEventPublisher.publish("UPDATED", updatedVo, id)
        1 * memberMembershipPeriod.findFirstByMemberIDAndEndDateIsNullOrderByStartDateDesc(id) >> Optional.of(openPeriod)
        1 * memberMembershipPeriod.update({ MemberMembershipPeriodVO p ->
            p.periodID == 100 && p.memberID == id && p.endDate == actionDate
        })
        0 * _

        and:
        noExceptionThrown()
    }

    def "test - updateMember: Transition INACTIVE to ACTIVE creates new reactivation period"() {
        given: "An inactive member reactivated"
        Integer id = 40
        LocalDate reactivationDate = LocalDate.of(2028, 4, 1)
        MemberVO existingVo = new MemberVO(memberID: id, status: ReferenceConstants.MEMBER_STATUS.INACTIVE, joinDate: LocalDate.of(2026, 1, 1))
        MemberDto updateDto = new MemberDto(memberID: id, status: ReferenceConstants.MEMBER_STATUS.ACTIVE, reactivationDate: reactivationDate)
        MemberVO updatedVo = new MemberVO(memberID: id, status: ReferenceConstants.MEMBER_STATUS.ACTIVE, joinDate: LocalDate.of(2026, 1, 1))

        when: "updateMember is executed"
        memberImpl.updateMember(updateDto)

        then: "New period is saved with reactivation start date"
        1 * memberRepo.findById(id) >> Optional.of(existingVo)
        1 * voTransformer.transformForUpdate(updateDto, existingVo) >> updatedVo
        1 * validator.validateForUpdate(updatedVo)
        1 * memberRepo.save(updatedVo) >> updatedVo
        1 * domainEventPublisher.publish("UPDATED", updatedVo, id)
        1 * memberMembershipPeriod.add({ MemberMembershipPeriodVO p ->
            p.memberID == id && p.startDate == reactivationDate && p.endDate == null
        })
        0 * _

        and:
        noExceptionThrown()
    }

    def "test - activateMemberOnRegistrationFee: Activates PENDING member and opens period"() {
        given: "A PENDING member and registration payment date"
        Integer id = 50
        LocalDate dateReceived = LocalDate.of(2026, 5, 14)
        LocalDate quarterStart = LocalDate.of(2026, 4, 1) // Q2 start
        MemberVO existingVo = new MemberVO(memberID: id, status: ReferenceConstants.MEMBER_STATUS.PENDING, joinDate: null)

        when: "activateMemberOnRegistrationFee is executed"
        memberImpl.activateMemberOnRegistrationFee(id, dateReceived)

        then: "Member becomes ACTIVE with joinDate at quarter start and period created"
        1 * memberRepo.findById(id) >> Optional.of(existingVo)
        1 * validator.validateForUpdate({ MemberVO vo ->
            vo.status == ReferenceConstants.MEMBER_STATUS.ACTIVE && vo.joinDate == quarterStart
        })
        1 * memberRepo.save({ MemberVO vo ->
            vo.status == ReferenceConstants.MEMBER_STATUS.ACTIVE && vo.joinDate == quarterStart
        }) >> existingVo
        1 * domainEventPublisher.publish("UPDATED", existingVo, id)
        1 * memberMembershipPeriod.add({ MemberMembershipPeriodVO p ->
            p.memberID == id && p.startDate == quarterStart && p.endDate == null
        })
        0 * _

        and:
        noExceptionThrown()
    }

    def "test - updateMemberJoinDateOnRegistrationFee: Updates joinDate and shifts first period start"() {
        given: "An ACTIVE member and updated payment date"
        Integer id = 60
        LocalDate newDateReceived = LocalDate.of(2026, 7, 20)
        LocalDate newQuarterStart = LocalDate.of(2026, 7, 1) // Q3 start
        MemberVO existingVo = new MemberVO(memberID: id, status: ReferenceConstants.MEMBER_STATUS.ACTIVE, joinDate: LocalDate.of(2026, 4, 1))
        MemberMembershipPeriodVO firstPeriod = new MemberMembershipPeriodVO(periodID: 200, memberID: id, startDate: LocalDate.of(2026, 4, 1), endDate: null)

        when: "updateMemberJoinDateOnRegistrationFee is executed"
        memberImpl.updateMemberJoinDateOnRegistrationFee(id, newDateReceived)

        then: "Member joinDate and first period startDate are updated"
        1 * memberRepo.findById(id) >> Optional.of(existingVo)
        1 * validator.validateForUpdate({ MemberVO vo -> vo.joinDate == newQuarterStart })
        1 * memberRepo.save({ MemberVO vo -> vo.joinDate == newQuarterStart }) >> existingVo
        1 * domainEventPublisher.publish("UPDATED", existingVo, id)
        1 * memberMembershipPeriod.findByMemberIDOrderByStartDateAsc(id) >> [firstPeriod]
        1 * memberMembershipPeriod.update({ MemberMembershipPeriodVO p ->
            p.periodID == 200 && p.startDate == newQuarterStart
        })
        0 * _

        and:
        noExceptionThrown()
    }

    def "test - deactivateDelinquentMembers: Deactivates members active full year with 0 payments"() {
        given: "Year and 2 active members: one paid, one delinquent"
        int year = 2026
        LocalDate yearStart = LocalDate.of(year, 1, 1)
        LocalDate yearEnd = LocalDate.of(year, 12, 31)

        MemberVO paidMember = new MemberVO(memberID: 1, status: ReferenceConstants.MEMBER_STATUS.ACTIVE)
        MemberVO delinquentMember = new MemberVO(memberID: 2, status: ReferenceConstants.MEMBER_STATUS.ACTIVE)

        MemberMembershipPeriodVO delinquentPeriod = new MemberMembershipPeriodVO(
                periodID: 301, memberID: 2, startDate: LocalDate.of(2025, 1, 1), endDate: null)

        PaymentSummary ps = Stub(PaymentSummary) {
            getMemberID() >> 1
        }

        when: "deactivateDelinquentMembers is called"
        int deactivated = memberImpl.deactivateDelinquentMembers(year)

        then: "Only delinquent member is deactivated and period closed"
        1 * memberRepo.findAll(_ as org.springframework.data.jpa.domain.Specification) >> [paidMember, delinquentMember]
        1 * memberMembershipPeriod.findByMemberIDInOrderByStartDateAsc([1, 2]) >> [delinquentPeriod]
        1 * payment.findPaymentSummaries(ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE, year) >> [ps]
        1 * memberMembershipPeriod.findFirstByMemberIDAndEndDateIsNullOrderByStartDateDesc(2) >> Optional.of(delinquentPeriod)
        1 * memberMembershipPeriod.update({ MemberMembershipPeriodVO p ->
            p.periodID == 301 && p.endDate == yearEnd
        })
        1 * validator.validateForUpdate({ MemberVO vo -> vo.memberID == 2 && vo.status == ReferenceConstants.MEMBER_STATUS.INACTIVE })
        1 * memberRepo.save({ MemberVO vo -> vo.memberID == 2 && vo.status == ReferenceConstants.MEMBER_STATUS.INACTIVE }) >> delinquentMember
        1 * domainEventPublisher.publish("UPDATED", delinquentMember, 2)
        0 * _

        and:
        deactivated == 1
    }

    def "test - deactivateDelinquentMembers: Returns 0 when no active members exist"() {
        given: "A year"
        int year = 2026

        when: "deactivateDelinquentMembers is called"
        int deactivated = memberImpl.deactivateDelinquentMembers(year)

        then: "Returns 0 immediately without querying payments or periods"
        1 * memberRepo.findAll(_ as org.springframework.data.jpa.domain.Specification) >> []
        0 * _

        and:
        deactivated == 0
    }

    def "test - deactivateDelinquentMembers: Does not deactivate member who joined mid-year with 0 payments"() {
        given: "Member who joined in Q3 of the year"
        int year = 2026
        MemberVO midYearMember = new MemberVO(memberID: 10, status: ReferenceConstants.MEMBER_STATUS.ACTIVE)
        MemberMembershipPeriodVO q3Period = new MemberMembershipPeriodVO(
                periodID: 501, memberID: 10, startDate: LocalDate.of(2026, 7, 1), endDate: null)

        when: "deactivateDelinquentMembers is called"
        int deactivated = memberImpl.deactivateDelinquentMembers(year)

        then: "Member is checked but NOT deactivated because they were not active all 4 quarters"
        1 * memberRepo.findAll(_ as org.springframework.data.jpa.domain.Specification) >> [midYearMember]
        1 * memberMembershipPeriod.findByMemberIDInOrderByStartDateAsc([10]) >> [q3Period]
        1 * payment.findPaymentSummaries(ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE, year) >> []
        0 * _

        and:
        deactivated == 0
    }

    def "test - deactivateDelinquentMembers: Evaluates period endDate boundary (endDate == yearEnd vs endDate > yearEnd)"() {
        given: "Two active members: Member 11 has a period closed on yearEnd (2026-12-31) and reactivated in 2027; Member 12 has a period ending after yearEnd (2027-03-31)"
        int year = 2026
        LocalDate yearEnd = LocalDate.of(year, 12, 31)
        MemberVO reactivatedMember = new MemberVO(memberID: 11, status: ReferenceConstants.MEMBER_STATUS.ACTIVE)
        MemberVO futureEndMember = new MemberVO(memberID: 12, status: ReferenceConstants.MEMBER_STATUS.ACTIVE)

        MemberMembershipPeriodVO closedOnYearEndPeriod = new MemberMembershipPeriodVO(
                periodID: 601, memberID: 11, startDate: LocalDate.of(2025, 1, 1), endDate: yearEnd)
        MemberMembershipPeriodVO open2027Period = new MemberMembershipPeriodVO(
                periodID: 602, memberID: 11, startDate: LocalDate.of(2027, 4, 1), endDate: null)
        MemberMembershipPeriodVO endingAfterYearEndPeriod = new MemberMembershipPeriodVO(
                periodID: 603, memberID: 12, startDate: LocalDate.of(2025, 1, 1), endDate: LocalDate.of(2027, 3, 31))

        when: "deactivateDelinquentMembers is called for 2026"
        int deactivated = memberImpl.deactivateDelinquentMembers(year)

        then: "Member 11 (already closed on 2026-12-31) is skipped, while Member 12 (endDate > 2026-12-31) is deactivated"
        1 * memberRepo.findAll(_ as org.springframework.data.jpa.domain.Specification) >> [reactivatedMember, futureEndMember]
        1 * memberMembershipPeriod.findByMemberIDInOrderByStartDateAsc([11, 12]) >> [closedOnYearEndPeriod, open2027Period, endingAfterYearEndPeriod]
        1 * payment.findPaymentSummaries(ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE, year) >> []
        1 * memberMembershipPeriod.findFirstByMemberIDAndEndDateIsNullOrderByStartDateDesc(12) >> Optional.empty()
        1 * validator.validateForUpdate({ MemberVO vo -> vo.memberID == 12 && vo.status == ReferenceConstants.MEMBER_STATUS.INACTIVE })
        1 * memberRepo.save({ MemberVO vo -> vo.memberID == 12 && vo.status == ReferenceConstants.MEMBER_STATUS.INACTIVE }) >> futureEndMember
        1 * domainEventPublisher.publish("UPDATED", futureEndMember, 12)
        0 * _

        and:
        deactivated == 1
    }

    def "test - getMemberSummary: Skips hiatus quarters when calculating overdue with multiple periods"() {
        given: "Member active in 2024, hiatus throughout 2025, reactivated in 2026 Q2"
        Integer id = 50
        LocalDate fixedNow = LocalDate.of(2026, 5, 15) // 2026 Q2
        MockedStatic<LocalDate> localDateMock = Mockito.mockStatic(LocalDate, Mockito.CALLS_REAL_METHODS)
        localDateMock.when(LocalDate::now).thenReturn(fixedNow)

        MemberVO member = new MemberVO(memberID: id, joinDate: LocalDate.of(2024, 1, 1), status: ReferenceConstants.MEMBER_STATUS.ACTIVE)
        List<MemberMembershipPeriodVO> periods = [
                MemberMembershipPeriodVO.builder().memberID(id).startDate(LocalDate.of(2024, 1, 1)).endDate(LocalDate.of(2024, 12, 31)).build(),
                MemberMembershipPeriodVO.builder().memberID(id).startDate(LocalDate.of(2026, 4, 1)).endDate(null).build()
        ]

        // 2024 was paid fully (4 quarters * 60 = 240)
        PaymentSummary s2024Q1 = [getMemberID: { id }, getYear: { 2024 }, getQuarter: { 1 }, getTotalPaid: { new BigDecimal("60") }] as PaymentSummary
        PaymentSummary s2024Q2 = [getMemberID: { id }, getYear: { 2024 }, getQuarter: { 2 }, getTotalPaid: { new BigDecimal("60") }] as PaymentSummary
        PaymentSummary s2024Q3 = [getMemberID: { id }, getYear: { 2024 }, getQuarter: { 3 }, getTotalPaid: { new BigDecimal("60") }] as PaymentSummary
        PaymentSummary s2024Q4 = [getMemberID: { id }, getYear: { 2024 }, getQuarter: { 4 }, getTotalPaid: { new BigDecimal("60") }] as PaymentSummary

        when: "getMemberSummary is executed"
        MemberSummaryDto summary = memberImpl.getMemberSummary(id)

        then: "Periods are loaded and hiatus year 2025 is excluded from overdue"
        1 * memberRepo.findById(id) >> Optional.of(member)
        1 * payment.sumAmountByMemberID(id) >> new BigDecimal("240")
        1 * payment.sumAmountByMemberIDAndYearAndQuarter(id, 2026, 2) >> BigDecimal.ZERO
        1 * payment.findMemberPaymentSummaries(id, ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE) >> [s2024Q1, s2024Q2, s2024Q3, s2024Q4]
        1 * systemSetting.getQuarterlyFeeAmount() >> new BigDecimal("60")
        1 * memberMembershipPeriod.findByMemberIDOrderByStartDateAsc(id) >> periods
        0 * _

        and: "Overdue is 0 because 2024 was paid and 2025 was a hiatus gap"
        summary.overdue == BigDecimal.ZERO
        summary.outstanding == new BigDecimal("60") // Current Q2 outstanding
        summary.totalPaid == new BigDecimal("240")

        cleanup:
        localDateMock.close()
    }

    def "test - period query delegation methods: countActiveMembersInDateRange, etc."() {
        given: "Member ID and dates"
        Integer memberID = 60
        LocalDate qStart = LocalDate.of(2026, 4, 1)
        LocalDate qEnd = LocalDate.of(2026, 6, 30)

        when: "countActiveMembersInDateRange is called"
        long count = memberImpl.countActiveMembersInDateRange(qStart, qEnd)

        then:
        1 * memberMembershipPeriod.findActiveMemberIDsInDateRange(qStart, qEnd) >> [1, 2, 3]
        count == 3

        when: "findMembershipPeriods is called"
        List<MemberMembershipPeriodVO> periods = memberImpl.findMembershipPeriods(memberID)

        then:
        1 * memberMembershipPeriod.findByMemberIDOrderByStartDateAsc(memberID) >> []
        periods.isEmpty()

        when: "findMembershipPeriodsForMembers is called"
        Map<Integer, List<MemberMembershipPeriodVO>> map = memberImpl.findMembershipPeriodsForMembers([1, 2])

        then:
        1 * memberMembershipPeriod.findByMemberIDInOrderByStartDateAsc([1, 2]) >> [
                MemberMembershipPeriodVO.builder().memberID(1).startDate(qStart).build()
        ]
        0 * _

        and:
        map.containsKey(1)
    }
}