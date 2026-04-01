package org.sofumar.portal.data.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuarterlyChecklistDto {
    private int year;
    private int currentQuarter;
    private BigDecimal quarterlyFeeAmount;
    private List<MemberQuarterlyRowDto> rows;
}