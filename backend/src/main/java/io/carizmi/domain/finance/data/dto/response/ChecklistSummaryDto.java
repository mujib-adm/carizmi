package io.carizmi.domain.finance.data.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistSummaryDto {
    private BigDecimal totalPaid;
    private BigDecimal totalBalance;
    private List<QuarterSummaryDto> quarterSummaries;
}