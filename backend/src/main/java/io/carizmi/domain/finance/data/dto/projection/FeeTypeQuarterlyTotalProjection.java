package io.carizmi.domain.finance.data.dto.projection;

import java.math.BigDecimal;

public interface FeeTypeQuarterlyTotalProjection {
    String getFeeType();
    Integer getQuarter();
    BigDecimal getTotalAmount();
}