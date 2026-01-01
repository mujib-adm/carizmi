package org.sofumar.portal.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LatestPaymentDto {
    private Integer paymentID;
    private Integer memberID;
    private String memberName;
    private String feeType;
    private BigDecimal amount;
    private LocalDate paymentDate;
}