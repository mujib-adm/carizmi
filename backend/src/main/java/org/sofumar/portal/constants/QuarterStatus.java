package org.sofumar.portal.constants;

public enum QuarterStatus {
    PAST,
    CURRENT,
    FUTURE;

    public static QuarterStatus from(String value) {
        if (value == null) {
            throw new IllegalArgumentException("QuarterStatus value cannot be null");
        }
        return QuarterStatus.valueOf(value.trim().toUpperCase());
    }
}