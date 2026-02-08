package org.sofumar.portal.data.dto.request;

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