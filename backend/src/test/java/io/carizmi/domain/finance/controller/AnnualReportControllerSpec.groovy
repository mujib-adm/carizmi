package io.carizmi.domain.finance.controller

import io.carizmi.domain.finance.data.dto.response.AnnualFinancialReportDto
import io.carizmi.domain.finance.service.AnnualReportService
import io.carizmi.domain.platform.scheduler.YearEndScheduler
import io.carizmi.framework.data.response.GlobalResponse
import io.carizmi.testbase.BaseSpecification
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Subject

import java.time.LocalDate

class AnnualReportControllerSpec extends BaseSpecification {

    AnnualReportService reportService = Mock()
    YearEndScheduler yearEndScheduler = Mock()

    @Subject
    AnnualReportController controller = new AnnualReportController(reportService, yearEndScheduler)

    def "test - getAnnualReport: Should delegate to service with given year and return okWithData"() {
        given: "A report year and mock report DTO"
        Integer year = 2026
        AnnualFinancialReportDto mockReport = AnnualFinancialReportDto.builder()
                .year(year)
                .generatedDate(LocalDate.now())
                .beginningCashBalance(new BigDecimal("55221.68"))
                .totalReceipts(new BigDecimal("24800.00"))
                .totalDisbursements(new BigDecimal("12350.50"))
                .netOperatingSurplus(new BigDecimal("12449.50"))
                .endingCashBalance(new BigDecimal("67671.18"))
                .build()

        when: "The target method is executed"
        ResponseEntity<GlobalResponse<AnnualFinancialReportDto>> response = controller.getAnnualReport(year)

        then: "The expected calls are made"
        1 * reportService.getAnnualReport(year) >> mockReport
        0 * _

        and: "The response is OK and contains the report data"
        response.statusCode == HttpStatus.OK
        response.body != null
        response.body.responseData.year == year
        response.body.responseData.beginningCashBalance == new BigDecimal("55221.68")
        response.body.responseData.endingCashBalance == new BigDecimal("67671.18")
    }

    def "test - getAnnualReport: Should delegate to service with null year when parameter omitted"() {
        given: "A null year parameter"
        AnnualFinancialReportDto mockReport = AnnualFinancialReportDto.builder()
                .year(2026)
                .build()

        when: "The target method is executed without year"
        ResponseEntity<GlobalResponse<AnnualFinancialReportDto>> response = controller.getAnnualReport(null)

        then: "The expected calls are made"
        1 * reportService.getAnnualReport(null) >> mockReport
        0 * _

        and: "The response is OK"
        response.statusCode == HttpStatus.OK
        response.body.responseData.year == 2026
    }

    def "test - getAnnualReport: Should propagate exception when service fails"() {
        when: "The target method is executed and service throws exception"
        controller.getAnnualReport(2026)

        then: "Exception is propagated"
        1 * reportService.getAnnualReport(2026) >> { throw new RuntimeException("Database error") }
        thrown(RuntimeException)
    }

    def "test - closeFiscalYear: Should delegate to yearEndScheduler and return okWithData with success message"() {
        given: "A completed fiscal year"
        Integer year = 2025
        AnnualFinancialReportDto closedReport = AnnualFinancialReportDto.builder()
                .year(year)
                .isClosed(true)
                .snapshotStatus("OFFICIAL_CLOSED_SNAPSHOT")
                .build()

        when: "closeFiscalYear is called"
        ResponseEntity<GlobalResponse<AnnualFinancialReportDto>> response = controller.closeFiscalYear(year)

        then: "Delegates to yearEndScheduler.closeFiscalYearManually"
        1 * yearEndScheduler.closeFiscalYearManually(year) >> closedReport
        0 * _

        and: "Returns OK with frozen report and confirmation message"
        response.statusCode == HttpStatus.OK
        response.body != null
        response.body.responseData.year == year
        response.body.responseData.isClosed
        response.body.globalMessages[0].message.contains("Fiscal year 2025 has been officially closed and frozen.")
    }
}