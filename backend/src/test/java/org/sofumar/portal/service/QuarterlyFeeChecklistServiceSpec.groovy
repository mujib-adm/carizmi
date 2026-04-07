package org.sofumar.portal.service

import org.sofumar.portal.constants.FieldConstants
import org.sofumar.portal.constants.QuarterCellStatus
import org.sofumar.portal.constants.ReferenceConstants
import org.sofumar.portal.core.businesslogic.Member
import org.sofumar.portal.core.businesslogic.Payment
import org.sofumar.portal.core.vo.MemberVO
import org.sofumar.portal.data.dto.request.ChecklistSearchRequestDto
import org.sofumar.portal.data.dto.response.MemberQuarterlyRowDto
import org.sofumar.portal.data.dto.response.PaymentSummary
import org.sofumar.portal.data.dto.response.QuarterlyChecklistDto
import org.sofumar.portal.framework.data.response.SinglePagedResult
import org.sofumar.portal.service.helper.impl.QuarterlyFeeChecklistServiceImpl
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import spock.lang.Specification
import spock.lang.Subject

import java.time.LocalDate

class QuarterlyFeeChecklistServiceSpec extends Specification {

    Member member = Mock()
    Payment payment = Mock()

    @Subject
    QuarterlyFeeChecklistServiceImpl service = new QuarterlyFeeChecklistServiceImpl(member, payment)

    // --- Helper methods ---

    private MemberVO createMember(Integer id, String firstName, String lastName, LocalDate joinDate) {
        MemberVO memberVO = new MemberVO()
        memberVO.setMemberID(id)
        memberVO.setFirstName(firstName)
        memberVO.setLastName(lastName)
        memberVO.setJoinDate(joinDate)
        memberVO.setStatus(ReferenceConstants.MEMBER_STATUS.ACTIVE)
        return memberVO
    }

    private PaymentSummary createSummary(Integer memberID, Integer year, Integer quarter, BigDecimal totalPaid) {
        return [
                getMemberID : { -> memberID },
                getYear     : { -> year },
                getQuarter  : { -> quarter },
                getTotalPaid: { -> totalPaid }
        ] as PaymentSummary
    }

    private ChecklistSearchRequestDto searchRequest(Integer year, int page = 0, int size = 100) {
        ChecklistSearchRequestDto requestDto = new ChecklistSearchRequestDto()
        requestDto.setYear(year)
        requestDto.setPage(page)
        requestDto.setSize(size)
        return requestDto
    }

    /**
     * Creates a Page wrapping the given members, sorted by firstName then lastName,
     * matching the DB-level sort that the service requests.
     */
    private Page<MemberVO> createPage(List<MemberVO> members, int page = 0, int size = 100) {
        List<MemberVO> sorted = members.sort { a, b ->
            int cmp = (a.firstName ?: '').compareTo(b.firstName ?: '')
            cmp != 0 ? cmp : (a.lastName ?: '').compareTo(b.lastName ?: '')
        }
        PageRequest pageable = PageRequest.of(page, size, Sort.by(FieldConstants.FIRST_NAME, FieldConstants.LAST_NAME))
        return new PageImpl<>(sorted, pageable, sorted.size())
    }

    // --- Test cases ---

    def "test getQuarterlyChecklist - all quarters paid - member active full year"() {
        given: "a member active since before the assessed year with all quarters paid"
        int year = LocalDate.now().getYear()
        int currentQuarter = (LocalDate.now().getMonthValue() - 1).intdiv(3) + 1

        MemberVO memberVO1 = createMember(1, "First1", "Last1", LocalDate.of(year - 1, 1, 1))
        Page<MemberVO> expectedPage = createPage([memberVO1])

        List<PaymentSummary> summaries = (1..currentQuarter).collect { q ->
            createSummary(1, year, q, new BigDecimal("60.00"))
        }

        Pageable capturedPageable

        when: "The target method executed"
        SinglePagedResult<QuarterlyChecklistDto> checklistResult = service.getQuarterlyChecklist(searchRequest(year))

        then: "The expected calls are made"
        1 * member.findActiveMembers(_ as Pageable) >> { Pageable p -> capturedPageable = p; expectedPage }
        1 * payment.findMembersPaymentSummaries([1], ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE, year) >> summaries
        0 * _

        and: "The expected result"
        QuarterlyChecklistDto result = checklistResult.data()
        result.rows.size() == 1
        MemberQuarterlyRowDto row = result.rows[0]
        row.memberID == 1
        row.memberName == "First1 Last1"

        // Assessable quarters should be PAID
        (0..<currentQuarter).each { idx ->
            assert row.quarters[idx].status == QuarterCellStatus.PAID
        }

        // Future quarters
        (currentQuarter..<4).each { idx ->
            assert row.quarters[idx].status == QuarterCellStatus.FUTURE
        }

        row.balance == BigDecimal.ZERO

        capturedPageable != null
        capturedPageable.pageNumber == 0
        capturedPageable.pageSize == 100
        capturedPageable.sort == Sort.by(FieldConstants.FIRST_NAME, FieldConstants.LAST_NAME)
        noExceptionThrown()
    }

    def "test getQuarterlyChecklist - member joined mid-year - Q1 is N/A"() {
        given: "a member who joined in Q2 of a past year (so all 4 quarters are assessable)"
        int year = LocalDate.now().getYear() - 1

        MemberVO memberVO1 = createMember(3, "First1", "Last1", LocalDate.of(year, 4, 15))
        Page<MemberVO> expectedPage = createPage([memberVO1])

        // Paid Q2
        List<PaymentSummary> summaries = [createSummary(3, year, 2, new BigDecimal("60.00"))]

        Pageable capturedPageable

        when: "The target method executed"
        SinglePagedResult<QuarterlyChecklistDto> checklistResult = service.getQuarterlyChecklist(searchRequest(year))

        then: "The expected calls are made"
        1 * member.findActiveMembers(_ as Pageable) >> { Pageable p -> capturedPageable = p; expectedPage }
        1 * payment.findMembersPaymentSummaries([3], ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE, year) >> summaries
        0 * _

        and: "The expected result"
        QuarterlyChecklistDto result = checklistResult.data()
        MemberQuarterlyRowDto row = result.rows[0]
        row.quarters[0].status == QuarterCellStatus.NOT_APPLICABLE
        row.quarters[1].status == QuarterCellStatus.PAID
        row.quarters[2].status == QuarterCellStatus.UNPAID
        row.quarters[3].status == QuarterCellStatus.UNPAID
        row.totalPaid == new BigDecimal("60.00")
        row.balance == new BigDecimal("120.00")   // 3 eligible Qs * $60 - $60 paid

        capturedPageable != null
        capturedPageable.pageNumber == 0
        capturedPageable.sort == Sort.by(FieldConstants.FIRST_NAME, FieldConstants.LAST_NAME)
        noExceptionThrown()
    }

    def "test getQuarterlyChecklist - missing payments - shows UNPAID with correct balance"() {
        given: "a member who paid Q1 but missed Q2 (past year, all 4 quarters assessable)"
        int year = LocalDate.now().getYear() - 1

        MemberVO memberVO1 = createMember(2, "First1", "Last1", LocalDate.of(year - 1, 1, 1))
        Page<MemberVO> expectedPage = createPage([memberVO1])

        // Paid Q1 only
        List<PaymentSummary> summaries = [createSummary(2, year, 1, new BigDecimal("60.00"))]

        when: "The target method executed"
        SinglePagedResult<QuarterlyChecklistDto> checklistResult = service.getQuarterlyChecklist(searchRequest(year))

        then: "The expected calls are made"
        1 * member.findActiveMembers(_ as Pageable) >> expectedPage
        1 * payment.findMembersPaymentSummaries([2], ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE, year) >> summaries
        0 * _

        and: "The expected result"
        QuarterlyChecklistDto result = checklistResult.data()
        MemberQuarterlyRowDto row = result.rows[0]
        row.quarters[0].status == QuarterCellStatus.PAID
        row.quarters[1].status == QuarterCellStatus.UNPAID
        row.quarters[2].status == QuarterCellStatus.UNPAID
        row.quarters[3].status == QuarterCellStatus.UNPAID
        row.totalPaid == new BigDecimal("60.00")
        row.balance == new BigDecimal("180.00")  // 4 eligible * $60 - $60 paid
        noExceptionThrown()
    }

    def "test getQuarterlyChecklist - future quarters beyond current quarter"() {
        given: "current year with a member - quarters after current are FUTURE"
        int year = LocalDate.now().getYear()
        int currentQuarter = (LocalDate.now().getMonthValue() - 1).intdiv(3) + 1

        MemberVO memberVO1 = createMember(1, "First1", "Last1", LocalDate.of(year - 1, 1, 1))
        Page<MemberVO> expectedPage = createPage([memberVO1])

        when: "The target method executed"
        SinglePagedResult<QuarterlyChecklistDto> checklistResult = service.getQuarterlyChecklist(searchRequest(year))

        then: "The expected calls are made"
        1 * member.findActiveMembers(_ as Pageable) >> expectedPage
        1 * payment.findMembersPaymentSummaries([1], ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE, year) >> []
        0 * _

        and: "The expected result"
        QuarterlyChecklistDto result = checklistResult.data()
        MemberQuarterlyRowDto row = result.rows[0]
        (currentQuarter..<4).each { idx ->
            assert row.quarters[idx].status == QuarterCellStatus.FUTURE
        }
        noExceptionThrown()
    }

    def "test getQuarterlyChecklist - future year - all quarters are FUTURE"() {
        given: "a future year"
        int futureYear = LocalDate.now().getYear() + 1

        MemberVO memberVO1 = createMember(1, "First1", "Last1", LocalDate.of(futureYear - 2, 1, 1))
        Page<MemberVO> expectedPage = createPage([memberVO1])

        when: "The target method executed"
        SinglePagedResult<QuarterlyChecklistDto> checklistResult = service.getQuarterlyChecklist(searchRequest(futureYear))

        then: "The expected calls are made"
        1 * member.findActiveMembers(_ as Pageable) >> expectedPage
        1 * payment.findMembersPaymentSummaries([1], ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE, futureYear) >> []
        0 * _

        and: "The expected result"
        QuarterlyChecklistDto result = checklistResult.data()
        MemberQuarterlyRowDto row = result.rows[0]
        row.quarters.every { it.status == QuarterCellStatus.FUTURE }
        row.totalPaid == BigDecimal.ZERO
        row.balance == BigDecimal.ZERO
        noExceptionThrown()
    }

    def "test getQuarterlyChecklist - no active members - returns empty rows"() {
        given: "no active members"
        int year = LocalDate.now().getYear()
        Page<MemberVO> emptyPage = createPage([])

        when: "The target method executed"
        SinglePagedResult<QuarterlyChecklistDto> checklistResult = service.getQuarterlyChecklist(searchRequest(year))

        then: "The expected calls are made"
        1 * member.findActiveMembers(_ as Pageable) >> emptyPage
        0 * _

        and: "The expected result"
        QuarterlyChecklistDto result = checklistResult.data()
        result.rows.isEmpty()
        result.year == year
        result.quarterlyFeeAmount == new BigDecimal("60.00")
        noExceptionThrown()
    }

    def "test getQuarterlyChecklist - member with no payments - all eligible quarters UNPAID"() {
        given: "a member active all year with zero payments"
        int year = LocalDate.now().getYear()
        int currentQuarter = (LocalDate.now().getMonthValue() - 1).intdiv(3) + 1

        MemberVO memberVO1 = createMember(1, "First1", "Last1", LocalDate.of(year - 1, 6, 1))
        Page<MemberVO> expectedPage = createPage([memberVO1])

        when: "The target method executed"
        SinglePagedResult<QuarterlyChecklistDto> checklistResult = service.getQuarterlyChecklist(searchRequest(year))

        then: "The expected calls are made"
        1 * member.findActiveMembers(_ as Pageable) >> expectedPage
        1 * payment.findMembersPaymentSummaries([1], ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE, year) >> []
        0 * _

        and: "The expected result"
        QuarterlyChecklistDto result = checklistResult.data()
        MemberQuarterlyRowDto row = result.rows[0]
        (0..<currentQuarter).each { idx ->
            assert row.quarters[idx].status == QuarterCellStatus.UNPAID
        }
        row.totalPaid == BigDecimal.ZERO
        row.balance == new BigDecimal("60.00").multiply(new BigDecimal(currentQuarter))
        noExceptionThrown()
    }

    def "test getQuarterlyChecklist - past year - all 4 quarters are assessable"() {
        given: "a past year"
        int pastYear = LocalDate.now().getYear() - 1

        MemberVO memberVO1 = createMember(1, "First1", "Last1", LocalDate.of(pastYear - 1, 1, 1))
        Page<MemberVO> expectedPage = createPage([memberVO1])

        List<PaymentSummary> summaries = (1..4).collect { q ->
            createSummary(1, pastYear, q, new BigDecimal("60.00"))
        }

        when: "The target method executed"
        SinglePagedResult<QuarterlyChecklistDto> checklistResult = service.getQuarterlyChecklist(searchRequest(pastYear))

        then: "The expected calls are made"
        1 * member.findActiveMembers(_ as Pageable) >> expectedPage
        1 * payment.findMembersPaymentSummaries([1], ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE, pastYear) >> summaries
        0 * _

        and: "The expected result"
        QuarterlyChecklistDto result = checklistResult.data()
        MemberQuarterlyRowDto row = result.rows[0]
        row.quarters.every { it.status == QuarterCellStatus.PAID }
        row.totalPaid == new BigDecimal("240.00")
        row.balance == BigDecimal.ZERO
        result.currentQuarter == 4
        noExceptionThrown()
    }

    def "test getQuarterlyChecklist - rows are sorted by member name via DB-level sort"() {
        given: "multiple members in unsorted order"
        int year = LocalDate.now().getYear()

        MemberVO memberVO3 = createMember(1, "First3", "Last3", LocalDate.of(year - 1, 1, 1))
        MemberVO memberVO1 = createMember(2, "First1", "Last1", LocalDate.of(year - 1, 1, 1))
        MemberVO memberVO2 = createMember(3, "First2", "Last2", LocalDate.of(year - 1, 1, 1))

        // The Page is created with DB-level sort (firstName, lastName)
        Page<MemberVO> expectedPage = createPage([memberVO3, memberVO1, memberVO2])

        when: "The target method executed"
        SinglePagedResult<QuarterlyChecklistDto> checklistResult = service.getQuarterlyChecklist(searchRequest(year))

        then: "The expected calls are made"
        1 * member.findActiveMembers(_ as Pageable) >> expectedPage
        1 * payment.findMembersPaymentSummaries([2, 3, 1], ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE, year) >> []
        0 * _

        and: "The expected result"
        QuarterlyChecklistDto result = checklistResult.data()
        result.rows[0].memberName == "First1 Last1"
        result.rows[1].memberName == "First2 Last2"
        result.rows[2].memberName == "First3 Last3"
        noExceptionThrown()
    }

    def "test getQuarterlyChecklist - pagination returns correct page 0 of results"() {
        given: "3 members, page size 2, requesting page 0"
        int year = LocalDate.now().getYear() - 1

        MemberVO memberVO1 = createMember(1, "First1", "Last1", LocalDate.of(year - 1, 1, 1))
        MemberVO memberVO2 = createMember(2, "First2", "Last2", LocalDate.of(year - 1, 1, 1))

        // Page 0, size 2: DB returns first 2 members sorted, with totalElements=3
        PageRequest page0Pageable = PageRequest.of(0, 2, Sort.by(FieldConstants.FIRST_NAME, FieldConstants.LAST_NAME))
        Page<MemberVO> page0 = new PageImpl<>([memberVO1, memberVO2], page0Pageable, 3)

        Pageable capturedPageable

        when: "The target method executed"
        SinglePagedResult<QuarterlyChecklistDto> checklistResult = service.getQuarterlyChecklist(searchRequest(year, 0, 2))

        then: "The expected calls are made"
        1 * member.findActiveMembers(_ as Pageable) >> { Pageable p -> capturedPageable = p; page0 }
        1 * payment.findMembersPaymentSummaries([1, 2], ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE, year) >> []
        0 * _

        and: "The expected result"
        QuarterlyChecklistDto result = checklistResult.data()
        result.rows.size() == 2
        result.rows[0].memberName == "First1 Last1"
        result.rows[1].memberName == "First2 Last2"
        checklistResult.meta().page == 0
        checklistResult.meta().pageSize == 2
        checklistResult.meta().totalRecords == 3
        checklistResult.meta().totalPages == 2

        capturedPageable != null
        capturedPageable.pageNumber == 0
        capturedPageable.pageSize == 2
        capturedPageable.sort == Sort.by(FieldConstants.FIRST_NAME, FieldConstants.LAST_NAME)
        noExceptionThrown()
    }

    def "test getQuarterlyChecklist - pagination returns correct page 1 of results"() {
        given: "3 members, page size 2, requesting page 1"
        int year = LocalDate.now().getYear() - 1

        MemberVO memberVO3 = createMember(3, "First3", "Last3", LocalDate.of(year - 1, 1, 1))

        // Page 1, size 2: DB returns last member, with totalElements=3
        PageRequest page1Pageable = PageRequest.of(1, 2, Sort.by(FieldConstants.FIRST_NAME, FieldConstants.LAST_NAME))
        Page<MemberVO> page1 = new PageImpl<>([memberVO3], page1Pageable, 3)

        Pageable capturedPageable

        when: "The target method executed"
        SinglePagedResult<QuarterlyChecklistDto> checklistResult = service.getQuarterlyChecklist(searchRequest(year, 1, 2))

        then: "The expected calls are made"
        1 * member.findActiveMembers(_ as Pageable) >> { Pageable p -> capturedPageable = p; page1 }
        1 * payment.findMembersPaymentSummaries([3], ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE, year) >> []
        0 * _

        and: "The expected result"
        QuarterlyChecklistDto result = checklistResult.data()
        result.rows.size() == 1
        result.rows[0].memberName == "First3 Last3"
        checklistResult.meta().page == 1
        checklistResult.meta().pageSize == 2
        checklistResult.meta().totalRecords == 3
        checklistResult.meta().totalPages == 2

        capturedPageable != null
        capturedPageable.pageNumber == 1
        capturedPageable.pageSize == 2
        capturedPageable.sort == Sort.by(FieldConstants.FIRST_NAME, FieldConstants.LAST_NAME)
        noExceptionThrown()
    }
}