package io.carizmi.domain.platform.scheduler

import io.carizmi.domain.finance.data.dto.response.AnnualFinancialReportDto
import io.carizmi.domain.finance.service.AnnualReportService
import io.carizmi.domain.finance.service.AnnualReportSnapshot
import io.carizmi.domain.membership.service.Member
import io.carizmi.domain.platform.constants.BaselineConstants
import io.carizmi.domain.platform.model.SystemSettingsVO
import io.carizmi.domain.platform.service.BaselineService
import io.carizmi.domain.platform.service.DashboardProjector
import io.carizmi.domain.platform.service.SystemSetting
import io.carizmi.framework.exception.ValidationException
import io.carizmi.testbase.BaseSpecification
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import spock.lang.Subject

import java.time.LocalDate
import java.time.ZoneId
import java.util.function.Consumer

class YearEndSchedulerSpec extends BaseSpecification {

    BaselineService baselineService = Mock()
    AnnualReportService annualReportService = Mock()
    Member member = Mock()
    SystemSetting systemSetting = Mock()
    AnnualReportSnapshot annualReportSnapshot = Mock()
    DashboardProjector dashboardProjector = Mock()
    TransactionTemplate transactionTemplate = Mock()

    @Subject
    YearEndScheduler scheduler = new YearEndScheduler(
            baselineService,
            annualReportService,
            member,
            systemSetting,
            annualReportSnapshot,
            dashboardProjector,
            transactionTemplate
    )

    def "test - executeYearEndClosingProcess: Should successfully close previous year based on business timezone"() {
        given: "Scheduler configured with America/Chicago timezone"
        scheduler.timezone = "America/Chicago"
        int expectedPreviousYear = LocalDate.now(ZoneId.of("America/Chicago")).getYear() - 1
        String closingKey = BaselineConstants.getYearlyBaselineKey(expectedPreviousYear + 1)

        when:
        scheduler.executeYearEndClosingProcess()

        then: "Checks if year is already closed, executes transactionally, and refreshes dashboard"
        1 * systemSetting.findBySettingKey(closingKey) >> Optional.empty()
        1 * transactionTemplate.executeWithoutResult(_ as Consumer) >> { Consumer<TransactionStatus> action ->
            action.accept(Mock(TransactionStatus))
        }
        1 * member.deactivateDelinquentMembers(expectedPreviousYear) >> 2
        1 * baselineService.closeYear(expectedPreviousYear)
        1 * annualReportService.generateAndFreezeSnapshot(expectedPreviousYear)
        1 * dashboardProjector.rebuildProjection()
        0 * _

        and:
        noExceptionThrown()
    }

    def "test - executeYearEndClosingProcess: Skips execution when previous year is already fully closed"() {
        given: "Scheduler where previous year is already closed and snapshot exists"
        scheduler.timezone = "America/Chicago"
        int expectedPreviousYear = LocalDate.now(ZoneId.of("America/Chicago")).getYear() - 1
        String closingKey = BaselineConstants.getYearlyBaselineKey(expectedPreviousYear + 1)

        when:
        scheduler.executeYearEndClosingProcess()

        then: "Exits early without re-running closing steps"
        1 * systemSetting.findBySettingKey(closingKey) >> Optional.of(new SystemSettingsVO())
        1 * annualReportSnapshot.existsByFiscalYear(expectedPreviousYear) >> true
        0 * _

        and:
        noExceptionThrown()
    }

    def "test - executeYearEndClosingProcess: Should use default timezone fallback when timezone property is null"() {
        given: "Scheduler with null timezone property"
        scheduler.timezone = null
        int expectedPreviousYear = LocalDate.now(ZoneId.of("America/Chicago")).getYear() - 1
        String closingKey = BaselineConstants.getYearlyBaselineKey(expectedPreviousYear + 1)

        when:
        scheduler.executeYearEndClosingProcess()

        then: "Defaults cleanly to America/Chicago, closes previous year and freezes snapshot"
        1 * systemSetting.findBySettingKey(closingKey) >> Optional.empty()
        1 * transactionTemplate.executeWithoutResult(_ as Consumer) >> { Consumer<TransactionStatus> action ->
            action.accept(Mock(TransactionStatus))
        }
        1 * member.deactivateDelinquentMembers(expectedPreviousYear) >> 0
        1 * baselineService.closeYear(expectedPreviousYear)
        1 * annualReportService.generateAndFreezeSnapshot(expectedPreviousYear)
        1 * dashboardProjector.rebuildProjection()
        0 * _

        and:
        noExceptionThrown()
    }

    def "test - executeYearEndClosingProcess: Retries transient failures and succeeds on subsequent attempt"() {
        given: "Scheduler where attempt 1 fails transiently and attempt 2 succeeds"
        scheduler.timezone = "America/Chicago"
        scheduler.maxAttempts = 3
        int expectedPreviousYear = LocalDate.now(ZoneId.of("America/Chicago")).getYear() - 1
        String closingKey = BaselineConstants.getYearlyBaselineKey(expectedPreviousYear + 1)
        int attemptCounter = 0

        when:
        scheduler.executeYearEndClosingProcess()

        then: "Retries after first failure and completes on second attempt"
        1 * systemSetting.findBySettingKey(closingKey) >> Optional.empty()
        2 * transactionTemplate.executeWithoutResult(_ as Consumer) >> { Consumer<TransactionStatus> action ->
            action.accept(Mock(TransactionStatus))
        }
        2 * member.deactivateDelinquentMembers(expectedPreviousYear) >> {
            attemptCounter++
            if (attemptCounter == 1) {
                throw new RuntimeException("Transient connection reset")
            }
            return 1
        }
        1 * baselineService.closeYear(expectedPreviousYear)
        1 * annualReportService.generateAndFreezeSnapshot(expectedPreviousYear)
        1 * dashboardProjector.rebuildProjection()
        0 * _

        and:
        noExceptionThrown()
    }

    def "test - executeYearEndClosingProcess: Should catch and log exceptions after exhausting retries without killing scheduler thread"() {
        given: "Scheduler where all retry attempts fail"
        scheduler.timezone = "America/Chicago"
        scheduler.maxAttempts = 2
        int expectedPreviousYear = LocalDate.now(ZoneId.of("America/Chicago")).getYear() - 1
        String closingKey = BaselineConstants.getYearlyBaselineKey(expectedPreviousYear + 1)

        when:
        scheduler.executeYearEndClosingProcess()

        then: "All attempts fail and scheduler catches final exception gracefully"
        1 * systemSetting.findBySettingKey(closingKey) >> Optional.empty()
        2 * transactionTemplate.executeWithoutResult(_ as Consumer) >> { Consumer<TransactionStatus> action ->
            action.accept(Mock(TransactionStatus))
        }
        2 * member.deactivateDelinquentMembers(expectedPreviousYear) >> { throw new RuntimeException("Database unreachable") }
        0 * _

        and:
        noExceptionThrown()
    }

    def "test - executeYearEndClosingProcess: Should use default timezone fallback when timezone is empty or blank"() {
        given: "Scheduler with whitespace timezone property"
        scheduler.timezone = "   "
        int expectedPreviousYear = LocalDate.now(ZoneId.of("America/Chicago")).getYear() - 1
        String closingKey = BaselineConstants.getYearlyBaselineKey(expectedPreviousYear + 1)

        when:
        scheduler.executeYearEndClosingProcess()

        then: "Falls back to America/Chicago and executes closing"
        1 * systemSetting.findBySettingKey(closingKey) >> Optional.empty()
        1 * transactionTemplate.executeWithoutResult(_ as Consumer) >> { Consumer<TransactionStatus> action ->
            action.accept(Mock(TransactionStatus))
        }
        1 * member.deactivateDelinquentMembers(expectedPreviousYear) >> 0
        1 * baselineService.closeYear(expectedPreviousYear)
        1 * annualReportService.generateAndFreezeSnapshot(expectedPreviousYear)
        1 * dashboardProjector.rebuildProjection()
        0 * _

        and:
        noExceptionThrown()
    }

    def "test - reconcileUnclosedYears: Triggers catch-up closing when prior year has opening baseline but is not fully closed"() {
        given: "Prior year has opening baseline, closing baseline exists, but report snapshot is missing"
        scheduler.timezone = "America/Chicago"
        int previousYear = LocalDate.now(ZoneId.of("America/Chicago")).getYear() - 1
        String openingKey = BaselineConstants.getYearlyBaselineKey(previousYear)
        String closingKey = BaselineConstants.getYearlyBaselineKey(previousYear + 1)

        when:
        scheduler.reconcileUnclosedYears()

        then: "Detects incomplete close and executes complete rollover"
        1 * systemSetting.findBySettingKey(openingKey) >> Optional.of(new SystemSettingsVO())
        1 * systemSetting.findBySettingKey(closingKey) >> Optional.of(new SystemSettingsVO())
        1 * annualReportSnapshot.existsByFiscalYear(previousYear) >> false
        1 * transactionTemplate.executeWithoutResult(_ as Consumer) >> { Consumer<TransactionStatus> action ->
            action.accept(Mock(TransactionStatus))
        }
        1 * member.deactivateDelinquentMembers(previousYear) >> 1
        1 * baselineService.closeYear(previousYear)
        1 * annualReportService.generateAndFreezeSnapshot(previousYear)
        1 * dashboardProjector.rebuildProjection()
        0 * _

        and:
        noExceptionThrown()
    }

    def "test - reconcileUnclosedYears: Skips closing when prior year has no opening baseline (pre-inaugural year)"() {
        given: "Prior year is before the organization's inaugural baseline year"
        scheduler.timezone = "America/Chicago"
        int previousYear = LocalDate.now(ZoneId.of("America/Chicago")).getYear() - 1
        String openingKey = BaselineConstants.getYearlyBaselineKey(previousYear)

        when:
        scheduler.reconcileUnclosedYears()

        then: "Does not trigger closing"
        1 * systemSetting.findBySettingKey(openingKey) >> Optional.empty()
        0 * _

        and:
        noExceptionThrown()
    }

    def "test - reconcileUnclosedYears: Skips closing when prior year is already fully closed"() {
        given: "Prior year has opening baseline and is already fully closed"
        scheduler.timezone = "America/Chicago"
        int previousYear = LocalDate.now(ZoneId.of("America/Chicago")).getYear() - 1
        String openingKey = BaselineConstants.getYearlyBaselineKey(previousYear)
        String closingKey = BaselineConstants.getYearlyBaselineKey(previousYear + 1)

        when:
        scheduler.reconcileUnclosedYears()

        then: "Checks eligibility and exits cleanly"
        1 * systemSetting.findBySettingKey(openingKey) >> Optional.of(new SystemSettingsVO())
        1 * systemSetting.findBySettingKey(closingKey) >> Optional.of(new SystemSettingsVO())
        1 * annualReportSnapshot.existsByFiscalYear(previousYear) >> true
        0 * _

        and:
        noExceptionThrown()
    }

    def "test - reconcileUnclosedYears: Catches and logs exception when catch-up fails after retries"() {
        given: "Prior year is eligible for closing but database fails on all attempts"
        scheduler.timezone = "America/Chicago"
        scheduler.maxAttempts = 1
        int previousYear = LocalDate.now(ZoneId.of("America/Chicago")).getYear() - 1
        String openingKey = BaselineConstants.getYearlyBaselineKey(previousYear)
        String closingKey = BaselineConstants.getYearlyBaselineKey(previousYear + 1)

        when:
        scheduler.reconcileUnclosedYears()

        then: "Exception is caught and logged without crashing startup"
        1 * systemSetting.findBySettingKey(openingKey) >> Optional.of(new SystemSettingsVO())
        1 * systemSetting.findBySettingKey(closingKey) >> Optional.empty()
        1 * transactionTemplate.executeWithoutResult(_ as Consumer) >> { Consumer<TransactionStatus> action ->
            action.accept(Mock(TransactionStatus))
        }
        1 * member.deactivateDelinquentMembers(previousYear) >> { throw new RuntimeException("DB offline") }
        0 * _

        and:
        noExceptionThrown()
    }

    def "test - executeYearEndProcess: Executes complete sequence for specified year without TransactionTemplate"() {
        given: "A scheduler instance with null TransactionTemplate"
        YearEndScheduler directScheduler = new YearEndScheduler(
                baselineService,
                annualReportService,
                member,
                systemSetting,
                annualReportSnapshot,
                dashboardProjector,
                null
        )
        int targetYear = 2026

        when: "executeYearEndProcess is called directly"
        directScheduler.executeYearEndProcess(targetYear)

        then: "Cascading operations are called in exact sequence"
        1 * member.deactivateDelinquentMembers(targetYear) >> 3
        1 * baselineService.closeYear(targetYear)
        1 * annualReportService.generateAndFreezeSnapshot(targetYear)
        1 * dashboardProjector.rebuildProjection()
        0 * _

        and:
        noExceptionThrown()
    }

    def "test - executeYearEndProcess: Logs warning and succeeds if post-commit dashboard rebuild fails"() {
        given: "A target year where dashboardProjector throws an exception"
        int targetYear = 2026

        when: "executeYearEndProcess is called"
        scheduler.executeYearEndProcess(targetYear)

        then: "Core closing steps commit and dashboard error is logged as warning"
        1 * transactionTemplate.executeWithoutResult(_ as Consumer) >> { Consumer<TransactionStatus> action ->
            action.accept(Mock(TransactionStatus))
        }
        1 * member.deactivateDelinquentMembers(targetYear) >> 1
        1 * baselineService.closeYear(targetYear)
        1 * annualReportService.generateAndFreezeSnapshot(targetYear)
        1 * dashboardProjector.rebuildProjection() >> { throw new RuntimeException("Dashboard projection error") }
        0 * _

        and:
        noExceptionThrown()
    }

    def "test - executeYearEndProcess: Propagates exception when a core step fails"() {
        given: "A target year and failing baselineService"
        int targetYear = 2026

        when: "executeYearEndProcess is called"
        scheduler.executeYearEndProcess(targetYear)

        then: "Exception is propagated to caller and dashboard rebuild is not invoked"
        1 * transactionTemplate.executeWithoutResult(_ as Consumer) >> { Consumer<TransactionStatus> action ->
            action.accept(Mock(TransactionStatus))
        }
        1 * member.deactivateDelinquentMembers(targetYear) >> 0
        1 * baselineService.closeYear(targetYear) >> { throw new IllegalStateException("Baseline calculation failed") }
        0 * annualReportService.generateAndFreezeSnapshot(_)
        0 * dashboardProjector.rebuildProjection()
        0 * _

        and:
        thrown(IllegalStateException)
    }

    def "test - closeFiscalYearManually: Executes closing process and returns frozen report for valid past year"() {
        given: "A valid past fiscal year with an opening baseline"
        scheduler.timezone = "America/Chicago"
        int previousYear = LocalDate.now(ZoneId.of("America/Chicago")).getYear() - 1
        String openingKey = BaselineConstants.getYearlyBaselineKey(previousYear)
        AnnualFinancialReportDto expectedReport = AnnualFinancialReportDto.builder()
                .year(previousYear)
                .isClosed(true)
                .snapshotStatus("OFFICIAL_CLOSED_SNAPSHOT")
                .build()

        when: "closeFiscalYearManually is invoked"
        AnnualFinancialReportDto result = scheduler.closeFiscalYearManually(previousYear)

        then: "Validates baseline, executes rollover, and returns frozen report"
        1 * systemSetting.findBySettingKey(openingKey) >> Optional.of(new SystemSettingsVO())
        1 * transactionTemplate.executeWithoutResult(_ as Consumer) >> { Consumer<TransactionStatus> action ->
            action.accept(Mock(TransactionStatus))
        }
        1 * member.deactivateDelinquentMembers(previousYear) >> 1
        1 * baselineService.closeYear(previousYear)
        1 * annualReportService.generateAndFreezeSnapshot(previousYear)
        1 * dashboardProjector.rebuildProjection()
        1 * annualReportService.getAnnualReport(previousYear) >> expectedReport
        0 * _

        and:
        result == expectedReport
    }

    def "test - closeFiscalYearManually: Throws ValidationException when attempting to close current or future year"() {
        given: "Current fiscal year"
        scheduler.timezone = "America/Chicago"
        int currentYear = LocalDate.now(ZoneId.of("America/Chicago")).getYear()

        when: "closeFiscalYearManually is invoked for current year"
        scheduler.closeFiscalYearManually(currentYear)

        then: "ValidationException is thrown immediately"
        0 * _
        thrown(ValidationException)
    }

    def "test - closeFiscalYearManually: Throws ValidationException when past year has no opening baseline"() {
        given: "A pre-inaugural past year with no opening baseline"
        scheduler.timezone = "America/Chicago"
        int preInauguralYear = LocalDate.now(ZoneId.of("America/Chicago")).getYear() - 5
        String openingKey = BaselineConstants.getYearlyBaselineKey(preInauguralYear)

        when: "closeFiscalYearManually is invoked"
        scheduler.closeFiscalYearManually(preInauguralYear)

        then: "ValidationException is thrown"
        1 * systemSetting.findBySettingKey(openingKey) >> Optional.empty()
        0 * _
        thrown(ValidationException)
    }
}