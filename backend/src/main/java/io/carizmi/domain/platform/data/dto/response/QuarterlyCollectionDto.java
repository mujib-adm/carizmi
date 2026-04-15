package io.carizmi.domain.platform.data.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.carizmi.shared.constants.QuarterStatus;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuarterlyCollectionDto {
    private String quarterLabel;
    private BigDecimal collectedAmount;
    private double percentage;
    private QuarterStatus status;
}