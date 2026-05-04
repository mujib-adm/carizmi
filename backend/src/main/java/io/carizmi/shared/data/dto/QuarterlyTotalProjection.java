package io.carizmi.shared.data.dto;

import java.math.BigDecimal;

/**
 * Projection interface for quarterly aggregate payment totals.
 */
public interface QuarterlyTotalProjection {
    Integer getQuarter();
    BigDecimal getTotalCollected();
}