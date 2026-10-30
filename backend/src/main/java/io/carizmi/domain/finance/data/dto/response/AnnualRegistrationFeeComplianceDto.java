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
public class AnnualRegistrationFeeComplianceDto {
    private BigDecimal registrationFeeRate;
    private Long newMembersCount;
    private Long q1NewMembers;
    private Long q2NewMembers;
    private Long q3NewMembers;
    private Long q4NewMembers;
    private BigDecimal expectedRegistrationFees;
    private BigDecimal actualRegistrationFeesCollected;
    private Double collectionEfficiencyRate;
    private BigDecimal totalOverdueRegistrationFees;
}