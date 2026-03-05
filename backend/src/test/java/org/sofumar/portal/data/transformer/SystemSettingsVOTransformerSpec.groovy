package org.sofumar.portal.data.transformer

import org.sofumar.portal.core.vo.SystemSettingsVO
import org.sofumar.portal.data.dto.SystemSettingsDto
import org.sofumar.portal.testbase.BaseSpecification

import java.time.LocalDate

class SystemSettingsVOTransformerSpec extends BaseSpecification {

    SystemSettingsVOTransformer transformer = new SystemSettingsVOTransformer()

    def "test - transform: Should transform SystemSettingsDto to SystemSettingsVO"() {
        given: "TestData setup"
        SystemSettingsDto dto = new SystemSettingsDto(
                settingName: "UI",
                settingKey: "THEME",
                settingValue: "DARK",
                effectiveDate: LocalDate.now(),
                active: true
        )

        when: "The target method executed"
        SystemSettingsVO result = transformer.transform(dto)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result != null
        result.settingName == dto.settingName
        result.settingKey == dto.settingKey
        result.settingValue == dto.settingValue
        result.effectiveDate == dto.effectiveDate
        result.isActive() == dto.isActive()
        noExceptionThrown()
    }

    def "test - transform: Should return null when input is null"() {
        when: "The target method executed"
        SystemSettingsVO result = transformer.transform(null)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result == null
        noExceptionThrown()
    }

    def "test - transformForUpdate: Should update existing SystemSettingsVO from DTO"() {
        given: "TestData setup"
        SystemSettingsVO existing = new SystemSettingsVO(systemSettingsID: 1, settingValue: "OLD")
        SystemSettingsDto dto = new SystemSettingsDto(
                settingName: "NEW_TYPE",
                settingKey: "NEW_KEY",
                settingValue: "NEW_VAL",
                effectiveDate: LocalDate.of(2025, 12, 31),
                active: false
        )

        when: "The target method executed"
        SystemSettingsVO result = transformer.transformForUpdate(dto, existing)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result != null
        result == existing
        result.settingName == dto.settingName
        result.settingKey == dto.settingKey
        result.settingValue == dto.settingValue
        result.effectiveDate == dto.effectiveDate
        result.isActive() == dto.isActive()
        noExceptionThrown()
    }

    def "test - transformForUpdate: Should return existing when DTO is null"() {
        given: "TestData setup"
        SystemSettingsVO existing = new SystemSettingsVO(systemSettingsID: 1, settingValue: "OLD")

        when: "The target method executed"
        SystemSettingsVO result = transformer.transformForUpdate(null, existing)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result == existing
        noExceptionThrown()
    }

    def "test - transformForUpdate: Should return null when existing is null"() {
        given: "TestData setup"
        SystemSettingsDto dto = new SystemSettingsDto(settingValue: "NEW")

        when: "The target method executed"
        SystemSettingsVO result = transformer.transformForUpdate(dto, null)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result == null
        noExceptionThrown()
    }

    def "test - transformList: Should transform list of SystemSettingsDtos"() {
        given: "TestData setup"
        SystemSettingsDto dto1 = new SystemSettingsDto(systemSettingsID: 1, settingKey: "K1")
        SystemSettingsDto dto2 = new SystemSettingsDto(systemSettingsID: 2, settingKey: "K2")
        List<SystemSettingsDto> list = [dto1, null, dto2]

        when: "The target method executed"
        List<SystemSettingsVO> result = transformer.transformList(list)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result.size() == 2
        result[0].settingKey == "K1"
        result[1].settingKey == "K2"
        noExceptionThrown()
    }

    def "test - transformList: Should return empty list when input is null"() {
        when: "The target method executed"
        List<SystemSettingsVO> result = transformer.transformList(null)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result != null
        result.isEmpty()
        noExceptionThrown()
    }
}