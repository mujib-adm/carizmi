package io.carizmi.domain.finance.service

import io.carizmi.domain.finance.model.AnnualReportSnapshotVO
import io.carizmi.domain.membership.model.MemberVO
import io.carizmi.domain.finance.data.dto.projection.ExpenseCategoryTotalProjection
import io.carizmi.domain.finance.data.dto.projection.FeeTypeReceiptBreakdownProjection
import io.carizmi.domain.finance.data.dto.projection.PaymentMethodTotalProjection
import io.carizmi.domain.finance.data.dto.response.AnnualFeeTypeReceiptDto
import io.carizmi.domain.finance.data.dto.response.AnnualFinancialReportDto
import io.carizmi.domain.finance.service.impl.AnnualReportServiceImpl
import io.carizmi.domain.membership.service.Member
import io.carizmi.domain.platform.constants.BaselineConstants
import io.carizmi.domain.platform.service.BaselineService
import io.carizmi.domain.platform.service.SystemSetting
import io.carizmi.shared.constants.ReferenceConstants
import io.carizmi.testbase.BaseSpecification
import spock.lang.Subject
import tools.jackson.databind.ObjectMapper

import java.time.LocalDate

class AnnualReportServiceSpec extends BaseSpecification {

    BaselineService baselineService = Mock()
    Payment payment = Mock()
    Expense expense = Mock()
    Member member = Mock()
    SystemSetting systemSetting = Mock()
    AnnualReportSnapshot annualReportSnapshot = Mock()
    ObjectMapper objectMapper = Mock()

    @Subject
    AnnualReportServiceImpl reportService = new AnnualReportServiceImpl(baselineService, payment, expense, member, systemSetting, annualReportSnapshot, objectMapper)

    def "test - getAnnualReport: Full report generation for specified year"() {
        given: "A specified year"
        int year = 2026
        LocalDate startOfYear = LocalDate.of(year, 1, 1)
        LocalDate endOfYear = LocalDate.of(year, 12, 31)

        BigDecimal beginningBalance = new BigDecimal("55221.68")
        BigDecimal totalReceipts = new BigDecimal("24800.00")
        BigDecimal totalDisbursements = new BigDecimal("12350.50")
        BigDecimal quarterlyFeeAmt = new BigDecimal("60.00")
        BigDecimal registrationFeeAmt = new BigDecimal("50.00")

        FeeTypeReceiptBreakdownProjection feeProj1 = Stub(FeeTypeReceiptBreakdownProjection) {
            getFeeType() >> ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE
            getObligationYear() >> year
            getReceivedQuarter() >> 1
            getFeeQuarter() >> 1
            getTotalAmount() >> new BigDecimal("5200.00")
        }
        FeeTypeReceiptBreakdownProjection feeProj2 = Stub(FeeTypeReceiptBreakdownProjection) {
            getFeeType() >> ReferenceConstants.FEE_TYPE.REGISTRATION_FEE
            getObligationYear() >> year
            getReceivedQuarter() >> 2
            getFeeQuarter() >> 2
            getTotalAmount() >> new BigDecimal("50.00")
        }

        PaymentMethodTotalProjection methodProj1 = Stub(PaymentMethodTotalProjection) {
            getMethodOfPayment() >> ReferenceConstants.PAYMENT_METHOD.ZELLE
            getTotalAmount() >> new BigDecimal("19840.00")
        }

        ExpenseCategoryTotalProjection expenseProj1 = Stub(ExpenseCategoryTotalProjection) {
            getCategory() >> ReferenceConstants.EXPENSE_CATEGORY.COMMUNITY_PICNIC
            getTotalAmount() >> new BigDecimal("4820.00")
        }

        List<MemberVO> memberList = (1..24).collect { new MemberVO(joinDate: LocalDate.of(2026, 1, 1)) } + [new MemberVO(joinDate: LocalDate.of(2026, 4, 26))]

        when: "getAnnualReport is executed"
        AnnualFinancialReportDto report = reportService.getAnnualReport(year)

        then: "Expected calls are made to services and repositories"
        1 * annualReportSnapshot.findByFiscalYear(year) >> Optional.empty()
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(year + 1)) >> Optional.empty()
        1 * baselineService.getBaselineForYear(year) >> beginningBalance
        1 * payment.sumAmountByDateReceivedBetween(startOfYear, endOfYear) >> totalReceipts
        1 * expense.sumAmountByDateOfExpenseBetween(startOfYear, endOfYear) >> totalDisbursements
        1 * payment.findFeeTypeReceiptBreakdownsBetween(startOfYear, endOfYear) >> [feeProj1, feeProj2]
        1 * payment.findPaymentMethodTotalsBetween(startOfYear, endOfYear) >> [methodProj1]
        1 * payment.findMemberIDsByFeeTypeAndDateReceivedBetween(ReferenceConstants.FEE_TYPE.REGISTRATION_FEE, startOfYear, endOfYear) >> []
        1 * expense.findExpenseCategoryTotalsBetween(startOfYear, endOfYear) >> [expenseProj1]
        1 * member.findAllMembersActiveInYear(year) >> memberList
        1 * systemSetting.getQuarterlyFeeAmount() >> quarterlyFeeAmt
        1 * systemSetting.getRegistrationFeeAmount() >> registrationFeeAmt
        0 * _

        and: "Report fields and financial reconciliations match expected calculations"
        report.year == year
        report.generatedDate == LocalDate.now()
        report.baselineKey == BaselineConstants.getYearlyBaselineKey(year)
        report.beginningCashBalance == beginningBalance
        report.totalReceipts == totalReceipts
        report.totalDisbursements == totalDisbursements
        report.netOperatingSurplus == new BigDecimal("12449.50")
        report.endingCashBalance == new BigDecimal("67671.18")

        and: "Receipts by fee type are populated"
        report.receiptsByFeeType.size() >= 2
        AnnualFeeTypeReceiptDto membershipRow = report.receiptsByFeeType.find { it.feeType == ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE }
        membershipRow != null
        membershipRow.q1 == new BigDecimal("5200.00")
        membershipRow.total == new BigDecimal("5200.00")

        and: "Receipts by method and disbursements by category percentages are calculated"
        report.receiptsByPaymentMethod.size() == 1
        report.receiptsByPaymentMethod[0].methodOfPayment == ReferenceConstants.PAYMENT_METHOD.ZELLE
        report.receiptsByPaymentMethod[0].totalAmount == new BigDecimal("19840.00")
        report.receiptsByPaymentMethod[0].percentage == 80.0

        report.disbursementsByCategory.size() == 1
        report.disbursementsByCategory[0].category == ReferenceConstants.EXPENSE_CATEGORY.COMMUNITY_PICNIC
        report.disbursementsByCategory[0].totalAmount == new BigDecimal("4820.00")
        report.disbursementsByCategory[0].percentage == 39.0

        and: "Membership fee compliance summary reflects join-date quarter eligibility"
        report.complianceSummary != null
        report.complianceSummary.membershipFeeSummary != null
        report.complianceSummary.membershipFeeSummary.activeMembersCount == 25L
        report.complianceSummary.membershipFeeSummary.quarterlyFeeRate == quarterlyFeeAmt
        report.complianceSummary.membershipFeeSummary.q1EligibleMembers == 24L
        report.complianceSummary.membershipFeeSummary.q2EligibleMembers == 25L
        report.complianceSummary.membershipFeeSummary.q3EligibleMembers == 25L
        report.complianceSummary.membershipFeeSummary.q4EligibleMembers == 25L
        // (24 + 25 + 25 + 25) * 60 = 99 * 60 = 5940.00
        report.complianceSummary.membershipFeeSummary.annualExpectedDues == new BigDecimal("5940.00")
        report.complianceSummary.membershipFeeSummary.actualDuesCollected == new BigDecimal("5200.00")
        report.complianceSummary.membershipFeeSummary.totalOverdueDues == new BigDecimal("740.00")
        report.complianceSummary.membershipFeeSummary.collectionEfficiencyRate == 87.5

        and: "Registration fee compliance summary tracks newly joined members and excludes 2026-01-01 charter members"
        report.complianceSummary.registrationFeeSummary != null
        report.complianceSummary.registrationFeeSummary.registrationFeeRate == registrationFeeAmt
        report.complianceSummary.registrationFeeSummary.newMembersCount == 1L
        report.complianceSummary.registrationFeeSummary.q1NewMembers == 0L // 2026-01-01 ignored!
        report.complianceSummary.registrationFeeSummary.q2NewMembers == 1L
        report.complianceSummary.registrationFeeSummary.q3NewMembers == 0L
        report.complianceSummary.registrationFeeSummary.q4NewMembers == 0L
        report.complianceSummary.registrationFeeSummary.expectedRegistrationFees == new BigDecimal("50.00")
        report.complianceSummary.registrationFeeSummary.actualRegistrationFeesCollected == new BigDecimal("50.00")
        report.complianceSummary.registrationFeeSummary.totalOverdueRegistrationFees == BigDecimal.ZERO
        report.complianceSummary.registrationFeeSummary.collectionEfficiencyRate == 100.0

        and: "Combined compliance summary sums all fee inflows"
        report.complianceSummary.combinedSummary != null
        report.complianceSummary.combinedSummary.totalExpectedAmount == new BigDecimal("5990.00") // 5940 + 50
        report.complianceSummary.combinedSummary.totalActualCollected == new BigDecimal("5250.00") // 5200 + 50
        report.complianceSummary.combinedSummary.overallCollectionEfficiencyRate == 87.6
        report.complianceSummary.combinedSummary.totalOverdueAmount == new BigDecimal("740.00") // 740 + 0

        and: "Top-level backward compatible fields match membership totals"
        report.complianceSummary.activeMembersCount == 25L
        report.complianceSummary.quarterlyFeeRate == quarterlyFeeAmt
        report.complianceSummary.annualExpectedDues == new BigDecimal("5940.00")
        report.complianceSummary.actualDuesCollected == new BigDecimal("5200.00")
        report.complianceSummary.totalOverdueDues == new BigDecimal("740.00")
        report.complianceSummary.collectionEfficiencyRate == 87.5
    }

    def "test - getAnnualReport: Defaults to current year when input year is null"() {
        given: "A null year input"
        int currentYear = LocalDate.now().getYear()
        LocalDate startOfYear = LocalDate.of(currentYear, 1, 1)
        LocalDate endOfYear = LocalDate.of(currentYear, 12, 31)

        when: "getAnnualReport is executed with null"
        AnnualFinancialReportDto report = reportService.getAnnualReport(null)

        then: "Current year is used"
        1 * annualReportSnapshot.findByFiscalYear(currentYear) >> Optional.empty()
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(currentYear + 1)) >> Optional.empty()
        1 * baselineService.getBaselineForYear(currentYear) >> BigDecimal.ZERO
        1 * payment.sumAmountByDateReceivedBetween(startOfYear, endOfYear) >> BigDecimal.ZERO
        1 * expense.sumAmountByDateOfExpenseBetween(startOfYear, endOfYear) >> BigDecimal.ZERO
        1 * payment.findFeeTypeReceiptBreakdownsBetween(startOfYear, endOfYear) >> []
        1 * payment.findPaymentMethodTotalsBetween(startOfYear, endOfYear) >> []
        1 * payment.findMemberIDsByFeeTypeAndDateReceivedBetween(ReferenceConstants.FEE_TYPE.REGISTRATION_FEE, startOfYear, endOfYear) >> []
        1 * expense.findExpenseCategoryTotalsBetween(startOfYear, endOfYear) >> []
        1 * member.findAllMembersActiveInYear(currentYear) >> []
        1 * systemSetting.getQuarterlyFeeAmount() >> BigDecimal.ZERO
        1 * systemSetting.getRegistrationFeeAmount() >> BigDecimal.ZERO
        0 * _

        and: "Result contains current year and zero balances"
        report.year == currentYear
        report.beginningCashBalance == BigDecimal.ZERO
        report.totalReceipts == BigDecimal.ZERO
        report.totalDisbursements == BigDecimal.ZERO
        report.netOperatingSurplus == BigDecimal.ZERO
        report.endingCashBalance == BigDecimal.ZERO
        report.complianceSummary.collectionEfficiencyRate == 0.0
        report.complianceSummary.totalOverdueDues == BigDecimal.ZERO
        report.complianceSummary.registrationFeeSummary.newMembersCount == 0L
        report.complianceSummary.combinedSummary.totalExpectedAmount == BigDecimal.ZERO
    }

    def "test - getAnnualReport: Null safety when query results return nulls"() {
        given: "A past year where queries return null"
        int pastYear = 2024
        LocalDate startOfYear = LocalDate.of(pastYear, 1, 1)
        LocalDate endOfYear = LocalDate.of(pastYear, 12, 31)

        when: "getAnnualReport is executed"
        AnnualFinancialReportDto report = reportService.getAnnualReport(pastYear)

        then: "Nulls are handled gracefully"
        1 * annualReportSnapshot.findByFiscalYear(pastYear) >> Optional.empty()
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(pastYear + 1)) >> Optional.empty()
        1 * baselineService.getBaselineForYear(pastYear) >> null
        1 * payment.sumAmountByDateReceivedBetween(startOfYear, endOfYear) >> null
        1 * expense.sumAmountByDateOfExpenseBetween(startOfYear, endOfYear) >> null
        1 * payment.findFeeTypeReceiptBreakdownsBetween(startOfYear, endOfYear) >> null
        1 * payment.findPaymentMethodTotalsBetween(startOfYear, endOfYear) >> null
        1 * payment.findMemberIDsByFeeTypeAndDateReceivedBetween(ReferenceConstants.FEE_TYPE.REGISTRATION_FEE, startOfYear, endOfYear) >> null
        1 * expense.findExpenseCategoryTotalsBetween(startOfYear, endOfYear) >> null
        1 * member.findAllMembersActiveInYear(pastYear) >> null
        1 * systemSetting.getQuarterlyFeeAmount() >> null
        1 * systemSetting.getRegistrationFeeAmount() >> { throw new RuntimeException("DB error") }
        0 * _

        and: "Balances are zero without throwing exceptions"
        report.beginningCashBalance == BigDecimal.ZERO
        report.totalReceipts == BigDecimal.ZERO
        report.totalDisbursements == BigDecimal.ZERO
        report.netOperatingSurplus == BigDecimal.ZERO
        report.endingCashBalance == BigDecimal.ZERO
        report.complianceSummary.totalOverdueDues == BigDecimal.ZERO
        report.complianceSummary.annualExpectedDues == BigDecimal.ZERO
        report.complianceSummary.collectionEfficiencyRate == 0.0
        report.complianceSummary.registrationFeeSummary.registrationFeeRate == BigDecimal.ZERO
        report.complianceSummary.combinedSummary.totalExpectedAmount == BigDecimal.ZERO
    }

    def "test - getAnnualReport: Handles custom fee type and edge quarter numbers"() {
        given: "A year with custom fee type and invalid quarter number in projection"
        int year = 2026
        LocalDate startOfYear = LocalDate.of(year, 1, 1)
        LocalDate endOfYear = LocalDate.of(year, 12, 31)

        FeeTypeReceiptBreakdownProjection projCustom = Stub(FeeTypeReceiptBreakdownProjection) {
            getFeeType() >> "DONATION"
            getObligationYear() >> year
            getReceivedQuarter() >> 3
            getFeeQuarter() >> 3
            getTotalAmount() >> new BigDecimal("300.00")
        }
        FeeTypeReceiptBreakdownProjection projInvalidQuarter = Stub(FeeTypeReceiptBreakdownProjection) {
            getFeeType() >> "OTHER"
            getObligationYear() >> year
            getReceivedQuarter() >> 5 // out of range
            getFeeQuarter() >> 5
            getTotalAmount() >> new BigDecimal("100.00")
        }
        FeeTypeReceiptBreakdownProjection projNullQuarter = Stub(FeeTypeReceiptBreakdownProjection) {
            getFeeType() >> null
            getObligationYear() >> null
            getReceivedQuarter() >> null
            getFeeQuarter() >> null
            getTotalAmount() >> null
        }

        when: "getAnnualReport is executed"
        AnnualFinancialReportDto report = reportService.getAnnualReport(year)

        then: "Projections are processed safely"
        1 * annualReportSnapshot.findByFiscalYear(year) >> Optional.empty()
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(year + 1)) >> Optional.empty()
        1 * baselineService.getBaselineForYear(year) >> BigDecimal.ZERO
        1 * payment.sumAmountByDateReceivedBetween(startOfYear, endOfYear) >> new BigDecimal("300.00")
        1 * expense.sumAmountByDateOfExpenseBetween(startOfYear, endOfYear) >> BigDecimal.ZERO
        1 * payment.findFeeTypeReceiptBreakdownsBetween(startOfYear, endOfYear) >> [projCustom, projInvalidQuarter, projNullQuarter]
        1 * payment.findPaymentMethodTotalsBetween(startOfYear, endOfYear) >> []
        1 * payment.findMemberIDsByFeeTypeAndDateReceivedBetween(ReferenceConstants.FEE_TYPE.REGISTRATION_FEE, startOfYear, endOfYear) >> []
        1 * expense.findExpenseCategoryTotalsBetween(startOfYear, endOfYear) >> []
        1 * member.findAllMembersActiveInYear(year) >> []
        1 * systemSetting.getQuarterlyFeeAmount() >> BigDecimal.ZERO
        1 * systemSetting.getRegistrationFeeAmount() >> BigDecimal.ZERO
        0 * _

        and: "DONATION row is present with Q3 populated"
        AnnualFeeTypeReceiptDto donationRow = report.receiptsByFeeType.find { it.feeType == "DONATION" }
        donationRow != null
        donationRow.q3 == new BigDecimal("300.00")
        donationRow.total == new BigDecimal("300.00")
        report.complianceSummary.totalOverdueDues == BigDecimal.ZERO
    }

    def "test - getAnnualReport: Overdue dues capped at zero when collected exceeds expected"() {
        given: "A year where collections exceed expected dues"
        int year = 2026
        LocalDate startOfYear = LocalDate.of(year, 1, 1)
        LocalDate endOfYear = LocalDate.of(year, 12, 31)

        FeeTypeReceiptBreakdownProjection feeProj = Stub(FeeTypeReceiptBreakdownProjection) {
            getFeeType() >> ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE
            getObligationYear() >> year
            getReceivedQuarter() >> 1
            getFeeQuarter() >> 1
            getTotalAmount() >> new BigDecimal("10000.00")
        }

        MemberVO memberVO = new MemberVO(joinDate: LocalDate.of(2025, 1, 1))

        when: "getAnnualReport is executed"
        AnnualFinancialReportDto report = reportService.getAnnualReport(year)

        then: "Interactions and calculations"
        1 * annualReportSnapshot.findByFiscalYear(year) >> Optional.empty()
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(year + 1)) >> Optional.empty()
        1 * baselineService.getBaselineForYear(year) >> BigDecimal.ZERO
        1 * payment.sumAmountByDateReceivedBetween(startOfYear, endOfYear) >> new BigDecimal("10000.00")
        1 * expense.sumAmountByDateOfExpenseBetween(startOfYear, endOfYear) >> BigDecimal.ZERO
        1 * payment.findFeeTypeReceiptBreakdownsBetween(startOfYear, endOfYear) >> [feeProj]
        1 * payment.findPaymentMethodTotalsBetween(startOfYear, endOfYear) >> []
        1 * payment.findMemberIDsByFeeTypeAndDateReceivedBetween(ReferenceConstants.FEE_TYPE.REGISTRATION_FEE, startOfYear, endOfYear) >> []
        1 * expense.findExpenseCategoryTotalsBetween(startOfYear, endOfYear) >> []
        1 * member.findAllMembersActiveInYear(year) >> [memberVO]
        1 * systemSetting.getQuarterlyFeeAmount() >> new BigDecimal("60.00")
        1 * systemSetting.getRegistrationFeeAmount() >> new BigDecimal("50.00")
        0 * _

        and: "Total overdue is zero"
        report.complianceSummary.annualExpectedDues == new BigDecimal("240.00")
        report.complianceSummary.actualDuesCollected == new BigDecimal("10000.00")
        report.complianceSummary.totalOverdueDues == BigDecimal.ZERO
    }

    def "test - getAnnualReport: Registration fee branch coverage for null rate and over-collection"() {
        given: "A year where registration fee setting is null and registration collections exceed expected"
        int year = 2026
        LocalDate startOfYear = LocalDate.of(year, 1, 1)
        LocalDate endOfYear = LocalDate.of(year, 12, 31)

        FeeTypeReceiptBreakdownProjection regFeeProj = Stub(FeeTypeReceiptBreakdownProjection) {
            getFeeType() >> ReferenceConstants.FEE_TYPE.REGISTRATION_FEE
            getObligationYear() >> year
            getReceivedQuarter() >> 1
            getFeeQuarter() >> 1
            getTotalAmount() >> new BigDecimal("100.00")
        }

        when: "getAnnualReport is executed"
        AnnualFinancialReportDto report = reportService.getAnnualReport(year)

        then: "Interactions and calculations"
        1 * annualReportSnapshot.findByFiscalYear(year) >> Optional.empty()
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(year + 1)) >> Optional.empty()
        1 * baselineService.getBaselineForYear(year) >> BigDecimal.ZERO
        1 * payment.sumAmountByDateReceivedBetween(startOfYear, endOfYear) >> new BigDecimal("100.00")
        1 * expense.sumAmountByDateOfExpenseBetween(startOfYear, endOfYear) >> BigDecimal.ZERO
        1 * payment.findFeeTypeReceiptBreakdownsBetween(startOfYear, endOfYear) >> [regFeeProj]
        1 * payment.findPaymentMethodTotalsBetween(startOfYear, endOfYear) >> []
        1 * payment.findMemberIDsByFeeTypeAndDateReceivedBetween(ReferenceConstants.FEE_TYPE.REGISTRATION_FEE, startOfYear, endOfYear) >> []
        1 * expense.findExpenseCategoryTotalsBetween(startOfYear, endOfYear) >> []
        1 * member.findAllMembersActiveInYear(year) >> []
        1 * systemSetting.getQuarterlyFeeAmount() >> BigDecimal.ZERO
        1 * systemSetting.getRegistrationFeeAmount() >> null
        0 * _

        and: "Registration fee defaults to zero rate and 100% efficiency when collected > 0 with 0 expected"
        report.complianceSummary.registrationFeeSummary.registrationFeeRate == BigDecimal.ZERO
        report.complianceSummary.registrationFeeSummary.expectedRegistrationFees == BigDecimal.ZERO
        report.complianceSummary.registrationFeeSummary.actualRegistrationFeesCollected == new BigDecimal("100.00")
        report.complianceSummary.registrationFeeSummary.collectionEfficiencyRate == 100.0
        report.complianceSummary.registrationFeeSummary.totalOverdueRegistrationFees == BigDecimal.ZERO
    }

    def "test - getAnnualReport: Option 1 Cash-Basis Inflow isolates Prior Year Arrears into dedicated row"() {
        given: "Fiscal year where late (year - 1) dues and advance (year + 1) dues are received"
        int year = LocalDate.now().year
        LocalDate startOfYear = LocalDate.of(year, 1, 1)
        LocalDate endOfYear = LocalDate.of(year, 12, 31)

        // (year) Current Year Membership Fee: $1,000 received in Q1 for Q1
        FeeTypeReceiptBreakdownProjection currentProj = Stub(FeeTypeReceiptBreakdownProjection) {
            getFeeType() >> ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE
            getObligationYear() >> year
            getReceivedQuarter() >> 1
            getFeeQuarter() >> 1
            getTotalAmount() >> new BigDecimal("1000.00")
        }
        // (year - 1) Late Arrears: $200 received in (year) Q2 for (year - 1) Q4
        FeeTypeReceiptBreakdownProjection arrearsProj = Stub(FeeTypeReceiptBreakdownProjection) {
            getFeeType() >> ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE
            getObligationYear() >> year - 1
            getReceivedQuarter() >> 2
            getFeeQuarter() >> 4
            getTotalAmount() >> new BigDecimal("200.00")
        }
        // (year + 1) Advance Dues: $100 received in (year) Q4 for (year + 1) Q1
        FeeTypeReceiptBreakdownProjection advanceProj = Stub(FeeTypeReceiptBreakdownProjection) {
            getFeeType() >> ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE
            getObligationYear() >> year + 1
            getReceivedQuarter() >> 4
            getFeeQuarter() >> 1
            getTotalAmount() >> new BigDecimal("100.00")
        }

        BigDecimal totalCashReceipts = new BigDecimal("1300.00")

        when: "getAnnualReport is generated for (year)"
        AnnualFinancialReportDto report = reportService.getAnnualReport(year)

        then: "Repository interactions"
        1 * annualReportSnapshot.findByFiscalYear(year) >> Optional.empty()
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(year + 1)) >> Optional.empty()
        1 * baselineService.getBaselineForYear(year) >> BigDecimal.ZERO
        1 * payment.sumAmountByDateReceivedBetween(startOfYear, endOfYear) >> totalCashReceipts
        1 * expense.sumAmountByDateOfExpenseBetween(startOfYear, endOfYear) >> BigDecimal.ZERO
        1 * payment.findFeeTypeReceiptBreakdownsBetween(startOfYear, endOfYear) >> [currentProj, arrearsProj, advanceProj]
        1 * payment.findPaymentMethodTotalsBetween(startOfYear, endOfYear) >> []
        1 * payment.findMemberIDsByFeeTypeAndDateReceivedBetween(ReferenceConstants.FEE_TYPE.REGISTRATION_FEE, startOfYear, endOfYear) >> []
        1 * expense.findExpenseCategoryTotalsBetween(startOfYear, endOfYear) >> []
        1 * member.findAllMembersActiveInYear(year) >> []
        1 * systemSetting.getQuarterlyFeeAmount() >> BigDecimal.ZERO
        1 * systemSetting.getRegistrationFeeAmount() >> BigDecimal.ZERO
        0 * _

        and: "Section 2 table isolates current year, prior year arrears, and prepaid dues"
        AnnualFeeTypeReceiptDto currentMembership = report.receiptsByFeeType.find { it.feeType == ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE }
        AnnualFeeTypeReceiptDto arrearsRow = report.receiptsByFeeType.find { it.feeType == "Prior Year Arrears" }
        AnnualFeeTypeReceiptDto advanceRow = report.receiptsByFeeType.find { it.feeType == "Prepaid Dues" }

        currentMembership != null
        currentMembership.q1 == new BigDecimal("1000.00")
        currentMembership.total == new BigDecimal("1000.00")

        arrearsRow != null
        arrearsRow.q2 == new BigDecimal("200.00")
        arrearsRow.total == new BigDecimal("200.00")

        advanceRow != null
        advanceRow.q4 == new BigDecimal("100.00")
        advanceRow.total == new BigDecimal("100.00")

        and: "Sum of Section 2 rows exactly equals totalReceipts (100% Cash-Basis reconciliation)"
        BigDecimal sumSection2 = currentMembership.total + arrearsRow.total + advanceRow.total
        sumSection2 == totalCashReceipts
        report.totalReceipts == totalCashReceipts
    }

    def "test - getAnnualReport: Serves frozen snapshot from repository with zero computation"() {
        given: "A closed year with snapshot present"
        int year = 2025
        AnnualFinancialReportDto cachedDto = AnnualFinancialReportDto.builder()
                .year(year)
                .totalReceipts(new BigDecimal("50000.00"))
                .isClosed(true)
                .snapshotStatus("OFFICIAL_CLOSED_SNAPSHOT")
                .build()
        String json = '{"year":2025,"totalReceipts":50000.00,"isClosed":true,"snapshotStatus":"OFFICIAL_CLOSED_SNAPSHOT"}'
        AnnualReportSnapshotVO snapshotVO = AnnualReportSnapshotVO.builder()
                .fiscalYear(year)
                .reportData(json)
                .build()

        when: "getAnnualReport is executed"
        AnnualFinancialReportDto report = reportService.getAnnualReport(year)

        then: "Served directly from snapshot"
        1 * annualReportSnapshot.findByFiscalYear(year) >> Optional.of(snapshotVO)
        1 * objectMapper.readValue(json, AnnualFinancialReportDto) >> cachedDto
        0 * _

        and:
        report.year == year
        report.totalReceipts == new BigDecimal("50000.00")
        report.isClosed
        report.snapshotStatus == "OFFICIAL_CLOSED_SNAPSHOT"
    }

    def "test - getAnnualReport: Closed year with missing snapshot generates and freezes snapshot"() {
        given: "A closed year where baseline exists but snapshot missing"
        int year = 2025
        LocalDate startOfYear = LocalDate.of(year, 1, 1)
        LocalDate endOfYear = LocalDate.of(year, 12, 31)

        when: "getAnnualReport is executed"
        AnnualFinancialReportDto report = reportService.getAnnualReport(year)

        then: "Delinquent members are deactivated, and snapshot is generated and persisted"
        1 * annualReportSnapshot.findByFiscalYear(year) >> Optional.empty()
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(year + 1)) >> Optional.of(new io.carizmi.domain.platform.model.SystemSettingsVO())
        1 * member.deactivateDelinquentMembers(year) >> 0
        1 * baselineService.getBaselineForYear(year) >> BigDecimal.ZERO
        1 * payment.sumAmountByDateReceivedBetween(startOfYear, endOfYear) >> BigDecimal.ZERO
        1 * expense.sumAmountByDateOfExpenseBetween(startOfYear, endOfYear) >> BigDecimal.ZERO
        1 * payment.findFeeTypeReceiptBreakdownsBetween(startOfYear, endOfYear) >> []
        1 * payment.findPaymentMethodTotalsBetween(startOfYear, endOfYear) >> []
        1 * payment.findMemberIDsByFeeTypeAndDateReceivedBetween(ReferenceConstants.FEE_TYPE.REGISTRATION_FEE, startOfYear, endOfYear) >> []
        1 * expense.findExpenseCategoryTotalsBetween(startOfYear, endOfYear) >> []
        1 * member.findAllMembersActiveInYear(year) >> []
        1 * systemSetting.getQuarterlyFeeAmount() >> BigDecimal.ZERO
        1 * systemSetting.getRegistrationFeeAmount() >> BigDecimal.ZERO
        1 * objectMapper.writeValueAsString(_ as AnnualFinancialReportDto) >> '{"year":2025,"isClosed":true}'
        1 * annualReportSnapshot.add(_ as AnnualReportSnapshotVO)
        0 * _

        and:
        report.year == year
        report.isClosed
        report.snapshotStatus == "OFFICIAL_CLOSED_SNAPSHOT"
    }

    def "test - generateAndFreezeSnapshot: Directly executes snapshot freezing"() {
        given: "A fiscal year"
        int year = 2026
        LocalDate startOfYear = LocalDate.of(year, 1, 1)
        LocalDate endOfYear = LocalDate.of(year, 12, 31)

        when: "generateAndFreezeSnapshot is executed"
        AnnualFinancialReportDto report = reportService.generateAndFreezeSnapshot(year)

        then: "Snapshot is saved"
        1 * annualReportSnapshot.findByFiscalYear(year) >> Optional.empty()
        1 * baselineService.getBaselineForYear(year) >> BigDecimal.ZERO
        1 * payment.sumAmountByDateReceivedBetween(startOfYear, endOfYear) >> BigDecimal.ZERO
        1 * expense.sumAmountByDateOfExpenseBetween(startOfYear, endOfYear) >> BigDecimal.ZERO
        1 * payment.findFeeTypeReceiptBreakdownsBetween(startOfYear, endOfYear) >> []
        1 * payment.findPaymentMethodTotalsBetween(startOfYear, endOfYear) >> []
        1 * payment.findMemberIDsByFeeTypeAndDateReceivedBetween(ReferenceConstants.FEE_TYPE.REGISTRATION_FEE, startOfYear, endOfYear) >> []
        1 * expense.findExpenseCategoryTotalsBetween(startOfYear, endOfYear) >> []
        1 * member.findAllMembersActiveInYear(year) >> []
        1 * systemSetting.getQuarterlyFeeAmount() >> BigDecimal.ZERO
        1 * systemSetting.getRegistrationFeeAmount() >> BigDecimal.ZERO
        1 * objectMapper.writeValueAsString(_ as AnnualFinancialReportDto) >> '{"year":2026,"isClosed":true}'
        1 * annualReportSnapshot.add(_ as AnnualReportSnapshotVO)
        0 * _

        and:
        report.year == year
        report.isClosed
        report.snapshotStatus == "OFFICIAL_CLOSED_SNAPSHOT"
    }

    def "test - generateAndFreezeSnapshot: Returns existing snapshot idempotently when already present"() {
        given: "A fiscal year with an already persisted snapshot"
        int year = 2026
        AnnualReportSnapshotVO existingSnapshot = AnnualReportSnapshotVO.builder()
                .fiscalYear(year)
                .reportData('{"year":2026,"isClosed":true}')
                .build()
        AnnualFinancialReportDto expectedReport = AnnualFinancialReportDto.builder()
                .year(year)
                .isClosed(true)
                .snapshotStatus("OFFICIAL_CLOSED_SNAPSHOT")
                .build()

        when: "generateAndFreezeSnapshot is executed"
        AnnualFinancialReportDto report = reportService.generateAndFreezeSnapshot(year)

        then: "Existing snapshot is returned without recomputing or overwriting"
        1 * annualReportSnapshot.findByFiscalYear(year) >> Optional.of(existingSnapshot)
        1 * objectMapper.readValue('{"year":2026,"isClosed":true}', AnnualFinancialReportDto.class) >> expectedReport
        0 * _

        and:
        report == expectedReport
    }

    def "test - generateAndFreezeSnapshot: Regenerates snapshot if existing snapshot fails deserialization"() {
        given: "A fiscal year with a corrupted existing snapshot"
        int year = 2026
        LocalDate startOfYear = LocalDate.of(year, 1, 1)
        LocalDate endOfYear = LocalDate.of(year, 12, 31)
        AnnualReportSnapshotVO corruptedSnapshot = AnnualReportSnapshotVO.builder()
                .fiscalYear(year)
                .reportData("corrupted-json")
                .build()

        when: "generateAndFreezeSnapshot is executed"
        AnnualFinancialReportDto report = reportService.generateAndFreezeSnapshot(year)

        then: "Falls back to computing and updating the existing snapshot"
        1 * annualReportSnapshot.findByFiscalYear(year) >> Optional.of(corruptedSnapshot)
        1 * objectMapper.readValue("corrupted-json", AnnualFinancialReportDto.class) >> { throw new RuntimeException("Bad JSON") }
        1 * baselineService.getBaselineForYear(year) >> BigDecimal.ZERO
        1 * payment.sumAmountByDateReceivedBetween(startOfYear, endOfYear) >> BigDecimal.ZERO
        1 * expense.sumAmountByDateOfExpenseBetween(startOfYear, endOfYear) >> BigDecimal.ZERO
        1 * payment.findFeeTypeReceiptBreakdownsBetween(startOfYear, endOfYear) >> []
        1 * payment.findPaymentMethodTotalsBetween(startOfYear, endOfYear) >> []
        1 * payment.findMemberIDsByFeeTypeAndDateReceivedBetween(ReferenceConstants.FEE_TYPE.REGISTRATION_FEE, startOfYear, endOfYear) >> []
        1 * expense.findExpenseCategoryTotalsBetween(startOfYear, endOfYear) >> []
        1 * member.findAllMembersActiveInYear(year) >> []
        1 * systemSetting.getQuarterlyFeeAmount() >> BigDecimal.ZERO
        1 * systemSetting.getRegistrationFeeAmount() >> BigDecimal.ZERO
        1 * objectMapper.writeValueAsString(_ as AnnualFinancialReportDto) >> '{"year":2026,"isClosed":true}'
        1 * annualReportSnapshot.update(_ as AnnualReportSnapshotVO)
        0 * _

        and:
        report.year == year
        report.isClosed
        report.snapshotStatus == "OFFICIAL_CLOSED_SNAPSHOT"
    }

    def "test - generateAndFreezeSnapshot: Throws IllegalStateException when serialization fails so transaction rolls back"() {
        given: "A fiscal year"
        int year = 2026
        LocalDate startOfYear = LocalDate.of(year, 1, 1)
        LocalDate endOfYear = LocalDate.of(year, 12, 31)

        when: "generateAndFreezeSnapshot is executed and objectMapper throws exception"
        reportService.generateAndFreezeSnapshot(year)

        then: "IllegalStateException is propagated to trigger transaction rollback"
        1 * annualReportSnapshot.findByFiscalYear(year) >> Optional.empty()
        1 * baselineService.getBaselineForYear(year) >> BigDecimal.ZERO
        1 * payment.sumAmountByDateReceivedBetween(startOfYear, endOfYear) >> BigDecimal.ZERO
        1 * expense.sumAmountByDateOfExpenseBetween(startOfYear, endOfYear) >> BigDecimal.ZERO
        1 * payment.findFeeTypeReceiptBreakdownsBetween(startOfYear, endOfYear) >> []
        1 * payment.findPaymentMethodTotalsBetween(startOfYear, endOfYear) >> []
        1 * payment.findMemberIDsByFeeTypeAndDateReceivedBetween(ReferenceConstants.FEE_TYPE.REGISTRATION_FEE, startOfYear, endOfYear) >> []
        1 * expense.findExpenseCategoryTotalsBetween(startOfYear, endOfYear) >> []
        1 * member.findAllMembersActiveInYear(year) >> []
        1 * systemSetting.getQuarterlyFeeAmount() >> BigDecimal.ZERO
        1 * systemSetting.getRegistrationFeeAmount() >> BigDecimal.ZERO
        1 * objectMapper.writeValueAsString(_ as AnnualFinancialReportDto) >> { throw new RuntimeException("Serialization failure") }
        0 * annualReportSnapshot.add(_)
        0 * _

        and:
        thrown(IllegalStateException)
    }

    def "test - getAnnualReport: Includes genuine new member registered in Q1 with 2026-01-01 joinDate who paid registration fee"() {
        given: "A year with 1 charter member (no reg fee) and 1 new member registered in Q1 (with reg fee)"
        int year = 2026
        LocalDate startOfYear = LocalDate.of(year, 1, 1)
        LocalDate endOfYear = LocalDate.of(year, 12, 31)

        MemberVO charterMember = new MemberVO(memberID: 1001, joinDate: LocalDate.of(2026, 1, 1))
        MemberVO newQ1Member = new MemberVO(memberID: 1109, joinDate: LocalDate.of(2026, 1, 1))

        FeeTypeReceiptBreakdownProjection regFeeProj = Stub(FeeTypeReceiptBreakdownProjection) {
            getFeeType() >> ReferenceConstants.FEE_TYPE.REGISTRATION_FEE
            getObligationYear() >> null
            getReceivedQuarter() >> 1
            getFeeQuarter() >> 1
            getTotalAmount() >> new BigDecimal("50.00")
        }

        when: "getAnnualReport is executed"
        AnnualFinancialReportDto report = reportService.getAnnualReport(year)

        then: "Interactions and expectations"
        1 * annualReportSnapshot.findByFiscalYear(year) >> Optional.empty()
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(year + 1)) >> Optional.empty()
        1 * baselineService.getBaselineForYear(year) >> BigDecimal.ZERO
        1 * payment.sumAmountByDateReceivedBetween(startOfYear, endOfYear) >> new BigDecimal("50.00")
        1 * expense.sumAmountByDateOfExpenseBetween(startOfYear, endOfYear) >> BigDecimal.ZERO
        1 * payment.findFeeTypeReceiptBreakdownsBetween(startOfYear, endOfYear) >> [regFeeProj]
        1 * payment.findPaymentMethodTotalsBetween(startOfYear, endOfYear) >> []
        1 * payment.findMemberIDsByFeeTypeAndDateReceivedBetween(ReferenceConstants.FEE_TYPE.REGISTRATION_FEE, startOfYear, endOfYear) >> [1109]
        1 * expense.findExpenseCategoryTotalsBetween(startOfYear, endOfYear) >> []
        1 * member.findAllMembersActiveInYear(year) >> [charterMember, newQ1Member]
        1 * member.findMembershipPeriodsForMembers([1001, 1109]) >> [:]
        1 * systemSetting.getQuarterlyFeeAmount() >> new BigDecimal("60.00")
        1 * systemSetting.getRegistrationFeeAmount() >> new BigDecimal("50.00")
        0 * _

        and: "Charter member is skipped while Q1 new member is included in registration fee compliance"
        report.complianceSummary.registrationFeeSummary != null
        report.complianceSummary.registrationFeeSummary.newMembersCount == 1L
        report.complianceSummary.registrationFeeSummary.q1NewMembers == 1L
        report.complianceSummary.registrationFeeSummary.q2NewMembers == 0L
        report.complianceSummary.registrationFeeSummary.expectedRegistrationFees == new BigDecimal("50.00")
        report.complianceSummary.registrationFeeSummary.actualRegistrationFeesCollected == new BigDecimal("50.00")
        report.complianceSummary.registrationFeeSummary.collectionEfficiencyRate == 100.0
        report.complianceSummary.registrationFeeSummary.totalOverdueRegistrationFees == BigDecimal.ZERO
    }
}
