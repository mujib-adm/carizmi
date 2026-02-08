package org.sofumar.portal.service.helper.impl

import org.mockito.MockedStatic
import org.mockito.Mockito
import org.sofumar.portal.constants.QuarterStatus
import org.sofumar.portal.constants.ReferenceConstants
import org.sofumar.portal.data.dto.response.DashboardMetricsDto
import org.sofumar.portal.core.vo.MemberVO
import org.sofumar.portal.framework.data.response.GlobalResponse
import org.sofumar.portal.core.businesslogic.Expense
import org.sofumar.portal.core.businesslogic.Member
import org.sofumar.portal.core.businesslogic.Payment
import org.sofumar.portal.data.dto.response.PaymentSummary
import org.sofumar.portal.service.helper.BaselineService
import org.sofumar.portal.testsupport.BaseSpecification
import org.springframework.data.jpa.domain.Specification as JpaSpecification
import org.springframework.http.ResponseEntity
import spock.lang.Subject

import java.time.LocalDate

class DashboardServiceSpec extends BaseSpecification {

    Member member = Mock()
    Payment payment = Mock()
    Expense expense = Mock()
    BaselineService baselineService = Mock()

    @Subject
    DashboardServiceImpl dashboardService = new DashboardServiceImpl(member, payment, expense, baselineService)

    def "test - getMetrics: Should handle various time contexts and data states"() {
        given: "A mocked date in Q2 and specific data states"
        int year = LocalDate.now().year
        LocalDate fixedDate = LocalDate.of(year, 5, 15) // Q2
        MockedStatic<LocalDate> localDateMock = Mockito.mockStatic(LocalDate, Mockito.CALLS_REAL_METHODS)
        localDateMock.when(LocalDate::now).thenReturn(fixedDate)

        long activeMemberCount = 3
        BigDecimal yearlyBaseline = new BigDecimal("5000.00")
        BigDecimal paidAmount = new BigDecimal("30.00")

        MemberVO m1 = new MemberVO(memberID: 1, joinDate: LocalDate.of(year - 1, 1, 1))
        MemberVO m2 = new MemberVO(memberID: 2, joinDate: LocalDate.of(year, 1, 1))
        MemberVO m3 = new MemberVO(memberID: 3, joinDate: LocalDate.of(year + 1, 1, 1))
        MemberVO m4 = new MemberVO(memberID: 4, joinDate: null)

        PaymentSummary ps1 = Mock(PaymentSummary)

        List<JpaSpecification> capturedSpecs = []
        ResponseEntity<GlobalResponse<DashboardMetricsDto>> response

        when: "The target method executed"
        response = dashboardService.getMetrics()

        then: "The expected calls are made"
        1 * member.countActiveMembers() >> activeMemberCount
        1 * baselineService.getBaselineForYear(year) >> yearlyBaseline
        1 * payment.sumAmountByDateReceivedBetween(_ as LocalDate, fixedDate) >> null
        1 * expense.sumAmountByDateOfExpenseBetween(_ as LocalDate, fixedDate) >> null
        // Q2 logic: 1 call from main method, 1 from compute(Q1), 1 from compute(Q2) = 3 total
        3 * payment.sumAmountByYearAndQuarter(year, _ as Integer) >> null
        1 * payment.findPaymentSummaries(ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE, year) >> [ps1]
        1 * member.findAllActiveMembers() >> [m1, m2, m3, m4]
        // Implicit calls from summary logic - use wildcards effectively but strictly
        _ * ps1.getMemberID() >> 1
        _ * ps1.getQuarter() >> 1
        _ * ps1.getTotalPaid() >> paidAmount

        0 * _

        and: "The expected result"
        response != null
        response.body.responseData.totalRevenue == yearlyBaseline
        response.body.responseData.overdueTotal >= paidAmount
        response.body.responseData.quarterlyCollections[0].status == QuarterStatus.PAST
        response.body.responseData.quarterlyCollections[1].status == QuarterStatus.CURRENT
        response.body.responseData.quarterlyCollections[2].status == QuarterStatus.FUTURE
        noExceptionThrown()

        cleanup:
        localDateMock.close()
    }

    def "test - getMetrics: Handling zero members and negative dues"() {
        given: "No active members and high relative collections"
        int year = LocalDate.now().year
        LocalDate fixedDate = LocalDate.of(year, 2, 1) // Q1
        MockedStatic<LocalDate> localDateMock = Mockito.mockStatic(LocalDate, Mockito.CALLS_REAL_METHODS)
        localDateMock.when(LocalDate::now).thenReturn(fixedDate)
        BigDecimal collectionAmount = new BigDecimal("100.00")

        ResponseEntity<GlobalResponse<DashboardMetricsDto>> response

        when: "The target method executed"
        response = dashboardService.getMetrics()

        then: "The expected calls are made"
        1 * member.countActiveMembers() >> 0
        1 * baselineService.getBaselineForYear(year) >> BigDecimal.ZERO
        1 * payment.sumAmountByDateReceivedBetween(_, _) >> BigDecimal.ZERO
        1 * expense.sumAmountByDateOfExpenseBetween(_, _) >> BigDecimal.ZERO
        // Q1 logic: 1 call from main, 1 from compute(Q1) = 2 total
        2 * payment.sumAmountByYearAndQuarter(year, _) >> collectionAmount
        1 * payment.findPaymentSummaries(_, _) >> []
        1 * member.findAllActiveMembers() >> []
        0 * _

        and: "The expected result"
        response.body.responseData.duesThisQuarter == BigDecimal.ZERO
        response.body.responseData.quarterlyCollections[0].percentage == 0.0
        noExceptionThrown()

        cleanup:
        localDateMock.close()
    }
}