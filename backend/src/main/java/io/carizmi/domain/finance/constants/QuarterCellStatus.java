package io.carizmi.domain.finance.constants;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true, description = "Payment status of a member for a specific quarter")
public enum QuarterCellStatus {
    PAID,
    UNPAID,
    NOT_APPLICABLE,
    FUTURE
}