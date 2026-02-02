package org.sofumar.portal.data.dto.response;

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