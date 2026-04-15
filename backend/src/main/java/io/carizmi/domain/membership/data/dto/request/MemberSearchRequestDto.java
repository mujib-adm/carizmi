package io.carizmi.domain.membership.data.dto.request;

import io.carizmi.shared.data.dto.PaginationDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import lombok.EqualsAndHashCode;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MemberSearchRequestDto extends PaginationDto {
    private String firstName;
    private String lastName;
    private String phone;
    private String status;
}