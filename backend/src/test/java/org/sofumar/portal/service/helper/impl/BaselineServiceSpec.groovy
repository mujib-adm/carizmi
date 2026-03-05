package org.sofumar.portal.service.helper.impl

import org.sofumar.portal.constants.BaselineConstants
import org.sofumar.portal.core.businesslogic.Expense
import org.sofumar.portal.core.businesslogic.Payment
import org.sofumar.portal.core.businesslogic.SystemSetting
import org.sofumar.portal.core.vo.SystemSettingsVO
import org.sofumar.portal.framework.exception.DuplicateRecordException

import org.sofumar.portal.testbase.BaseSpecification
import spock.lang.Subject

import java.time.LocalDate

class BaselineServiceSpec extends BaseSpecification {

    SystemSetting systemSetting = Mock()
    Payment payment = Mock()
    Expense expense = Mock()

    @Subject
    BaselineServiceImpl baselineService = new BaselineServiceImpl(systemSetting, payment, expense)

    int currentYear = LocalDate.now().getYear()
    int specialYear = 2026 // Keeping this literal as it tests a specific hardcoded branch in logic

    def "test - getBaselineForYear: Should return existing snapshot if present"() {
        given: "A year and an existing baseline snapshot"
        int year = currentYear - 1
        BigDecimal expectedValue = new BigDecimal("1500.00")
        SystemSettingsVO snapshot = new SystemSettingsVO(settingValue: expectedValue.toString())


        when: "The target method executed"
        BigDecimal result = baselineService.getBaselineForYear(year)

        then: "The expected calls are made"
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(year)) >> Optional.of(snapshot)
        0 * _

        and: "The existing value is returned without calculation"
        result == expectedValue
        noExceptionThrown()
    }

    def "test - getBaselineForYear: Should return and save hardcoded value for 2026 if snapshot missing"() {
        given: "Year 2026 (special) and no existing snapshot"
        int year = specialYear
        BigDecimal expectedBaseline = new BigDecimal("59863.68")

        SystemSettingsVO savedVO

        when: "The target method executed"
        BigDecimal result = baselineService.getBaselineForYear(year)

        then: "The expected calls are made"
        2 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(year)) >> Optional.empty()
        1 * systemSetting.add(_ as SystemSettingsVO) >> { SystemSettingsVO vo -> savedVO = vo; vo }
        0 * _

        and: "The expected result"
        result == expectedBaseline
        savedVO != null
        savedVO.settingValue == expectedBaseline.toString()
        savedVO.settingKey == BaselineConstants.getYearlyBaselineKey(specialYear)
        savedVO.effectiveDate == LocalDate.of(specialYear, 1, 1)
        noExceptionThrown()
    }

    def "test - getBaselineForYear: Should calculate dynamically, return, and save for other years if snapshot missing"() {
        given: "A year other than 2026 with no snapshot"
        int year = currentYear + 1 // Ensure clean future year
        // Avoid collision if currentYear + 1 is specialYear
        if (year == specialYear) year = year + 1

        LocalDate expectedDate = LocalDate.of(year, 1, 1)
        BigDecimal payments = new BigDecimal("5000.00")
        BigDecimal expenses = new BigDecimal("2000.00")
        BigDecimal expectedBaseline = payments - expenses

        SystemSettingsVO savedVO

        when: "The target method executed"
        BigDecimal result = baselineService.getBaselineForYear(year)

        then: "The expected calls are made"
        2 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(year)) >> Optional.empty()
        1 * systemSetting.findBySettingKey(BaselineConstants.KEY_SEED) >> Optional.empty()
        1 * payment.sumAmountByDateReceivedBefore(expectedDate) >> payments
        1 * expense.sumAmountByDateOfExpenseBefore(expectedDate) >> expenses
        1 * systemSetting.add(_ as SystemSettingsVO) >> { SystemSettingsVO vo -> savedVO = vo; vo }
        0 * _

        and: "The expected result"
        result == expectedBaseline
        savedVO != null
        savedVO.settingValue == expectedBaseline.toString()
        savedVO.settingKey == BaselineConstants.getYearlyBaselineKey(year)
        noExceptionThrown()
    }

    def "test - getBaselineForYear: Should include seed amount in dynamic calculation"() {
        given: "A year and a seed amount configured"
        int year = currentYear - 1
        if (year == specialYear) year = year - 1

        LocalDate expectedDate = LocalDate.of(year, 1, 1)
        BigDecimal seedAmount = new BigDecimal("10000.00")
        BigDecimal payments = new BigDecimal("100.00")
        BigDecimal expenses = new BigDecimal("50.00")
        BigDecimal expectedBaseline = seedAmount + payments - expenses

        SystemSettingsVO savedVO

        when: "The target method executed"
        BigDecimal result = baselineService.getBaselineForYear(year)

        then: "The expected calls are made"
        2 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(year)) >> Optional.empty()
        1 * systemSetting.findBySettingKey(BaselineConstants.KEY_SEED) >> Optional.of(new SystemSettingsVO(settingValue: seedAmount.toString()))
        1 * payment.sumAmountByDateReceivedBefore(expectedDate) >> payments
        1 * expense.sumAmountByDateOfExpenseBefore(expectedDate) >> expenses
        1 * systemSetting.add(_ as SystemSettingsVO) >> { SystemSettingsVO vo -> savedVO = vo; vo }
        0 * _

        and: "The expected result"
        result == expectedBaseline
        savedVO != null
        savedVO.settingValue == expectedBaseline.toString()
        savedVO.settingKey == BaselineConstants.getYearlyBaselineKey(year)
        noExceptionThrown()
    }

    def "test - getBaselineForYear: Should handle null returns from repositories during calculation"() {
        given: "A year requiring calculation"
        int year = currentYear - 2
        if (year == specialYear) year = year - 1

        LocalDate expectedDate = LocalDate.of(year, 1, 1)
        BigDecimal expectedBaseline = BigDecimal.ZERO

        SystemSettingsVO savedVO

        when: "The target method executed"
        BigDecimal result = baselineService.getBaselineForYear(year)

        then: "The expected calls are made"
        2 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(year)) >> Optional.empty()
        1 * systemSetting.findBySettingKey(BaselineConstants.KEY_SEED) >> Optional.empty()
        1 * payment.sumAmountByDateReceivedBefore(expectedDate) >> null
        1 * expense.sumAmountByDateOfExpenseBefore(expectedDate) >> null
        1 * systemSetting.add(_ as SystemSettingsVO) >> { SystemSettingsVO vo -> savedVO = vo; vo }
        0 * _

        and: "The expected result"
        result == expectedBaseline
        savedVO != null
        savedVO.settingValue == expectedBaseline.toString()
        noExceptionThrown()
    }

    def "test - closeYear: Should calculate revenue for next year and save snapshot"() {
        given: "A year to close"
        int yearToClose = currentYear - 1
        int nextYear = yearToClose + 1
        LocalDate nextYearStart = LocalDate.of(nextYear, 1, 1)
        BigDecimal payments = new BigDecimal("200.00")
        BigDecimal expenses = new BigDecimal("50.00")
        BigDecimal expectedBaseline = payments - expenses

        SystemSettingsVO savedVO

        when: "The target method executed"
        baselineService.closeYear(yearToClose)

        then: "The expected calls are made"
        1 * systemSetting.findBySettingKey(BaselineConstants.KEY_SEED) >> Optional.empty()
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(nextYear)) >> Optional.empty()
        1 * payment.sumAmountByDateReceivedBefore(nextYearStart) >> payments
        1 * expense.sumAmountByDateOfExpenseBefore(nextYearStart) >> expenses
        1 * systemSetting.add(_ as SystemSettingsVO) >> { SystemSettingsVO vo -> savedVO = vo; vo }
        0 * _

        and: "The expected result"
        noExceptionThrown()
        savedVO != null
        savedVO.settingKey == BaselineConstants.getYearlyBaselineKey(nextYear)
        savedVO.settingValue == expectedBaseline.toString()
    }

    def "test - saveBaselineSnapshot: Should update existing entity if present"() {
        given: "A year where a snapshot key already exists but needs updating"
        int year = currentYear - 2
        SystemSettingsVO existingVo = new SystemSettingsVO(systemSettingsID: 1L, settingValue: "999.99")

        LocalDate expectedNextYearStart = LocalDate.of(year + 1, 1, 1)
        BigDecimal expectedBaseline = BigDecimal.ZERO

        SystemSettingsVO savedVO = null

        when: "The target method executed"
        baselineService.closeYear(year)

        then: "The expected calls are made"
        1 * systemSetting.findBySettingKey(BaselineConstants.KEY_SEED) >> Optional.empty()
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(year + 1)) >> Optional.of(existingVo)
        1 * payment.sumAmountByDateReceivedBefore(expectedNextYearStart) >> expectedBaseline
        1 * expense.sumAmountByDateOfExpenseBefore(expectedNextYearStart) >> expectedBaseline
        1 * systemSetting.update(_ as SystemSettingsVO) >> { SystemSettingsVO vo -> savedVO = vo; vo }
        0 * _

        and: "The expected result"
        savedVO == existingVo
        savedVO.settingValue == expectedBaseline.toString()
        savedVO.systemSettingsID == 1L
        noExceptionThrown()
    }

    def "test - saveBaselineSnapshot: DB Error"() {
        given: "A year to close where saving fails at the delegate service level"
        int year = 2025

        when: "The target method executed"
        baselineService.closeYear(year)

        then: "The expected calls are made"
        _ * systemSetting.findBySettingKey(_) >> Optional.empty()
        _ * payment.sumAmountByDateReceivedBefore(_) >> BigDecimal.ZERO
        _ * expense.sumAmountByDateOfExpenseBefore(_) >> BigDecimal.ZERO
        // Simulate SystemSetting throwing an unchecked exception (e.g. if it didn't suppress it, or some other runtime error)
        1 * systemSetting.add(_) >> { throw new RuntimeException("Unexpected Error") }
        0 * _

        and: "The exception propagates"
        thrown(RuntimeException)
    }

    def "test - saveBaselineSnapshot: Duplicate Handling"() {
        given: "A year to close where snapshot already exists"
        int year = 2025

        when: "The target method executed"
        baselineService.closeYear(year)

        then: "The expected calls are made"
        _ * systemSetting.findBySettingKey(_) >> Optional.empty()
        _ * payment.sumAmountByDateReceivedBefore(_) >> BigDecimal.ZERO
        _ * expense.sumAmountByDateOfExpenseBefore(_) >> BigDecimal.ZERO
        // Simulate SystemSetting throwing DuplicateRecordException
        1 * systemSetting.add(_) >> { throw new DuplicateRecordException(new SystemSettingsVO()) }
        0 * _

        and: "A DuplicateRecordException is thrown"
        thrown(DuplicateRecordException)
    }
}