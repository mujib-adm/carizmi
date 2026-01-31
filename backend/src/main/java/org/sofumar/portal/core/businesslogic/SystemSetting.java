package org.sofumar.portal.core.businesslogic;

import java.util.List;
import java.util.Optional;

import org.sofumar.portal.data.dto.SystemSettingsDto;
import org.sofumar.portal.core.vo.SystemSettingsVO;
import org.sofumar.portal.framework.bl.BusinessLogic;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.springframework.http.ResponseEntity;

public interface SystemSetting extends BusinessLogic<SystemSettingsVO> {

    ResponseEntity<GlobalResponse<Void>> updateSystemSetting(SystemSettingsDto dto);

    ResponseEntity<GlobalResponse<SystemSettingsDto>> getSystemSetting(Integer id);

    ResponseEntity<GlobalResponse<List<SystemSettingsDto>>> searchSystemSettings(String settingType, String settingKey,
            String settingValue, int page, int size, String sortField, String sortOrder);

    ResponseEntity<GlobalResponse<List<SystemSettingsDto>>> getSettingsByKey(String key);

    Optional<SystemSettingsVO> findBySettingKey(String key);

    Optional<SystemSettingsVO> findByTypeAndKey(String settingType, String key);
}