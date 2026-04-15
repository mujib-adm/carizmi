package io.carizmi.domain.finance.data.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.carizmi.domain.finance.constants.QuarterCellStatus;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuarterCellDto {
    private int quarter;
    private QuarterCellStatus status;
    private BigDecimal amountPaid;
}