package org.sofumar.portal.data.transformer;

import org.sofumar.portal.data.dto.SystemSettingsDto;
import org.sofumar.portal.core.vo.SystemSettingsVO;
import org.sofumar.portal.framework.data.transformer.Transformer;
import org.springframework.stereotype.Service;

@Service
public class SystemSettingsVOTransformer implements Transformer<SystemSettingsDto, SystemSettingsVO> {

    @Override
    public SystemSettingsVO transform(SystemSettingsDto dto) {
        if (dto == null) return null;
        SystemSettingsVO vo = new SystemSettingsVO();
        vo.setSettingType(dto.getSettingType());
        vo.setSettingKey(dto.getSettingKey());
        vo.setSettingValue(dto.getSettingValue());
        vo.setEffectiveDate(dto.getEffectiveDate());
        vo.setActive(dto.isActive());
        return vo;
    }

    public SystemSettingsVO transformForUpdate(SystemSettingsDto dto, SystemSettingsVO existingVO) {
        if (dto == null || existingVO == null) return existingVO;
        existingVO.setSettingType(dto.getSettingType());
        existingVO.setSettingKey(dto.getSettingKey());
        existingVO.setSettingValue(dto.getSettingValue());
        existingVO.setEffectiveDate(dto.getEffectiveDate());
        existingVO.setActive(dto.isActive());
        return existingVO;
    }
}