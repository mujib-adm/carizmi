package io.carizmi.domain.platform.service

import io.carizmi.domain.platform.service.impl.BaselineServiceImpl
import io.carizmi.domain.platform.constants.BaselineConstants
import io.carizmi.domain.finance.service.Expense
import io.carizmi.domain.finance.service.Payment
import io.carizmi.domain.platform.service.SystemSetting
import io.carizmi.domain.platform.model.SystemSettingsVO
import io.carizmi.framework.exception.DuplicateRecordException
import io.carizmi.testbase.BaseSpecification
import spock.lang.Subject

import java.time.LocalDate

class BaselineServiceSpec extends BaseSpecification {

    SystemSetting systemSetting = Mock()
    Payment payment = Mock()
    Expense expense = Mock()

    @Subject
    BaselineServiceImpl baselineService = new BaselineServiceImpl(systemSetting, payment, expense)

    int currentYear = LocalDate.now().getYear()

    def "test - getBaselineForYear: Should return existing snapshot if present directly"() {
        given: "A year with an existing baseline snapshot"
        int year = 2026
        BigDecimal expectedValue = new BigDecimal("55221.68")
        SystemSettingsVO snapshot = new SystemSettingsVO(
                settingKey: BaselineConstants.getYearlyBaselineKey(year),
                settingValue: expectedValue.toString()
        )

        when: "The target method executed"
        BigDecimal result = baselineService.getBaselineForYear(year)

        then: "The expected calls are made"
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(year)) >> Optional.of(snapshot)
        0 * _

        and: "Result matches snapshot value"
        result == expectedValue
        noExceptionThrown()
    }

    def "test - getBaselineForYear: Should roll forward dynamically from prior year when current snapshot missing"() {
        given: "Year 2027 with missing snapshot, but year 2026 has a snapshot"
        int targetYear = 2027
        int priorYear = 2026
        BigDecimal priorBaseline = new BigDecimal("55221.68")
        SystemSettingsVO priorSnapshot = new SystemSettingsVO(
                settingKey: BaselineConstants.getYearlyBaselineKey(priorYear),
                settingValue: priorBaseline.toString()
        )

        LocalDate startOfPriorYear = LocalDate.of(priorYear, 1, 1)
        LocalDate endOfPriorYear = LocalDate.of(priorYear, 12, 31)
        BigDecimal receipts = new BigDecimal("12000.00")
        BigDecimal disbursements = new BigDecimal("4500.00")
        BigDecimal expectedBaseline = priorBaseline.add(receipts).subtract(disbursements)

        when: "getBaselineForYear is called for 2027"
        BigDecimal result = baselineService.getBaselineForYear(targetYear)

        then: "It checks 2027 (empty), finds 2026 snapshot, queries receipts/disbursements between Jan 1 and Dec 31 of 2026, and returns dynamic roll-forward without persisting"
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(targetYear)) >> Optional.empty()
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(priorYear)) >> Optional.of(priorSnapshot)
        1 * payment.sumAmountByDateReceivedBetween(startOfPriorYear, endOfPriorYear) >> receipts
        1 * expense.sumAmountByDateOfExpenseBetween(startOfPriorYear, endOfPriorYear) >> disbursements
        0 * _

        and: "Computed baseline is returned without prematurely closing the prior year"
        result == expectedBaseline
    }

    def "test - getBaselineForYear: Should return ZERO if snapshot and prior year snapshot both missing"() {
        given: "A year before inception where no baseline exists"
        int targetYear = 2020
        int priorYear = 2019

        when: "getBaselineForYear is called"
        BigDecimal result = baselineService.getBaselineForYear(targetYear)

        then: "Both current and prior snapshots are missing"
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(targetYear)) >> Optional.empty()
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(priorYear)) >> Optional.empty()
        0 * _

        and: "Defaults to ZERO without persisting"
        result == BigDecimal.ZERO
    }

    def "test - getBaselineForYear: Null safety when prior year payments or expenses return null"() {
        given: "Prior year exists but has null receipts and disbursements"
        int targetYear = 2027
        int priorYear = 2026
        BigDecimal priorBaseline = new BigDecimal("50000.00")
        SystemSettingsVO priorSnapshot = new SystemSettingsVO(
                settingKey: BaselineConstants.getYearlyBaselineKey(priorYear),
                settingValue: priorBaseline.toString()
        )

        LocalDate startOfPriorYear = LocalDate.of(priorYear, 1, 1)
        LocalDate endOfPriorYear = LocalDate.of(priorYear, 12, 31)

        when: "getBaselineForYear is executed"
        BigDecimal result = baselineService.getBaselineForYear(targetYear)

        then: "Nulls are safely coerced to ZERO"
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(targetYear)) >> Optional.empty()
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(priorYear)) >> Optional.of(priorSnapshot)
        1 * payment.sumAmountByDateReceivedBetween(startOfPriorYear, endOfPriorYear) >> null
        1 * expense.sumAmountByDateOfExpenseBetween(startOfPriorYear, endOfPriorYear) >> null
        0 * _

        and: "Result equals prior baseline"
        result == priorBaseline
    }

    def "test - closeYear: Should calculate ending balance of closed year and save snapshot for next year"() {
        given: "A year to close"
        int yearToClose = 2026
        int nextYear = 2027
        BigDecimal beginningBalance = new BigDecimal("55221.68")
        SystemSettingsVO snapshot2026 = new SystemSettingsVO(
                settingKey: BaselineConstants.getYearlyBaselineKey(yearToClose),
                settingValue: beginningBalance.toString()
        )

        LocalDate startOfYear = LocalDate.of(yearToClose, 1, 1)
        LocalDate endOfYear = LocalDate.of(yearToClose, 12, 31)
        BigDecimal payments = new BigDecimal("8500.00")
        BigDecimal expenses = new BigDecimal("2100.00")
        BigDecimal expectedEndingBalance = beginningBalance.add(payments).subtract(expenses)

        SystemSettingsVO savedVO = null

        when: "closeYear is executed"
        baselineService.closeYear(yearToClose)

        then: "Retrieves beginning balance, sums payments and expenses strictly between Jan 1 and Dec 31, and saves next year's baseline"
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(yearToClose)) >> Optional.of(snapshot2026)
        1 * payment.sumAmountByDateReceivedBetween(startOfYear, endOfYear) >> payments
        1 * expense.sumAmountByDateOfExpenseBetween(startOfYear, endOfYear) >> expenses
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(nextYear)) >> Optional.empty()
        1 * systemSetting.add(_ as SystemSettingsVO) >> { SystemSettingsVO vo -> savedVO = vo; vo }
        0 * _

        and: "Snapshot for next year is created with exact ending balance"
        savedVO != null
        savedVO.settingKey == BaselineConstants.getYearlyBaselineKey(nextYear)
        savedVO.settingValue == expectedEndingBalance.toString()
        savedVO.effectiveDate == LocalDate.of(nextYear, 1, 1)
        savedVO.active == true
    }

    def "test - closeYear: Null safety for payments and expenses during closing"() {
        given: "A year to close where repository returns null"
        int yearToClose = 2026
        int nextYear = 2027
        BigDecimal beginningBalance = new BigDecimal("1000.00")
        SystemSettingsVO snapshot = new SystemSettingsVO(
                settingKey: BaselineConstants.getYearlyBaselineKey(yearToClose),
                settingValue: beginningBalance.toString()
        )

        LocalDate startOfYear = LocalDate.of(yearToClose, 1, 1)
        LocalDate endOfYear = LocalDate.of(yearToClose, 12, 31)
        SystemSettingsVO savedVO = null

        when: "closeYear is executed"
        baselineService.closeYear(yearToClose)

        then: "Null payments and expenses default to ZERO"
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(yearToClose)) >> Optional.of(snapshot)
        1 * payment.sumAmountByDateReceivedBetween(startOfYear, endOfYear) >> null
        1 * expense.sumAmountByDateOfExpenseBetween(startOfYear, endOfYear) >> null
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(nextYear)) >> Optional.empty()
        1 * systemSetting.add(_ as SystemSettingsVO) >> { SystemSettingsVO vo -> savedVO = vo; vo }
        0 * _

        and: "Ending balance equals beginning balance"
        savedVO != null
        savedVO.settingValue == beginningBalance.toString()
    }

    def "test - closeYear: Should update existing entity if snapshot for next year is already present"() {
        given: "A year to close where next year snapshot already exists (e.g. re-closing / rerun)"
        int yearToClose = 2026
        int nextYear = 2027
        BigDecimal beginningBalance = new BigDecimal("55221.68")
        SystemSettingsVO snapshot2026 = new SystemSettingsVO(
                settingKey: BaselineConstants.getYearlyBaselineKey(yearToClose),
                settingValue: beginningBalance.toString()
        )
        SystemSettingsVO existingNextYearVo = new SystemSettingsVO(
                systemSettingsID: 42L,
                settingKey: BaselineConstants.getYearlyBaselineKey(nextYear),
                settingValue: "999.00"
        )

        LocalDate startOfYear = LocalDate.of(yearToClose, 1, 1)
        LocalDate endOfYear = LocalDate.of(yearToClose, 12, 31)
        BigDecimal payments = new BigDecimal("5000.00")
        BigDecimal expenses = new BigDecimal("1000.00")
        BigDecimal expectedEndingBalance = beginningBalance.add(payments).subtract(expenses)

        SystemSettingsVO updatedVO = null

        when: "closeYear is executed"
        baselineService.closeYear(yearToClose)

        then: "Entity is updated instead of added"
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(yearToClose)) >> Optional.of(snapshot2026)
        1 * payment.sumAmountByDateReceivedBetween(startOfYear, endOfYear) >> payments
        1 * expense.sumAmountByDateOfExpenseBetween(startOfYear, endOfYear) >> expenses
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(nextYear)) >> Optional.of(existingNextYearVo)
        1 * systemSetting.update(_ as SystemSettingsVO) >> { SystemSettingsVO vo -> updatedVO = vo; vo }
        0 * _

        and: "Updated VO retains original ID and new value"
        updatedVO != null
        updatedVO.systemSettingsID == 42L
        updatedVO.settingValue == expectedEndingBalance.toString()
    }

    def "test - saveBaselineSnapshot: DB Error propagates exception"() {
        given: "A year to close where saving fails"
        int year = 2026
        SystemSettingsVO snapshot = new SystemSettingsVO(settingValue: "100.00")

        when: "closeYear is executed"
        baselineService.closeYear(year)

        then: "DB error from systemSetting is thrown"
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(year)) >> Optional.of(snapshot)
        1 * payment.sumAmountByDateReceivedBetween(_, _) >> BigDecimal.ZERO
        1 * expense.sumAmountByDateOfExpenseBetween(_, _) >> BigDecimal.ZERO
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(year + 1)) >> Optional.empty()
        1 * systemSetting.add(_) >> { throw new RuntimeException("DB Connection Timeout") }
        0 * _

        and: "Exception is propagated"
        thrown(RuntimeException)
    }

    def "test - saveBaselineSnapshot: DuplicateRecordException propagates"() {
        given: "A year to close where duplicate record is reported"
        int year = 2026
        SystemSettingsVO snapshot = new SystemSettingsVO(settingValue: "100.00")

        when: "closeYear is executed"
        baselineService.closeYear(year)

        then: "DuplicateRecordException propagates"
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(year)) >> Optional.of(snapshot)
        1 * payment.sumAmountByDateReceivedBetween(_, _) >> BigDecimal.ZERO
        1 * expense.sumAmountByDateOfExpenseBetween(_, _) >> BigDecimal.ZERO
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(year + 1)) >> Optional.empty()
        1 * systemSetting.add(_) >> { throw new DuplicateRecordException(new SystemSettingsVO()) }
        0 * _

        and: "DuplicateRecordException is thrown"
        thrown(DuplicateRecordException)
    }
}