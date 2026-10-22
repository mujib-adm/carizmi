package io.carizmi.domain.finance.data.dto.projection;

import java.math.BigDecimal;

/**
 * Projection representing a fee receipt breakdown bounded by cash-receipt date range.
 * Decouples the cash-received timing (receivedQuarter) from the obligation year (obligationYear)
 * and obligation quarter (feeQuarter).
 */
public interface FeeTypeReceiptBreakdownProjection {
    String getFeeType();
    Integer getObligationYear();
    Integer getReceivedQuarter();
    Integer getFeeQuarter();
    BigDecimal getTotalAmount();
}