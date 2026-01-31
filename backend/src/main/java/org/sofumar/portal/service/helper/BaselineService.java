package org.sofumar.portal.service.helper;

import java.math.BigDecimal;

public interface BaselineService {
    /**
     * Retrieves the baseline revenue as of Jan 1st of the given year.
     * This baseline equals TOTAL_REVENUE as of Dec 31st of (year - 1).
     */
    BigDecimal getBaselineForYear(int year);

    /**
     * Calculates and persists the baseline for the given year if it doesn't exist.
     * This can be used for "closing" a year.
     */
    void closeYear(int year);
}