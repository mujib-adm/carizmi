package io.carizmi.shared.util;

import org.springframework.lang.Nullable;

import java.time.LocalDate;

/**
 * Utility for quarter-based date calculations.
 * Centralizes business rules to ensure consistency across all services.
 */
public final class QuarterUtils {

    private QuarterUtils() {
        // Utility class — no instantiation
    }

    /**
     * Returns the calendar quarter (1–4) for a given date.
     */
    public static int quarterOf(LocalDate date) {
        return (date.getMonthValue() - 1) / 3 + 1;
    }

    /**
     * Returns the last day of the given quarter in the specified year.
     * <p>Q1 → Mar 31, Q2 → Jun 30, Q3 → Sep 30, Q4 → Dec 31.</p>
     */
    public static LocalDate lastDayOfQuarter(int year, int quarter) {
        int endMonth = quarter * 3;
        return LocalDate.of(year, endMonth, 1).plusMonths(1).minusDays(1);
    }

    /**
     * Resolves a potentially null join date to a non-null value.
     * Falls back to {@code LocalDate.now()} if the provided date is null.
     */
    public static LocalDate resolveJoinDate(@Nullable LocalDate joinDate) {
        return joinDate != null ? joinDate : LocalDate.now();
    }
}