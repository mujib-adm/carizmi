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
public class ExpenseSearchRequestDto extends PaginationDto {
    private String category;
    private LocalDate dateFrom;
    private LocalDate dateTo;
}