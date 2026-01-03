package org.sofumar.portal.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.sofumar.portal.constants.QuarterStatus;

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