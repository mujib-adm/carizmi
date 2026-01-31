package org.sofumar.portal.data.transformer;

import org.sofumar.portal.data.dto.SystemSettingsDto;
import org.sofumar.portal.core.vo.SystemSettingsVO;
import org.springframework.stereotype.Service;

@Service
public class SystemSettingsDtoTransformer implements Transformer<SystemSettingsVO, SystemSettingsDto> {

    @Override
    public SystemSettingsDto transform(SystemSettingsVO vo) {
        SystemSettingsDto dto = new SystemSettingsDto();
        dto.setSystemSettingsID(vo.getSystemSettingsID());
        dto.setSettingType(vo.getSettingType());
        dto.setSettingKey(vo.getSettingKey());
        dto.setSettingValue(vo.getSettingValue());
        dto.setEffectiveDate(vo.getEffectiveDate());
        dto.setActive(vo.isActive());
        return dto;
    }
}