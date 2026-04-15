package io.carizmi.domain.platform.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import io.carizmi.domain.platform.data.dto.SystemSettingsDto;
import io.carizmi.domain.platform.data.dto.request.SystemSettingsSearchRequestDto;
import io.carizmi.domain.platform.model.SystemSettingsVO;
import io.carizmi.framework.bl.BusinessLogic;
import io.carizmi.framework.data.response.PagedResult;

public interface SystemSetting extends BusinessLogic<SystemSettingsVO> {

    void updateSystemSetting(SystemSettingsDto dto);

    SystemSettingsDto getSystemSetting(Integer id);

    PagedResult<SystemSettingsDto> searchSystemSettings(SystemSettingsSearchRequestDto request);

    List<SystemSettingsDto> getSettingsByKey(String key);

    Optional<SystemSettingsVO> findBySettingKey(String key);

    Optional<SystemSettingsVO> findByNameAndKey(String settingName, String key);

    BigDecimal getQuarterlyFeeAmount();
}