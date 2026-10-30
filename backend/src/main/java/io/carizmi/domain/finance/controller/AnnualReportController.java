package io.carizmi.domain.finance.controller;

import io.carizmi.domain.finance.data.dto.response.AnnualFinancialReportDto;
import io.carizmi.domain.finance.service.AnnualReportService;
import io.carizmi.domain.platform.scheduler.YearEndScheduler;
import io.carizmi.framework.data.response.GlobalResponse;
import io.carizmi.framework.util.ResponseUtils;
import io.carizmi.infrastructure.security.annotation.IsAdmin;
import io.carizmi.infrastructure.security.annotation.IsAuthenticated;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static io.carizmi.shared.message.ValidationMessages.FISCAL_YEAR_CLOSED_SUCCESS;

@RestController
@RequestMapping("/reports")
@Tag(name = "Reports", description = "Financial and organizational report APIs")
@RequiredArgsConstructor
@IsAuthenticated
public class AnnualReportController {

    private final AnnualReportService reportService;
    private final YearEndScheduler yearEndScheduler;

    @GetMapping("/annual")
    @Operation(summary = "Generate official annual financial report")
    public ResponseEntity<GlobalResponse<AnnualFinancialReportDto>> getAnnualReport(
            @RequestParam(value = "year", required = false) Integer year) {
        AnnualFinancialReportDto report = reportService.getAnnualReport(year);
        return ResponseUtils.okWithData(report);
    }

    @PostMapping("/annual/{year}/close")
    @Operation(summary = "Manually trigger or retry year-end closing and snapshot freeze for a completed fiscal year")
    @IsAdmin
    public ResponseEntity<GlobalResponse<AnnualFinancialReportDto>> closeFiscalYear(
            @PathVariable("year") Integer year) {
        AnnualFinancialReportDto report = yearEndScheduler.closeFiscalYearManually(year);
        return ResponseUtils.okWithData(
                report,
                FISCAL_YEAR_CLOSED_SUCCESS.addMessageArgs(String.valueOf(year)).getMessageString());
    }
}