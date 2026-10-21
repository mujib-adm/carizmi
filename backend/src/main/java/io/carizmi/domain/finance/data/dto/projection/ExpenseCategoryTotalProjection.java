package io.carizmi.domain.finance.data.dto.projection;

import java.math.BigDecimal;

public interface ExpenseCategoryTotalProjection {
    String getCategory();
    BigDecimal getTotalAmount();
}