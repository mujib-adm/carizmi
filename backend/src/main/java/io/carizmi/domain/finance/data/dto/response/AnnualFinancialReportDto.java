package io.carizmi.domain.finance.data.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnnualFinancialReportDto {
    private Integer year;
    private LocalDate generatedDate;
    private String baselineKey;
    private BigDecimal beginningCashBalance;
    private BigDecimal totalReceipts;
    private BigDecimal totalDisbursements;
    private BigDecimal netOperatingSurplus;
    private BigDecimal endingCashBalance;
    private List<AnnualFeeTypeReceiptDto> receiptsByFeeType;
    private List<AnnualMethodReceiptDto> receiptsByPaymentMethod;
    private List<AnnualExpenseCategoryDto> disbursementsByCategory;
    private AnnualComplianceSummaryDto complianceSummary;
    private Boolean isClosed;
    private String snapshotStatus;
}