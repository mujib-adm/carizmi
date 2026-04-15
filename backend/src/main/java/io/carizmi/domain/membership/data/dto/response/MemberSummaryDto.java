package io.carizmi.domain.membership.data.dto.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberSummaryDto {
    private BigDecimal totalPaid;
    private BigDecimal outstanding;
    private BigDecimal overdue;
}