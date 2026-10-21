package io.carizmi.domain.finance.service;

import io.carizmi.domain.finance.data.dto.response.AnnualFinancialReportDto;

public interface AnnualReportService {
    AnnualFinancialReportDto getAnnualReport(Integer year);
}