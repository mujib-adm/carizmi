package io.carizmi.domain.platform.service

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import io.carizmi.domain.finance.service.Expense
import io.carizmi.domain.finance.service.Payment
import io.carizmi.domain.membership.service.Member
import io.carizmi.domain.platform.model.DashboardSnapshotVO
import io.carizmi.shared.constants.ReferenceConstants
import io.carizmi.shared.data.dto.QuarterlyTotalProjection
import io.carizmi.shared.util.QuarterUtils
import io.carizmi.testbase.BaseSpecification
import spock.lang.Subject

import java.time.LocalDate

class DashboardProjectorSpec extends BaseSpecification {

    DashboardSnapshot dashboardSnapshot = Mock()
    Member member = Mock()
    Payment payment = Mock()
    Expense expense = Mock()
    BaselineService baselineService = Mock()
    SystemSetting systemSetting = Mock()
    ObjectMapper objectMapper = Mock()

    @Subject
    DashboardProjector projector = new DashboardProjector(dashboardSnapshot, member, payment, expense, baselineService, systemSetting, objectMapper)

    LocalDate now = LocalDate.now()
    int currentYear = now.getYear()
    int currentQuarter = QuarterUtils.quarterOf(now)
    LocalDate startOfYear = LocalDate.of(currentYear, 1, 1)
    BigDecimal quarterlyFeeAmt = new BigDecimal("60.00")

    def "test rebuildProjection - Should compute all metrics and persist snapshot"() {
        given:
        long totalActiveMembers = 3L
        BigDecimal yearlyBaseline = new BigDecimal("1000.00")
        BigDecimal currentYearPayments = new BigDecimal("500.00")
        BigDecimal currentYearExpenses = new BigDecimal("200.00")
        BigDecimal expectedRevenue = yearlyBaseline + currentYearPayments - currentYearExpenses

        BigDecimal expectedDues = BigDecimal.valueOf(totalActiveMembers) * quarterlyFeeAmt
        BigDecimal collectedCurrentQ = new BigDecimal("60.00")
        BigDecimal expectedDuesThisQuarter = expectedDues - collectedCurrentQ

        BigDecimal overdueTotal = new BigDecimal("120.00")

        QuarterlyTotalProjection qt1 = Stub() { getQuarter() >> 1; getTotalCollected() >> new BigDecimal("180.00") }
        QuarterlyTotalProjection qt2 = Stub() { getQuarter() >> 2; getTotalCollected() >> new BigDecimal("60.00") }

        DashboardSnapshotVO existingSnapshot = new DashboardSnapshotVO(id: 1)
        DashboardSnapshotVO capturedSnapshot

        when: "The target method executed"
        projector.rebuildProjection()

        then: "The expected calls are made"
        1 * systemSetting.getQuarterlyFeeAmount() >> quarterlyFeeAmt
        1 * member.countActiveMembers() >> totalActiveMembers
        1 * baselineService.getBaselineForYear(currentYear) >> yearlyBaseline
        1 * payment.sumAmountByDateReceivedBetween(startOfYear, now) >> currentYearPayments
        1 * expense.sumAmountByDateOfExpenseBetween(startOfYear, now) >> currentYearExpenses
        1 * payment.sumAmountByYearAndQuarter(currentYear, currentQuarter) >> collectedCurrentQ
        1 * payment.calculateTotalOverdue(currentYear, currentQuarter, quarterlyFeeAmt,
                ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE, ReferenceConstants.MEMBER_STATUS.ACTIVE) >> overdueTotal
        1 * payment.findQuarterlyTotals(currentYear) >> [qt1, qt2]
        1 * dashboardSnapshot.getSnapshot() >> Optional.of(existingSnapshot)
        1 * objectMapper.writeValueAsString(_ as List) >> '[]'
        1 * dashboardSnapshot.saveSnapshot(_ as DashboardSnapshotVO) >> { DashboardSnapshotVO vo -> capturedSnapshot = vo }
        0 * _

        and: "The snapshot is populated with the computed values"
        capturedSnapshot != null
        capturedSnapshot.id == 1
        capturedSnapshot.totalActiveMembers == totalActiveMembers
        capturedSnapshot.totalRevenue == expectedRevenue
        capturedSnapshot.duesThisQuarter == expectedDuesThisQuarter
        capturedSnapshot.overdueTotal == overdueTotal
        capturedSnapshot.quarterlyFeeAmt == quarterlyFeeAmt
        capturedSnapshot.lastProjectedAt != null
        noExceptionThrown()
    }

    def "test rebuildProjection - Should create new snapshot when none exists"() {
        given: "No existing snapshot"
        DashboardSnapshotVO capturedSnapshot

        when: "The target method executed"
        projector.rebuildProjection()

        then: "The expected calls are made"
        1 * systemSetting.getQuarterlyFeeAmount() >> quarterlyFeeAmt
        1 * member.countActiveMembers() >> 0L
        1 * baselineService.getBaselineForYear(currentYear) >> BigDecimal.ZERO
        1 * payment.sumAmountByDateReceivedBetween(startOfYear, now) >> BigDecimal.ZERO
        1 * expense.sumAmountByDateOfExpenseBetween(startOfYear, now) >> BigDecimal.ZERO
        1 * payment.sumAmountByYearAndQuarter(currentYear, currentQuarter) >> BigDecimal.ZERO
        1 * payment.calculateTotalOverdue(currentYear, currentQuarter, quarterlyFeeAmt,
                ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE, ReferenceConstants.MEMBER_STATUS.ACTIVE) >> BigDecimal.ZERO
        1 * payment.findQuarterlyTotals(currentYear) >> []
        1 * dashboardSnapshot.getSnapshot() >> Optional.empty()
        1 * objectMapper.writeValueAsString(_ as List) >> '[]'
        1 * dashboardSnapshot.saveSnapshot(_ as DashboardSnapshotVO) >> { DashboardSnapshotVO vo -> capturedSnapshot = vo }
        0 * _

        and: "A new snapshot is created with id=1 and zero values"
        capturedSnapshot != null
        capturedSnapshot.id == 1
        capturedSnapshot.totalActiveMembers == 0L
        capturedSnapshot.totalRevenue == BigDecimal.ZERO
        capturedSnapshot.overdueTotal == BigDecimal.ZERO
        noExceptionThrown()
    }

    def "test rebuildProjection - Should clamp negative dues to zero"() {
        given:
        long totalActiveMembers = 1L
        BigDecimal collectedCurrentQ = new BigDecimal("100.00") // More than 60.00 fee

        DashboardSnapshotVO capturedSnapshot = null

        when: "The target method executed"
        projector.rebuildProjection()

        then: "The expected calls are made"
        1 * systemSetting.getQuarterlyFeeAmount() >> quarterlyFeeAmt
        1 * member.countActiveMembers() >> totalActiveMembers
        1 * baselineService.getBaselineForYear(currentYear) >> BigDecimal.ZERO
        1 * payment.sumAmountByDateReceivedBetween(startOfYear, now) >> BigDecimal.ZERO
        1 * expense.sumAmountByDateOfExpenseBetween(startOfYear, now) >> BigDecimal.ZERO
        1 * payment.sumAmountByYearAndQuarter(currentYear, currentQuarter) >> collectedCurrentQ
        1 * payment.calculateTotalOverdue(currentYear, currentQuarter, quarterlyFeeAmt,
                ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE, ReferenceConstants.MEMBER_STATUS.ACTIVE) >> BigDecimal.ZERO
        1 * payment.findQuarterlyTotals(currentYear) >> []
        1 * dashboardSnapshot.getSnapshot() >> Optional.empty()
        1 * objectMapper.writeValueAsString(_ as List) >> '[]'
        1 * dashboardSnapshot.saveSnapshot(_ as DashboardSnapshotVO) >> { DashboardSnapshotVO vo -> capturedSnapshot = vo }
        0 * _

        and: "Dues clamped to zero (not negative)"
        capturedSnapshot.duesThisQuarter == BigDecimal.ZERO
        noExceptionThrown()
    }

    def "test rebuildProjection - Should handle JsonProcessingException gracefully"() {
        when: "The target method executed and serialization fails"
        projector.rebuildProjection()

        then: "The expected calls are made up to the failure point"
        1 * systemSetting.getQuarterlyFeeAmount() >> quarterlyFeeAmt
        1 * member.countActiveMembers() >> 1L
        1 * baselineService.getBaselineForYear(currentYear) >> BigDecimal.ZERO
        1 * payment.sumAmountByDateReceivedBetween(startOfYear, now) >> BigDecimal.ZERO
        1 * expense.sumAmountByDateOfExpenseBetween(startOfYear, now) >> BigDecimal.ZERO
        1 * payment.sumAmountByYearAndQuarter(currentYear, currentQuarter) >> BigDecimal.ZERO
        1 * payment.calculateTotalOverdue(currentYear, currentQuarter, quarterlyFeeAmt,
                ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE, ReferenceConstants.MEMBER_STATUS.ACTIVE) >> BigDecimal.ZERO
        1 * payment.findQuarterlyTotals(currentYear) >> []
        1 * dashboardSnapshot.getSnapshot() >> Optional.empty()
        1 * objectMapper.writeValueAsString(_ as List) >> { throw new JsonProcessingException("test error") {} }
        0 * _

        and: "The exception is caught — no snapshot saved, but no exception thrown"
        noExceptionThrown()
    }

    def "test - supportedAggregateTypes: Should return PaymentVO, ExpenseVO, MemberVO"() {
        when: "The supported types are queried"
        Set<String> types = projector.supportedAggregateTypes()

        then: "No external calls are made"
        0 * _

        and: "The correct aggregate types are returned"
        types.size() == 3
        types.containsAll(["PaymentVO", "ExpenseVO", "MemberVO"])
        noExceptionThrown()
    }
}