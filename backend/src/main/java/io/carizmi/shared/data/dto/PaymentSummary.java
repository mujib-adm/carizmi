package io.carizmi.shared.data.dto;

import java.math.BigDecimal;

/**
 * Projection interface for payment summary queries.
 */
public interface PaymentSummary {
    Integer getMemberID();

    Integer getYear();

    Integer getQuarter();

    BigDecimal getTotalPaid();
}