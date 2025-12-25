package org.sofumar.portal.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentDto {
    private Integer paymentID;
    private Integer memberID;
    private String memberFullName;
    private String feeType;
    private BigDecimal amount;
    private LocalDate dateReceived;
    private String methodOfPayment;
    private Integer year;
    private Integer quarter;
}