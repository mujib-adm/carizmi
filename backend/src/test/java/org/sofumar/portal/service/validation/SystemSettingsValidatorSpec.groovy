package org.sofumar.portal.service.validation

import org.sofumar.portal.constants.FieldConstants
import org.sofumar.portal.core.vo.SystemSettingsVO
import org.sofumar.portal.framework.exception.ValidationException
import org.sofumar.portal.testsupport.BaseSpecification
import spock.lang.Subject
import spock.lang.Unroll

class SystemSettingsValidatorSpec extends BaseSpecification {

    @Subject
    SystemSettingsValidator systemSettingsValidator = new SystemSettingsValidator()

    def "test - validate: Should pass for valid VO"() {
        given: "A valid SystemSettingsVO"
        String type = "GENERAL"
        String key = "FEE"
        String value = "100"
        SystemSettingsVO vo = new SystemSettingsVO(settingName: type, settingKey: key, settingValue: value)

        when: "The target method executed"
        systemSettingsValidator.validate(vo)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        !vo.hasErrors()
        noExceptionThrown()
    }

    @Unroll
    def "test - validate: Handling field validations [field: #field, value: #value]"() {
        given: "A SystemSettingsVO with a specific field variation"
        SystemSettingsVO vo = new SystemSettingsVO(
                settingName: field == FieldConstants.SETTING_NAME ? value : "GENERAL",
                settingKey: field == FieldConstants.SETTING_KEY ? value : "FEE",
                settingValue: field == FieldConstants.SETTING_VALUE ? value : "100"
        )

        when: "The target method executed"
        try {
            systemSettingsValidator.validate(vo)
        } catch (ValidationException e) {
            // expected
        }

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        vo.hasErrors()
        vo.getFieldMessages().containsKey(field)
        noExceptionThrown()

        where:
        field                        | value
        FieldConstants.SETTING_NAME | null
        FieldConstants.SETTING_NAME | ""
        FieldConstants.SETTING_KEY   | null
        FieldConstants.SETTING_KEY   | ""
        FieldConstants.SETTING_VALUE | null
        FieldConstants.SETTING_VALUE | ""
    }

    def "test - validateForUpdate: Should validate systemSettingsID and call validate"() {
        given: "A SystemSettingsVO for update"
        Integer settingID = 1
        SystemSettingsVO vo = new SystemSettingsVO(systemSettingsID: settingID, settingName: "GENERAL", settingKey: "FEE", settingValue: "100")

        when: "The target method executed"
        systemSettingsValidator.validateForUpdate(vo)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        !vo.hasErrors()
        noExceptionThrown()
    }

    def "test - validateForUpdate: Should catch missing systemSettingsID"() {
        given: "A SystemSettingsVO for update without ID"
        SystemSettingsVO vo = new SystemSettingsVO(settingName: "GENERAL", settingKey: "FEE", settingValue: "100")

        when: "The target method executed"
        systemSettingsValidator.validateForUpdate(vo)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        thrown(ValidationException)
        vo.hasErrors()
        vo.getFieldMessages().containsKey(FieldConstants.SYSTEM_SETTINGS_ID)
    }
}