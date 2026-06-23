package io.carizmi.domain.platform.data.transformer;

import io.carizmi.domain.platform.data.dto.SystemSettingsDto;
import io.carizmi.domain.platform.model.SystemSettingsVO;
import io.carizmi.framework.data.transformer.Transformer;
import org.springframework.stereotype.Service;

@Service
public class SystemSettingsVOTransformer implements Transformer<SystemSettingsDto, SystemSettingsVO> {

    @Override
    public SystemSettingsVO transform(SystemSettingsDto dto) {
        if (dto == null) return null;
        SystemSettingsVO vo = new SystemSettingsVO();
        vo.setSettingName(dto.getSettingName());
        vo.setSettingKey(dto.getSettingKey());
        vo.setSettingValue(dto.getSettingValue());
        vo.setEffectiveDate(dto.getEffectiveDate());
        vo.setActive(Boolean.TRUE.equals(dto.getActive()));
        return vo;
    }

    public SystemSettingsVO transformForUpdate(SystemSettingsDto dto, SystemSettingsVO existingVO) {
        if (dto == null || existingVO == null) return existingVO;
        existingVO.setSettingName(dto.getSettingName());
        existingVO.setSettingKey(dto.getSettingKey());
        existingVO.setSettingValue(dto.getSettingValue());
        existingVO.setEffectiveDate(dto.getEffectiveDate());
        existingVO.setActive(Boolean.TRUE.equals(dto.getActive()));
        return existingVO;
    }
}