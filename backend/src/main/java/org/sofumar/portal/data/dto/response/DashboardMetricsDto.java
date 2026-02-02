package org.sofumar.portal.data.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetricsDto {
    private long totalMembers;
    private BigDecimal totalRevenue;
    private BigDecimal duesThisQuarter;
    private BigDecimal overdueTotal;
    private BigDecimal quarterlyFeeAmt;
    private List<QuarterlyCollectionDto> quarterlyCollections;
}