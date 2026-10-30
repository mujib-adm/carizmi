package io.carizmi.domain.finance.data.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnnualCombinedComplianceDto {
    private BigDecimal totalExpectedAmount;
    private BigDecimal totalActualCollected;
    private Double overallCollectionEfficiencyRate;
    private BigDecimal totalOverdueAmount;
}