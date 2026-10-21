package io.carizmi.domain.platform.scheduler

import io.carizmi.domain.platform.service.BaselineService
import io.carizmi.testbase.BaseSpecification
import spock.lang.Subject

import java.time.LocalDate
import java.time.ZoneId

class YearEndSchedulerSpec extends BaseSpecification {

    BaselineService baselineService = Mock()

    @Subject
    YearEndScheduler scheduler = new YearEndScheduler(baselineService)

    def "test - captureYearEndBaseline: Should successfully close previous year based on business timezone"() {
        given: "Scheduler configured with America/Chicago timezone"
        scheduler.timezone = "America/Chicago"
        int expectedPreviousYear = LocalDate.now(ZoneId.of("America/Chicago")).minusDays(1).getYear()

        when: "captureYearEndBaseline is executed"
        scheduler.captureYearEndBaseline()

        then: "baselineService.closeYear is called with the expected year"
        1 * baselineService.closeYear(expectedPreviousYear)
        0 * _

        and:
        noExceptionThrown()
    }

    def "test - captureYearEndBaseline: Should use default timezone fallback when timezone property is null"() {
        given: "Scheduler with null timezone property"
        scheduler.timezone = null
        int expectedPreviousYear = LocalDate.now(ZoneId.of("America/Chicago")).minusDays(1).getYear()

        when: "captureYearEndBaseline is executed"
        scheduler.captureYearEndBaseline()

        then: "Defaults cleanly to America/Chicago and closes previous year"
        1 * baselineService.closeYear(expectedPreviousYear)
        0 * _

        and:
        noExceptionThrown()
    }

    def "test - captureYearEndBaseline: Should catch and log exceptions without killing scheduler thread"() {
        given: "Scheduler where closeYear fails with an unexpected exception"
        scheduler.timezone = "America/Chicago"
        int expectedPreviousYear = LocalDate.now(ZoneId.of("America/Chicago")).minusDays(1).getYear()

        when: "captureYearEndBaseline is executed"
        scheduler.captureYearEndBaseline()

        then: "baselineService throws an exception, which scheduler catches gracefully"
        1 * baselineService.closeYear(expectedPreviousYear) >> { throw new RuntimeException("Database unreachable") }
        0 * _

        and:
        noExceptionThrown()
    }
}