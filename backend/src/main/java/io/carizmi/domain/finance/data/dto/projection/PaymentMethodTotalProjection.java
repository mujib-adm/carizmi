package io.carizmi.domain.finance.data.dto.projection;

import java.math.BigDecimal;

public interface PaymentMethodTotalProjection {
    String getMethodOfPayment();
    BigDecimal getTotalAmount();
}