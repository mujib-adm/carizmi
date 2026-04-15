package io.carizmi.domain.finance.data.dto.request;

import io.carizmi.shared.data.dto.PaginationDto;

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