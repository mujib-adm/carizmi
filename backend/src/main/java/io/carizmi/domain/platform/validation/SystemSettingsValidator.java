package io.carizmi.domain.platform.validation;

import io.carizmi.shared.constants.FieldConstants;
import io.carizmi.domain.platform.model.SystemSettingsVO;
import io.carizmi.framework.bl.AbstractDomainValidator;
import org.springframework.stereotype.Service;

@Service
public class SystemSettingsValidator extends AbstractDomainValidator<SystemSettingsVO> {

    @Override
    public void validate(SystemSettingsVO vo) {
        validateSettingName(vo);
        validateSettingKey(vo);
        validateSettingValue(vo);
    }

    @Override
    public void validateForUpdate(SystemSettingsVO vo) {
        validateRequired(vo, FieldConstants.SYSTEM_SETTINGS_ID, vo.getSystemSettingsID());
        validate(vo);
    }

    private void validateSettingName(SystemSettingsVO vo) {
        validateRequired(vo, FieldConstants.SETTING_NAME, vo.getSettingName());
    }

    private void validateSettingKey(SystemSettingsVO vo) {
        validateRequired(vo, FieldConstants.SETTING_KEY, vo.getSettingKey());
    }

    private void validateSettingValue(SystemSettingsVO vo) {
        validateRequired(vo, FieldConstants.SETTING_VALUE, vo.getSettingValue());
    }
}