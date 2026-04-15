package io.carizmi.domain.platform.data.transformer;

import io.carizmi.domain.platform.data.dto.SystemSettingsDto;
import io.carizmi.domain.platform.model.SystemSettingsVO;
import io.carizmi.framework.data.transformer.Transformer;
import org.springframework.stereotype.Service;

@Service
public class SystemSettingsDtoTransformer implements Transformer<SystemSettingsVO, SystemSettingsDto> {

    @Override
    public SystemSettingsDto transform(SystemSettingsVO vo) {
        if (vo == null) return null;
        SystemSettingsDto dto = new SystemSettingsDto();
        dto.setSystemSettingsID(vo.getSystemSettingsID());
        dto.setSettingName(vo.getSettingName());
        dto.setSettingKey(vo.getSettingKey());
        dto.setSettingValue(vo.getSettingValue());
        dto.setEffectiveDate(vo.getEffectiveDate());
        dto.setActive(vo.isActive());
        return dto;
    }
}