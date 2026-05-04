package io.carizmi.domain.finance.data.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface LatestPaymentProjection {
    Integer getPaymentID();
    Integer getMemberID();
    String getMemberName();
    String getFeeType();
    BigDecimal getAmount();
    LocalDate getPaymentDate();
}