package io.carizmi.domain.platform.data.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SystemSettingsDto {
    private Integer systemSettingsID;
    private String settingName;
    private String settingKey;
    private String settingValue;
    private LocalDate effectiveDate;
    private boolean active;
}