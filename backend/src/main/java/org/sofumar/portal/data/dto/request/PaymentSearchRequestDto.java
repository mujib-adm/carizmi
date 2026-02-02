package org.sofumar.portal.data.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PaymentSearchRequestDto extends PaginationDto {
    private Integer memberID;
    private String feeType;
    private Integer year;
    private Integer quarter;
    private LocalDate dateFrom;
    private LocalDate dateTo;
}