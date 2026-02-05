package org.sofumar.portal.core.businesslogic.impl

import org.mockito.MockedStatic
import org.mockito.Mockito
import org.sofumar.portal.constants.FieldConstants
import org.sofumar.portal.constants.ReferenceCodeConstants
import org.sofumar.portal.core.businesslogic.Payment
import org.sofumar.portal.core.repo.MemberRepository
import org.sofumar.portal.core.vo.MemberVO
import org.sofumar.portal.data.dto.MemberDto
import org.sofumar.portal.data.dto.request.MemberSearchRequestDto
import org.sofumar.portal.data.dto.response.MemberLookupDto
import org.sofumar.portal.data.dto.response.MemberSummaryDto
import org.sofumar.portal.data.dto.response.PaymentSummary
import org.sofumar.portal.data.transformer.MemberDtoTransformer
import org.sofumar.portal.data.transformer.MemberVOTransformer
import org.sofumar.portal.framework.data.response.GlobalResponse
import org.sofumar.portal.framework.exception.DuplicateRecordException
import org.sofumar.portal.framework.exception.RecordNotFoundException
import org.sofumar.portal.framework.util.MySQLConstraintResolver
import org.sofumar.portal.service.validation.MemberValidator
import org.sofumar.portal.testsupport.BaseSpecification
import org.springframework.dao.DataAccessException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification as JpaSpecification
import org.springframework.http.ResponseEntity
import org.springframework.test.util.ReflectionTestUtils
import spock.lang.Subject
import spock.lang.Unroll

import java.time.LocalDate

class MemberSpec extends BaseSpecification {

    static final BigDecimal QUARTERLY_FEE = 60

    MemberRepository memberRepo = Mock()
    Payment payment = Mock()
    MemberVOTransformer voTransformer = Mock()
    MemberDtoTransformer dtoTransformer = Mock()
    MemberValidator validator = Mock()
    MySQLConstraintResolver constraintResolver = Mock()

    @Subject
    MemberImpl memberService = new MemberImpl(memberRepo, payment, voTransformer, dtoTransformer, validator)

    void setup() {
        ReflectionTestUtils.setField(memberService, "constraintResolver", constraintResolver)
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
        ResponseEntity<GlobalResponse<Integer>> response

        when: "The target method executed"
        response = memberService.addMember(requestDto)

        then: "The expected calls are made"
        1 * voTransformer.transform(_) >> { MemberDto dto -> capturedDto = dto; transformedVo }
        1 * validator.validate(transformedVo)
        1 * memberRepo.save(_) >> { MemberVO vo -> capturedVo = vo; savedVo }
        0 * _

        and: "The expected result"
        response.body.responseData == 1
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
        memberService.addMember(requestDto)

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
        memberService.addMember(requestDto)

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
        ResponseEntity<GlobalResponse<Void>> response

        when: "The target method executed"
        response = memberService.updateMember(dto)

        then: "The expected calls are made"
        1 * memberRepo.findById(1) >> Optional.of(vo)
        1 * voTransformer.transformForUpdate(dto, vo) >> vo
        1 * validator.validateForUpdate(vo)
        1 * memberRepo.save(vo) >> vo
        0 * _

        and: "The expected result"
        response.statusCode.value() == 200
        noExceptionThrown()
    }

    def "test - updateMember: DB Error"() {
        given: "A DB error during update"
        Integer id = 1
        MemberDto dto = new MemberDto(memberID: id)
        MemberVO vo = new MemberVO(memberID: id)

        when: "The target method executed"
        memberService.updateMember(dto)

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
        memberService.updateMember(dto)

        then: "The expected calls are made"
        1 * memberRepo.findById(99) >> Optional.empty()
        0 * _

        and: "The expected result"
        thrown(RecordNotFoundException)
    }

    def "test - updateMember: Missing ID"() {
        given: "An empty member DTO"
        ResponseEntity<GlobalResponse<Void>> response

        when: "The target method executed"
        response = memberService.updateMember(new MemberDto())

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        response.statusCode.value() == 400
        noExceptionThrown()
    }

    def "test - deleteMember: Success"() {
        given: "An existing member VO"
        Integer id = 1
        MemberVO vo = new MemberVO(memberID: id)
        ResponseEntity<GlobalResponse<Void>> response

        when: "The target method executed"
        response = memberService.deleteMember(id)

        then: "The expected calls are made"
        1 * memberRepo.findById(1) >> Optional.of(vo)
        1 * memberRepo.delete(vo)
        0 * _

        and: "The expected result"
        response.statusCode.value() == 200
        noExceptionThrown()
    }

    def "test - deleteMember: DB Error"() {
        given: "A DB error during deletion"
        Integer id = 1
        MemberVO vo = new MemberVO(memberID: id)

        when: "The target method executed"
        memberService.deleteMember(id)

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
        memberService.deleteMember(memberID)

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
        memberService.searchMembers(request)

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
        ResponseEntity<GlobalResponse<MemberDto>> response

        when: "The target method executed"
        response = memberService.getMember(id)

        then: "The expected calls are made"
        1 * memberRepo.findById(1) >> Optional.of(vo)
        1 * dtoTransformer.transform(vo) >> new MemberDto()
        0 * _

        and: "The expected result"
        response.statusCode.value() == 200
        noExceptionThrown()
    }

    def "test - getMember: Not Found"() {
        given: "A non-existent member ID"
        Integer id = 99

        when: "The target method executed"
        memberService.getMember(id)

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
        ResponseEntity<GlobalResponse<List<MemberLookupDto>>> response

        when: "The target method executed"
        response = memberService.lookupMembers(query)

        then: "The expected calls are made"
        1 * memberRepo.findAll(_ as JpaSpecification, _ as PageRequest) >> mockPage
        1 * mockPage.getContent() >> [m1]
        0 * _

        and: "The expected result"
        response.body.responseData.size() == 1
        response.body.responseData[0].firstName == firstName
        response.body.responseData[0].phone == phone
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
        ResponseEntity<GlobalResponse<MemberSummaryDto>> response

        when: "The target method executed"
        response = memberService.getMemberSummary(id)

        then: "The expected calls are made"
        1 * memberRepo.findById(id) >> (found ? Optional.of(member) : Optional.empty())
        if (found) {
            1 * payment.sumAmountByMemberID(id) >> paidTotal
            1 * payment.sumAmountByMemberIDAndYearAndQuarter(id, currentYear, 2) >> paidCurrent
            1 * payment.findMemberPaymentSummaries(id, ReferenceCodeConstants.FEE_TYPE.MEMBERSHIP_FEE) >> (paidPast != null ? [psQ1] : [])
            // Implicit calls - use wildcards to handle varying scenarios strictly
            _ * psQ1.getYear() >> currentYear
            _ * psQ1.getQuarter() >> 1
            _ * psQ1.getTotalPaid() >> paidPast
        }
        0 * _

        and: "The expected result"
        if (found) {
            response.body.responseData.totalPaid == (paidTotal ?: 0)
            response.body.responseData.outstanding == (BigDecimal) Math.max(0, QUARTERLY_FEE - (paidCurrent ?: 0) as double)
            if (join == null || join.year > currentYear) {
                response.body.responseData.overdue == 0
            } else if (join == joinYearStart) {
                response.body.responseData.overdue == (BigDecimal) Math.max(0, QUARTERLY_FEE - (paidPast ?: 0) as double)
            }
        } else {
            response.body.responseData.totalPaid == 0
        }
        noExceptionThrown()

        cleanup:
        localDateMock.close()

        where:
        id | found | joinType | paidTotal | paidCurrent | paidPast
        1  | true  | 'START'  | 90.00     | 30.00       | 60.00
        2  | true  | 'Q2'     | 0         | null        | null
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
        long result = memberService.countActiveMembers()

        then: "The expected calls are made"
        1 * memberRepo.count(_ as JpaSpecification) >> { JpaSpecification spec -> capturedSpec = spec; expectedCount }
        0 * _

        and: "The expected result"
        result == expectedCount
        Map<String, List> inspection = inspectSpecification(capturedSpec)
        inspection.filters.contains(FieldConstants.STATUS)
        inspection.values.contains(ReferenceCodeConstants.MEMBER_STATUS.ACTIVE)
        noExceptionThrown()

        where:
        expectedCount << [0L, 5L, 100L]
    }

    @Unroll
    def "test - findAllActiveMembers: Should delegate to repo with active status criteria [listSize: #expectedList.size()]"() {
        given: "Setup"
        JpaSpecification capturedSpec = null

        when: "The target method executed"
        List<MemberVO> result = memberService.findAllActiveMembers()

        then: "The expected calls are made"
        1 * memberRepo.findAll(_ as JpaSpecification) >> { JpaSpecification spec -> capturedSpec = spec; expectedList }
        0 * _

        and: "The expected result"
        result == expectedList
        Map<String, List> inspection = inspectSpecification(capturedSpec)
        inspection.filters.contains(FieldConstants.STATUS)
        inspection.values.contains(ReferenceCodeConstants.MEMBER_STATUS.ACTIVE)
        noExceptionThrown()

        where:
        expectedList << [
                [],
                [new MemberVO(memberID: 1)],
                [new MemberVO(memberID: 1), new MemberVO(memberID: 2)]
        ]
    }
}