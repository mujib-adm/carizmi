package io.carizmi.domain.finance.data.dto.request;

import io.carizmi.shared.data.dto.PaginationDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ChecklistSearchRequestDto extends PaginationDto {
    private Integer year;
    private List<Integer> unpaidQuarters;
}