package org.sofumar.portal.service.validation;

import io.micrometer.common.util.StringUtils;
import org.sofumar.portal.constants.FieldConstants;

import org.sofumar.portal.data.vo.SystemSettingsVO;
import org.sofumar.portal.framework.exception.ValidationException;
import org.sofumar.portal.framework.util.LabelUtils;
import org.springframework.stereotype.Service;

import static org.sofumar.portal.constants.MessagesConstants.REQUIRED_FIELD;

@Service
public class SystemSettingsValidator {

    public void validate(SystemSettingsVO vo) throws ValidationException {
        validateSettingType(vo);
        validateSettingKey(vo);
        validateSettingValue(vo);

        if (vo.hasErrors()) {
            throw new ValidationException(vo);
        }
    }

    public void validateForUpdate(SystemSettingsVO vo) throws ValidationException {
        if (vo.getSystemSettingsID() == null) {
            vo.addFieldMessage(FieldConstants.SYSTEM_SETTINGS_ID,
                    REQUIRED_FIELD.addMessageArgs(LabelUtils.toLabel(FieldConstants.SYSTEM_SETTINGS_ID)));
        }
        validate(vo);
    }

    private void validateSettingType(SystemSettingsVO vo) {
        if (StringUtils.isBlank(vo.getSettingType())) {
            vo.addFieldMessage(FieldConstants.SETTING_TYPE,
                    REQUIRED_FIELD.addMessageArgs(LabelUtils.toLabel(FieldConstants.SETTING_TYPE)));
        }
    }

    private void validateSettingKey(SystemSettingsVO vo) {
        if (StringUtils.isBlank(vo.getSettingKey())) {
            vo.addFieldMessage(FieldConstants.SETTING_KEY,
                    REQUIRED_FIELD.addMessageArgs(LabelUtils.toLabel(FieldConstants.SETTING_KEY)));
        }
    }

    private void validateSettingValue(SystemSettingsVO vo) {
        if (StringUtils.isBlank(vo.getSettingValue())) {
            vo.addFieldMessage(FieldConstants.SETTING_VALUE,
                    REQUIRED_FIELD.addMessageArgs(LabelUtils.toLabel(FieldConstants.SETTING_VALUE)));
        }
    }
}