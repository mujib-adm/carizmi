package org.sofumar.portal.data.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuarterSummaryDto {
    private int quarter;
    private int paidCount;
    private int unpaidCount;
    private boolean future;
}