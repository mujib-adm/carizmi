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
public class AnnualComplianceSummaryDto {
    private Long activeMembersCount;
    private BigDecimal quarterlyFeeRate;
    private BigDecimal annualExpectedDues;
    private BigDecimal actualDuesCollected;
    private Double collectionEfficiencyRate;
    private BigDecimal totalOverdueDues;

    private AnnualMembershipFeeComplianceDto membershipFeeSummary;
    private AnnualRegistrationFeeComplianceDto registrationFeeSummary;
    private AnnualCombinedComplianceDto combinedSummary;
}