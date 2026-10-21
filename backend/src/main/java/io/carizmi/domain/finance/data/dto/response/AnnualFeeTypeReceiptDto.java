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
public class AnnualFeeTypeReceiptDto {
    private String feeType;
    private BigDecimal q1;
    private BigDecimal q2;
    private BigDecimal q3;
    private BigDecimal q4;
    private BigDecimal total;
}