package io.carizmi.domain.platform.data.dto.request;

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
public class SystemSettingsSearchRequestDto extends PaginationDto {
    private String settingName;
}