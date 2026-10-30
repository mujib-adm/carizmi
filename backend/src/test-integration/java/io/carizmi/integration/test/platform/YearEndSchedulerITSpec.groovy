package io.carizmi.integration.test.platform

import io.carizmi.domain.finance.data.dto.ExpenseDto
import io.carizmi.domain.finance.data.dto.PaymentDto
import org.springframework.cache.CacheManager
import tools.jackson.databind.ObjectMapper
import io.carizmi.domain.finance.data.dto.response.AnnualFinancialReportDto
import io.carizmi.domain.finance.model.AnnualReportSnapshotVO
import io.carizmi.domain.finance.model.ExpenseVO
import io.carizmi.domain.finance.model.PaymentVO
import io.carizmi.domain.finance.repository.AnnualReportSnapshotRepository
import io.carizmi.domain.finance.repository.ExpenseRepository
import io.carizmi.domain.finance.repository.PaymentRepository
import io.carizmi.domain.finance.service.AnnualReportService
import io.carizmi.domain.finance.service.Expense
import io.carizmi.domain.finance.service.Payment
import io.carizmi.domain.membership.model.MemberMembershipPeriodVO
import io.carizmi.domain.membership.model.MemberVO
import io.carizmi.domain.membership.repository.MemberMembershipPeriodRepository
import io.carizmi.domain.membership.repository.MemberRepository
import io.carizmi.domain.platform.constants.BaselineConstants
import io.carizmi.domain.platform.data.dto.response.DashboardMetricsDto
import io.carizmi.domain.platform.model.SystemSettingsVO
import io.carizmi.domain.platform.repository.SystemSettingRepository
import io.carizmi.domain.platform.scheduler.YearEndScheduler
import io.carizmi.domain.platform.service.BaselineService
import io.carizmi.domain.platform.service.DashboardService
import io.carizmi.domain.platform.service.SystemSetting
import io.carizmi.framework.exception.ValidationException
import io.carizmi.integration.base.BaseIntegrationSpecification
import io.carizmi.shared.constants.ReferenceConstants
import org.springframework.beans.factory.annotation.Autowired

import java.time.LocalDate

class YearEndSchedulerITSpec extends BaseIntegrationSpecification {

    @Autowired
    YearEndScheduler yearEndScheduler

    @Autowired
    BaselineService baselineService

    @Autowired
    AnnualReportService annualReportService

    @Autowired
    DashboardService dashboardService

    @Autowired
    Payment payment

    @Autowired
    Expense expense

    @Autowired
    MemberRepository memberRepository

    @Autowired
    MemberMembershipPeriodRepository memberMembershipPeriodRepository

    @Autowired
    PaymentRepository paymentRepository

    @Autowired
    ExpenseRepository expenseRepository

    @Autowired
    AnnualReportSnapshotRepository annualReportSnapshotRepository

    @Autowired
    SystemSettingRepository systemSettingRepository

    @Autowired
    SystemSetting systemSetting

    @Autowired
    ObjectMapper objectMapper

    @Autowired(required = false)
    CacheManager cacheManager

    def setup() {
        cleanTestData()
        ensureSystemSettings()
    }

    def cleanup() {
        cleanTestData()
    }

    private void clearCaches() {
        if (cacheManager != null) {
            cacheManager.cacheNames.each { name ->
                cacheManager.getCache(name)?.clear()
            }
        }
    }

    private void cleanTestData() {
        annualReportSnapshotRepository.deleteAll()
        paymentRepository.deleteAll()
        expenseRepository.deleteAll()
        memberMembershipPeriodRepository.deleteAll()
        memberRepository.deleteAll()
        systemSettingRepository.findAll().each { vo ->
            if (vo.settingKey.startsWith(BaselineConstants.KEY_PREFIX_YEARLY)) {
                systemSettingRepository.delete(vo)
            }
        }
        clearCaches()
    }

    private void ensureSystemSettings() {
        int currentYear = LocalDate.now().getYear()
        String currentYearBaselineKey = BaselineConstants.getYearlyBaselineKey(currentYear)
        systemSettingRepository.save(new SystemSettingsVO(
                settingName: BaselineConstants.NAME_BASELINE,
                settingKey: currentYearBaselineKey,
                settingValue: "50000.00",
                effectiveDate: LocalDate.of(currentYear, 1, 1),
                active: true
        ))

        if (systemSetting.findBySettingKey("MEMBERSHIP_FEE").isEmpty()) {
            systemSettingRepository.save(new SystemSettingsVO(
                    settingName: "FEE",
                    settingKey: "MEMBERSHIP_FEE",
                    settingValue: "60.00",
                    active: true
            ))
        }

        if (systemSetting.findBySettingKey("REGISTRATION_FEE").isEmpty()) {
            systemSettingRepository.save(new SystemSettingsVO(
                    settingName: "FEE",
                    settingKey: "REGISTRATION_FEE",
                    settingValue: "50.00",
                    active: true
            ))
        }

        clearCaches()
    }

    def "Integration Test: YearEndScheduler executes complete rollover on Jan 1st with 100% accounting coherence"() {
        given: "Dynamic fiscal year variables and a diverse membership population"
        int currentYear = LocalDate.now().getYear()
        int previousYear = currentYear - 1
        int twoYearsAgo = currentYear - 2
        int nextYear = currentYear + 1

        LocalDate prevYearStart = LocalDate.of(previousYear, 1, 1)
        LocalDate prevYearEnd = LocalDate.of(previousYear, 12, 31)
        LocalDate twoYearsAgoStart = LocalDate.of(twoYearsAgo, 1, 1)
        LocalDate currentYearStart = LocalDate.of(currentYear, 1, 1)
        LocalDate currentYearQ1End = LocalDate.of(currentYear, 3, 31)
        LocalDate currentYearQ3Start = LocalDate.of(currentYear, 7, 1)
        LocalDate currentYearEnd = LocalDate.of(currentYear, 12, 31)

        BigDecimal openingBaseline = new BigDecimal("50000.00")
        BigDecimal quarterlyFee = new BigDecimal("60.00")
        BigDecimal partialRegPayment = new BigDecimal("25.00")
        BigDecimal expenseAmount = new BigDecimal("100.00")
        BigDecimal expectedReceipts = new BigDecimal("890.00")
        BigDecimal expectedEndingBalance = new BigDecimal("50790.00")

        // Member A: Active all 4 quarters, 0 payments -> must be DEACTIVATED on Jan 1st
        MemberVO memberA = memberRepository.save(new MemberVO(
                firstName: "Delinquent",
                lastName: "MemberA",
                phone: "612-555-0001",
                email: "delinquent.a@test.com",
                status: ReferenceConstants.MEMBER_STATUS.ACTIVE,
                joinDate: prevYearStart,
                state: "MN"
        ))
        MemberMembershipPeriodVO periodA = memberMembershipPeriodRepository.save(MemberMembershipPeriodVO.builder()
                .memberID(memberA.getMemberID())
                .startDate(prevYearStart)
                .endDate(null)
                .build())

        // Member B: Active all 4 quarters, paid dues in full -> remains ACTIVE
        MemberVO memberB = memberRepository.save(new MemberVO(
                firstName: "Compliant",
                lastName: "MemberB",
                phone: "612-555-0002",
                email: "compliant.b@test.com",
                status: ReferenceConstants.MEMBER_STATUS.ACTIVE,
                joinDate: prevYearStart,
                state: "MN"
        ))
        MemberMembershipPeriodVO periodB = memberMembershipPeriodRepository.save(MemberMembershipPeriodVO.builder()
                .memberID(memberB.getMemberID())
                .startDate(prevYearStart)
                .endDate(null)
                .build())

        // Member C: Joined mid-year in Q3, paid registration fee -> remains ACTIVE
        MemberVO memberC = memberRepository.save(new MemberVO(
                firstName: "MidYear",
                lastName: "MemberC",
                phone: "612-555-0003",
                email: "midyear.c@test.com",
                status: ReferenceConstants.MEMBER_STATUS.ACTIVE,
                joinDate: currentYearQ3Start,
                state: "MN"
        ))
        MemberMembershipPeriodVO periodC = memberMembershipPeriodRepository.save(MemberMembershipPeriodVO.builder()
                .memberID(memberC.getMemberID())
                .startDate(currentYearQ3Start)
                .endDate(null)
                .build())

        // Member D: Inactive prior to currentYear -> remains INACTIVE
        MemberVO memberD = memberRepository.save(new MemberVO(
                firstName: "Inactive",
                lastName: "MemberD",
                phone: "612-555-0004",
                email: "inactive.d@test.com",
                status: ReferenceConstants.MEMBER_STATUS.INACTIVE,
                joinDate: twoYearsAgoStart,
                state: "MN"
        ))
        memberMembershipPeriodRepository.save(MemberMembershipPeriodVO.builder()
                .memberID(memberD.getMemberID())
                .startDate(twoYearsAgoStart)
                .endDate(prevYearEnd)
                .build())

        // Member E: Pending member -> remains PENDING, no periods
        MemberVO memberE = memberRepository.save(new MemberVO(
                firstName: "Pending",
                lastName: "MemberE",
                phone: "612-555-0005",
                email: "pending.e@test.com",
                status: ReferenceConstants.MEMBER_STATUS.PENDING,
                joinDate: null,
                state: "MN"
        ))

        // Member F: Partially compliant (active all 4 quarters, paid only Q1) -> remains ACTIVE
        MemberVO memberF = memberRepository.save(new MemberVO(
                firstName: "Partial",
                lastName: "MemberF",
                phone: "612-555-0006",
                email: "partial.f@test.com",
                status: ReferenceConstants.MEMBER_STATUS.ACTIVE,
                joinDate: prevYearStart,
                state: "MN"
        ))
        MemberMembershipPeriodVO periodF = memberMembershipPeriodRepository.save(MemberMembershipPeriodVO.builder()
                .memberID(memberF.getMemberID())
                .startDate(prevYearStart)
                .endDate(null)
                .build())

        // Member G: Multi-episode member with Q2 hiatus (active Q1, inactive Q2, reactivated Q3, 0 payments) -> remains ACTIVE
        MemberVO memberG = memberRepository.save(new MemberVO(
                firstName: "Hiatus",
                lastName: "MemberG",
                phone: "612-555-0007",
                email: "hiatus.g@test.com",
                status: ReferenceConstants.MEMBER_STATUS.ACTIVE,
                joinDate: prevYearStart,
                state: "MN"
        ))
        memberMembershipPeriodRepository.save(MemberMembershipPeriodVO.builder()
                .memberID(memberG.getMemberID())
                .startDate(prevYearStart)
                .endDate(currentYearQ1End)
                .build())
        MemberMembershipPeriodVO periodG2 = memberMembershipPeriodRepository.save(MemberMembershipPeriodVO.builder()
                .memberID(memberG.getMemberID())
                .startDate(currentYearQ3Start)
                .endDate(null)
                .build())

        // Member H: Genuine Q1 new member (joinDate currentYear-01-01 + Registration Fee) -> counted in registrationFeeSummary
        MemberVO memberH = memberRepository.save(new MemberVO(
                firstName: "NewQ1",
                lastName: "MemberH",
                phone: "612-555-0008",
                email: "newq1.h@test.com",
                status: ReferenceConstants.MEMBER_STATUS.ACTIVE,
                joinDate: currentYearStart,
                state: "MN"
        ))
        memberMembershipPeriodRepository.save(MemberMembershipPeriodVO.builder()
                .memberID(memberH.getMemberID())
                .startDate(currentYearStart)
                .endDate(null)
                .build())

        // Member I: Charter member with default currentYear-01-01 joinDate (no Registration Fee) -> excluded from registrationFeeSummary
        MemberVO memberI = memberRepository.save(new MemberVO(
                firstName: "Charter",
                lastName: "MemberI",
                phone: "612-555-0009",
                email: "charter.i@test.com",
                status: ReferenceConstants.MEMBER_STATUS.ACTIVE,
                joinDate: currentYearStart,
                state: "MN"
        ))
        memberMembershipPeriodRepository.save(MemberMembershipPeriodVO.builder()
                .memberID(memberI.getMemberID())
                .startDate(currentYearStart)
                .endDate(null)
                .build())

        and: "Financial transactions within currentYear"
        // Member B pays all 4 quarters of currentYear ($60 each = $240)
        PaymentVO paymentBQ1 = paymentRepository.save(new PaymentVO(
                member: memberB,
                feeType: ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE,
                amount: quarterlyFee,
                dateReceived: LocalDate.of(currentYear, 1, 15),
                methodOfPayment: ReferenceConstants.PAYMENT_METHOD.ZELLE,
                year: currentYear,
                quarter: 1
        ))
        paymentRepository.save(new PaymentVO(
                member: memberB,
                feeType: ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE,
                amount: quarterlyFee,
                dateReceived: LocalDate.of(currentYear, 4, 10),
                methodOfPayment: ReferenceConstants.PAYMENT_METHOD.ZELLE,
                year: currentYear,
                quarter: 2
        ))
        paymentRepository.save(new PaymentVO(
                member: memberB,
                feeType: ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE,
                amount: quarterlyFee,
                dateReceived: LocalDate.of(currentYear, 7, 12),
                methodOfPayment: ReferenceConstants.PAYMENT_METHOD.CASH,
                year: currentYear,
                quarter: 3
        ))
        paymentRepository.save(new PaymentVO(
                member: memberB,
                feeType: ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE,
                amount: quarterlyFee,
                dateReceived: LocalDate.of(currentYear, 10, 5),
                methodOfPayment: ReferenceConstants.PAYMENT_METHOD.CHECK,
                year: currentYear,
                quarter: 4
        ))

        // Member B also pays previousYear Q4 prior-year arrears in currentYear ($60.00)
        paymentRepository.save(new PaymentVO(
                member: memberB,
                feeType: ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE,
                amount: quarterlyFee,
                dateReceived: LocalDate.of(currentYear, 2, 10),
                methodOfPayment: ReferenceConstants.PAYMENT_METHOD.ZELLE,
                year: previousYear,
                quarter: 4
        ))

        // Member F pays only Q1 of currentYear ($60.00)
        paymentRepository.save(new PaymentVO(
                member: memberF,
                feeType: ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE,
                amount: quarterlyFee,
                dateReceived: LocalDate.of(currentYear, 2, 20),
                methodOfPayment: ReferenceConstants.PAYMENT_METHOD.ZELLE,
                year: currentYear,
                quarter: 1
        ))

        // Member H pays Registration Fee ($25.00) + all 4 quarters of currentYear ($240.00)
        paymentRepository.save(new PaymentVO(
                member: memberH,
                feeType: ReferenceConstants.FEE_TYPE.REGISTRATION_FEE,
                amount: partialRegPayment,
                dateReceived: LocalDate.of(currentYear, 1, 10),
                methodOfPayment: ReferenceConstants.PAYMENT_METHOD.ZELLE,
                year: null,
                quarter: null
        ))
        (1..4).each { int q ->
            paymentRepository.save(new PaymentVO(
                    member: memberH,
                    feeType: ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE,
                    amount: quarterlyFee,
                    dateReceived: LocalDate.of(currentYear, (q - 1) * 3 + 1, 18),
                    methodOfPayment: ReferenceConstants.PAYMENT_METHOD.ZELLE,
                    year: currentYear,
                    quarter: q
            ))
        }

        // Member I (charter) pays all 4 quarters of currentYear ($240.00)
        (1..4).each { int q ->
            paymentRepository.save(new PaymentVO(
                    member: memberI,
                    feeType: ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE,
                    amount: quarterlyFee,
                    dateReceived: LocalDate.of(currentYear, (q - 1) * 3 + 1, 22),
                    methodOfPayment: ReferenceConstants.PAYMENT_METHOD.CASH,
                    year: currentYear,
                    quarter: q
            ))
        }

        // Member C pays registration fee in Q3 ($25.00)
        paymentRepository.save(new PaymentVO(
                member: memberC,
                feeType: ReferenceConstants.FEE_TYPE.REGISTRATION_FEE,
                amount: partialRegPayment,
                dateReceived: LocalDate.of(currentYear, 7, 5),
                methodOfPayment: ReferenceConstants.PAYMENT_METHOD.ZELLE,
                year: null,
                quarter: null
        ))

        // Total currentYear Receipts = 780.00 (currentYear dues) + 60.00 (previousYear arrears) + 50.00 (reg fees) = 890.00

        // Operational expense in currentYear ($100.00)
        ExpenseVO savedExpense = expenseRepository.save(new ExpenseVO(
                amount: expenseAmount,
                dateOfExpense: LocalDate.of(currentYear, 6, 20),
                category: ReferenceConstants.EXPENSE_CATEGORY.OFFICE_SUPPLIES,
                description: "Annual accounting ledger notebooks"
        ))

        // Total currentYear Disbursements = 100.00
        // Net Operating Surplus = 890.00 - 100.00 = 790.00
        // Expected Ending Cash Balance = 50,000.00 + 790.00 = 50,790.00

        when: "YearEndScheduler executes the currentYear year-end process"
        yearEndScheduler.executeYearEndProcess(currentYear)

        then: "1. Member Lifecycle: Delinquent Member A is deactivated and their period closed as of Dec 31 of currentYear"
        MemberVO updatedMemberA = memberRepository.findById(memberA.getMemberID()).get()
        updatedMemberA.getStatus() == ReferenceConstants.MEMBER_STATUS.INACTIVE

        MemberMembershipPeriodVO updatedPeriodA = memberMembershipPeriodRepository.findById(periodA.getPeriodID()).get()
        updatedPeriodA.getEndDate() == currentYearEnd

        and: "2. Compliant Member B and Partially Compliant Member F remain ACTIVE with open periods"
        memberRepository.findById(memberB.getMemberID()).get().getStatus() == ReferenceConstants.MEMBER_STATUS.ACTIVE
        memberMembershipPeriodRepository.findById(periodB.getPeriodID()).get().getEndDate() == null

        memberRepository.findById(memberF.getMemberID()).get().getStatus() == ReferenceConstants.MEMBER_STATUS.ACTIVE
        memberMembershipPeriodRepository.findById(periodF.getPeriodID()).get().getEndDate() == null

        and: "3. Mid-Year Member C and Multi-Episode Hiatus Member G remain ACTIVE (not active full 4 unbroken quarters)"
        memberRepository.findById(memberC.getMemberID()).get().getStatus() == ReferenceConstants.MEMBER_STATUS.ACTIVE
        memberMembershipPeriodRepository.findById(periodC.getPeriodID()).get().getEndDate() == null

        memberRepository.findById(memberG.getMemberID()).get().getStatus() == ReferenceConstants.MEMBER_STATUS.ACTIVE
        memberMembershipPeriodRepository.findById(periodG2.getPeriodID()).get().getEndDate() == null

        and: "4. Inactive Member D, Pending Member E, Q1 New Member H, and Charter Member I statuses remain unchanged"
        memberRepository.findById(memberD.getMemberID()).get().getStatus() == ReferenceConstants.MEMBER_STATUS.INACTIVE
        memberRepository.findById(memberE.getMemberID()).get().getStatus() == ReferenceConstants.MEMBER_STATUS.PENDING
        memberRepository.findById(memberH.getMemberID()).get().getStatus() == ReferenceConstants.MEMBER_STATUS.ACTIVE
        memberRepository.findById(memberI.getMemberID()).get().getStatus() == ReferenceConstants.MEMBER_STATUS.ACTIVE

        and: "5. Baseline Roll-Forward: nextYear baseline is persisted with exact ending cash balance"
        BigDecimal baselineNextYear = baselineService.getBaselineForYear(nextYear)
        baselineNextYear == expectedEndingBalance

        and: "6. CQRS Snapshot Table: Official immutable report snapshot is frozen for currentYear"
        Optional<AnnualReportSnapshotVO> snapshotOpt = annualReportSnapshotRepository.findById(currentYear)
        snapshotOpt.isPresent()

        AnnualReportSnapshotVO snapshot = snapshotOpt.get()
        snapshot.getFiscalYear() == currentYear
        snapshot.getBeginningCashBalance() == openingBaseline
        snapshot.getTotalReceipts() == expectedReceipts
        snapshot.getTotalDisbursements() == expenseAmount
        snapshot.getEndingCashBalance() == expectedEndingBalance
        snapshot.getActiveMembersCount() == 7L // Members A, B, C, F, G, H, I were active during currentYear
        snapshot.getClosedBy() == "SYSTEM_SCHEDULER"
        snapshot.getClosedAt() != null

        and: "7. JSON Report Content: Fully populated and verified including registrationFeeSummary and combinedSummary"
        AnnualFinancialReportDto reportDto = objectMapper.readValue(snapshot.getReportData(), AnnualFinancialReportDto.class)
        reportDto.getYear() == currentYear
        reportDto.getIsClosed()
        reportDto.getSnapshotStatus() == "OFFICIAL_CLOSED_SNAPSHOT"
        reportDto.getBeginningCashBalance() == openingBaseline
        reportDto.getTotalReceipts() == expectedReceipts
        reportDto.getTotalDisbursements() == expenseAmount
        reportDto.getEndingCashBalance() == expectedEndingBalance

        // Membership dues compliance:
        // Q1: 6 active (A, B, F, G, H, I), Q2: 5 active (A, B, F, H, I), Q3: 7 active, Q4: 7 active = 25 member-quarters * $60 = $1,500.00
        reportDto.getComplianceSummary() != null
        reportDto.getComplianceSummary().getAnnualExpectedDues() == new BigDecimal("1500.00")
        reportDto.getComplianceSummary().getActualDuesCollected() == new BigDecimal("780.00")
        reportDto.getComplianceSummary().getTotalOverdueDues() == new BigDecimal("720.00")
        reportDto.getReceiptsByFeeType().find { it.getFeeType() == "Prior Year Arrears" }.getTotal() == quarterlyFee

        // Registration fee compliance: Member H (Q1) and Member C (Q3) = 2 new members; Charter Member I excluded
        reportDto.getComplianceSummary().getRegistrationFeeSummary() != null
        reportDto.getComplianceSummary().getRegistrationFeeSummary().getNewMembersCount() == 2L
        reportDto.getComplianceSummary().getRegistrationFeeSummary().getQ1NewMembers() == 1L
        reportDto.getComplianceSummary().getRegistrationFeeSummary().getQ2NewMembers() == 0L
        reportDto.getComplianceSummary().getRegistrationFeeSummary().getQ3NewMembers() == 1L
        reportDto.getComplianceSummary().getRegistrationFeeSummary().getQ4NewMembers() == 0L
        reportDto.getComplianceSummary().getRegistrationFeeSummary().getExpectedRegistrationFees() == new BigDecimal("100.00")
        reportDto.getComplianceSummary().getRegistrationFeeSummary().getActualRegistrationFeesCollected() == new BigDecimal("50.00")
        reportDto.getComplianceSummary().getRegistrationFeeSummary().getCollectionEfficiencyRate() == 50.0d
        reportDto.getComplianceSummary().getRegistrationFeeSummary().getTotalOverdueRegistrationFees() == new BigDecimal("50.00")

        // Combined summary: $1,500 + $100 = $1,600 expected; $780 + $50 = $830 collected; $720 + $50 = $770 overdue
        reportDto.getComplianceSummary().getCombinedSummary() != null
        reportDto.getComplianceSummary().getCombinedSummary().getTotalExpectedAmount() == new BigDecimal("1600.00")
        reportDto.getComplianceSummary().getCombinedSummary().getTotalActualCollected() == new BigDecimal("830.00")
        reportDto.getComplianceSummary().getCombinedSummary().getTotalOverdueAmount() == new BigDecimal("770.00")

        and: "8. Subsequent Queries: Serves frozen currentYear snapshot in O(1) time and serves live nextYear report with rolled-forward baseline and 6 active members"
        AnnualFinancialReportDto retrievedClosedReport = annualReportService.getAnnualReport(currentYear)
        retrievedClosedReport.getEndingCashBalance() == expectedEndingBalance
        retrievedClosedReport.getSnapshotStatus() == "OFFICIAL_CLOSED_SNAPSHOT"

        AnnualFinancialReportDto liveNextYearReport = annualReportService.getAnnualReport(nextYear)
        !liveNextYearReport.getIsClosed()
        liveNextYearReport.getSnapshotStatus() == "LIVE_PROJECTION"
        liveNextYearReport.getBeginningCashBalance() == expectedEndingBalance
        liveNextYearReport.getComplianceSummary().getActiveMembersCount() == 6L // Member A deactivated

        DashboardMetricsDto dashboardMetrics = dashboardService.getMetrics()
        dashboardMetrics.getTotalMembers() == 6L

        and: "9. Idempotency: Re-running executeYearEndProcess(currentYear) executes safely without duplicates or errors"
        when: "Year-end process is executed a second time"
        yearEndScheduler.executeYearEndProcess(currentYear)

        then: "No exception is thrown and baseline remains unchanged"
        noExceptionThrown()
        baselineService.getBaselineForYear(nextYear) == expectedEndingBalance
        annualReportSnapshotRepository.count() == 1

        and: "10. Closed-Year Mutation Lockout: Adding, updating, or deleting payment or expense in currentYear is blocked"
        when: "Attempting to record a new payment for closed currentYear"
        payment.addPayment(PaymentDto.builder()
                .memberID(memberB.getMemberID())
                .feeType(ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE)
                .amount(quarterlyFee)
                .dateReceived(LocalDate.of(currentYear, 5, 1))
                .methodOfPayment(ReferenceConstants.PAYMENT_METHOD.ZELLE)
                .year(currentYear)
                .quarter(2)
                .build())

        then: "Blocked with validation error"
        thrown(ValidationException)

        when: "Attempting to update an existing payment in closed currentYear"
        payment.updatePayment(PaymentDto.builder()
                .paymentID(paymentBQ1.getPaymentID())
                .memberID(memberB.getMemberID())
                .feeType(ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE)
                .amount(quarterlyFee)
                .dateReceived(LocalDate.of(currentYear, 1, 20))
                .methodOfPayment(ReferenceConstants.PAYMENT_METHOD.ZELLE)
                .year(currentYear)
                .quarter(1)
                .build())

        then: "Blocked with validation error"
        thrown(ValidationException)

        when: "Attempting to delete an existing payment in closed currentYear"
        payment.deletePayment(paymentBQ1.getPaymentID())

        then: "Blocked with validation error"
        thrown(ValidationException)

        when: "Attempting to record an expense for closed currentYear"
        expense.addExpense(ExpenseDto.builder()
                .amount(new BigDecimal("50.00"))
                .dateOfExpense(LocalDate.of(currentYear, 8, 1))
                .category(ReferenceConstants.EXPENSE_CATEGORY.OFFICE_SUPPLIES)
                .description("Late expense")
                .build())

        then: "Blocked with validation error"
        thrown(ValidationException)

        when: "Attempting to update an existing expense in closed currentYear"
        expense.updateExpense(ExpenseDto.builder()
                .expenseID(savedExpense.getExpenseID())
                .amount(new BigDecimal("120.00"))
                .dateOfExpense(LocalDate.of(currentYear, 6, 20))
                .category(ReferenceConstants.EXPENSE_CATEGORY.OFFICE_SUPPLIES)
                .description("Modified expense")
                .build())

        then: "Blocked with validation error"
        thrown(ValidationException)

        when: "Attempting to delete an existing expense in closed currentYear"
        expense.deleteExpense(savedExpense.getExpenseID())

        then: "Blocked with validation error"
        thrown(ValidationException)
    }

    def "Integration Test: Self-healing recovery when year is partially closed (baseline exists without snapshot)"() {
        given: "currentYear has a delinquent member and nextYear baseline was created, but snapshot is missing"
        int currentYear = LocalDate.now().getYear()
        int previousYear = currentYear - 1
        LocalDate prevYearStart = LocalDate.of(previousYear, 1, 1)
        LocalDate currentYearEnd = LocalDate.of(currentYear, 12, 31)

        MemberVO delinquentMember = memberRepository.save(new MemberVO(
                firstName: "Delinquent",
                lastName: "RecoveryMember",
                phone: "612-555-0099",
                email: "recovery@test.com",
                status: ReferenceConstants.MEMBER_STATUS.ACTIVE,
                joinDate: prevYearStart,
                state: "MN"
        ))
        MemberMembershipPeriodVO period = memberMembershipPeriodRepository.save(MemberMembershipPeriodVO.builder()
                .memberID(delinquentMember.getMemberID())
                .startDate(prevYearStart)
                .endDate(null)
                .build())

        baselineService.closeYear(currentYear)
        assert !yearEndScheduler.isYearFullyClosed(currentYear)
        assert yearEndScheduler.isYearEligibleForClosing(currentYear)

        when: "executeYearEndProcess(currentYear) runs to heal the partial close"
        yearEndScheduler.executeYearEndProcess(currentYear)

        then: "Delinquent member is deactivated, snapshot is frozen, and year is now marked fully closed"
        yearEndScheduler.isYearFullyClosed(currentYear)
        !yearEndScheduler.isYearEligibleForClosing(currentYear)
        memberRepository.findById(delinquentMember.getMemberID()).get().getStatus() == ReferenceConstants.MEMBER_STATUS.INACTIVE
        memberMembershipPeriodRepository.findById(period.getPeriodID()).get().getEndDate() == currentYearEnd
        annualReportSnapshotRepository.existsById(currentYear)
    }

    def "Integration Test: Admin manual close endpoint logic blocks current/future year and closes eligible past year"() {
        given: "An opening baseline for completed past year (previousYear)"
        int currentYear = LocalDate.now().getYear()
        int previousYear = currentYear - 1
        int preInauguralYear = currentYear - 6
        BigDecimal prevYearBaseline = new BigDecimal("40000.00")

        systemSettingRepository.save(new SystemSettingsVO(
                settingName: BaselineConstants.NAME_BASELINE,
                settingKey: BaselineConstants.getYearlyBaselineKey(previousYear),
                settingValue: prevYearBaseline.toString(),
                effectiveDate: LocalDate.of(previousYear, 1, 1),
                active: true
        ))
        systemSettingRepository.findAll().each { vo ->
            if (vo.settingKey == BaselineConstants.getYearlyBaselineKey(currentYear)) {
                systemSettingRepository.delete(vo)
            }
        }
        clearCaches()

        when: "Admin attempts to manually close the current open year"
        yearEndScheduler.closeFiscalYearManually(currentYear)

        then: "Blocked with ValidationException"
        thrown(ValidationException)

        when: "Admin attempts to manually close a pre-inaugural year with no baseline"
        yearEndScheduler.closeFiscalYearManually(preInauguralYear)

        then: "Blocked with ValidationException"
        thrown(ValidationException)

        when: "Admin manually triggers close for completed past year (previousYear)"
        AnnualFinancialReportDto closedPreviousYear = yearEndScheduler.closeFiscalYearManually(previousYear)

        then: "previousYear is closed, currentYear baseline is created, and frozen snapshot is returned"
        closedPreviousYear != null
        closedPreviousYear.getYear() == previousYear
        closedPreviousYear.getIsClosed()
        closedPreviousYear.getSnapshotStatus() == "OFFICIAL_CLOSED_SNAPSHOT"
        closedPreviousYear.getEndingCashBalance() == prevYearBaseline
        yearEndScheduler.isYearFullyClosed(previousYear)
    }
}