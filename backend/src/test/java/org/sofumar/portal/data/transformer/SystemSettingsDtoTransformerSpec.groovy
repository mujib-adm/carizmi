package org.sofumar.portal.data.transformer

import org.sofumar.portal.core.vo.SystemSettingsVO
import org.sofumar.portal.data.dto.SystemSettingsDto
import org.sofumar.portal.testsupport.BaseSpecification

import java.time.LocalDate

class SystemSettingsDtoTransformerSpec extends BaseSpecification {

    SystemSettingsDtoTransformer transformer = new SystemSettingsDtoTransformer()

    def "test - transform: Should transform SystemSettingsVO to SystemSettingsDto"() {
        given: "TestData setup"
        SystemSettingsVO vo = new SystemSettingsVO(
                systemSettingsID: 1,
                settingName: "CONFIG",
                settingKey: "MAX_LOGIN_ATTEMPTS",
                settingValue: "5",
                effectiveDate: LocalDate.of(2025, 1, 1),
                active: true
        )

        when: "The target method executed"
        SystemSettingsDto result = transformer.transform(vo)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result != null
        result.systemSettingsID == vo.systemSettingsID
        result.settingName == vo.settingName
        result.settingKey == vo.settingKey
        result.settingValue == vo.settingValue
        result.effectiveDate == vo.effectiveDate
        result.isActive() == vo.isActive()
        noExceptionThrown()
    }

    def "test - transform: Should return null when input is null"() {
        when: "The target method executed"
        SystemSettingsDto result = transformer.transform(null)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result == null
        noExceptionThrown()
    }

    def "test - transformList: Should transform list of SystemSettingsVOs"() {
        given: "TestData setup"
        SystemSettingsVO vo1 = new SystemSettingsVO(systemSettingsID: 1, settingKey: "K1")
        SystemSettingsVO vo2 = new SystemSettingsVO(systemSettingsID: 2, settingKey: "K2")
        List<SystemSettingsVO> list = [vo1, null, vo2]

        when: "The target method executed"
        List<SystemSettingsDto> result = transformer.transformList(list)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result.size() == 2
        result[0].systemSettingsID == 1
        result[1].systemSettingsID == 2
        noExceptionThrown()
    }

    def "test - transformList: Should return empty list when input is null"() {
        when: "The target method executed"
        List<SystemSettingsDto> result = transformer.transformList(null)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result != null
        result.isEmpty()
        noExceptionThrown()
    }
}