package io.carizmi.shared.constants;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true, description = "The temporal status of a fiscal quarter")
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