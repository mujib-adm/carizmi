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
public class ExpenseSearchRequestDto extends PaginationDto {
    private String category;
    private LocalDate dateFrom;
    private LocalDate dateTo;
}