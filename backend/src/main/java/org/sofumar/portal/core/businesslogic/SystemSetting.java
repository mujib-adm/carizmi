package org.sofumar.portal.core.businesslogic;

import java.util.List;
import java.util.Optional;

import org.sofumar.portal.data.dto.SystemSettingsDto;
import org.sofumar.portal.data.dto.request.SystemSettingsSearchRequestDto;
import org.sofumar.portal.core.vo.SystemSettingsVO;
import org.sofumar.portal.framework.bl.BusinessLogic;
import org.sofumar.portal.framework.data.response.PagedResult;

public interface SystemSetting extends BusinessLogic<SystemSettingsVO> {

    void updateSystemSetting(SystemSettingsDto dto);

    SystemSettingsDto getSystemSetting(Integer id);

    PagedResult<SystemSettingsDto> searchSystemSettings(SystemSettingsSearchRequestDto request);

    List<SystemSettingsDto> getSettingsByKey(String key);

    Optional<SystemSettingsVO> findBySettingKey(String key);

    Optional<SystemSettingsVO> findByNameAndKey(String settingName, String key);
}