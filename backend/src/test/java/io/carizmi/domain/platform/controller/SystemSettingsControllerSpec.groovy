package io.carizmi.domain.platform.controller

import io.carizmi.domain.platform.service.SystemSetting
import io.carizmi.domain.platform.data.dto.SystemSettingsDto
import io.carizmi.domain.platform.data.dto.request.SystemSettingsSearchRequestDto
import io.carizmi.framework.data.response.GlobalResponse
import io.carizmi.framework.data.response.PagedResult
import io.carizmi.framework.data.response.PaginationMeta
import io.carizmi.testbase.BaseSpecification
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Subject

class SystemSettingsControllerSpec extends BaseSpecification {

    SystemSetting systemSettingService = Mock()

    @Subject
    SystemSettingsController systemSettingsController = new SystemSettingsController(systemSettingService)

    def "test - getSystemSetting: Should delegate to system setting service and wrap result"() {
        given: "An ID"
        Integer id = 1
        SystemSettingsDto dto = new SystemSettingsDto(systemSettingsID: id)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<SystemSettingsDto>> result = systemSettingsController.getSystemSetting(id)

        then: "The expected calls are made"
        1 * systemSettingService.getSystemSetting(id) >> dto
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.OK
        result.body.responseData.systemSettingsID == id
        noExceptionThrown()
    }

    def "test - updateSystemSetting: Should delegate to system setting service and wrap result"() {
        given: "A system settings DTO"
        SystemSettingsDto dto = new SystemSettingsDto(systemSettingsID: 1, settingValue: "value")

        when: "The target method executed"
        ResponseEntity<GlobalResponse<Void>> result = systemSettingsController.updateSystemSetting(dto)

        then: "The expected calls are made"
        1 * systemSettingService.updateSystemSetting(dto)
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.OK
        noExceptionThrown()
    }

    def "test - searchSystemSettings: Should delegate to system setting service and wrap result"() {
        given: "A search request"
        SystemSettingsSearchRequestDto request = new SystemSettingsSearchRequestDto()
        List<SystemSettingsDto> dtoList = [new SystemSettingsDto(systemSettingsID: 1)]
        PaginationMeta meta = PaginationMeta.of(0, 10, 1, 1)
        PagedResult<SystemSettingsDto> pagedResult = PagedResult.of(dtoList, meta)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<List<SystemSettingsDto>>> result = systemSettingsController.searchSystemSettings(request)

        then: "The expected calls are made"
        1 * systemSettingService.searchSystemSettings(request) >> pagedResult
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.OK
        result.body.responseData.size() == 1
        result.body.responseData[0].systemSettingsID == 1
        noExceptionThrown()
    }

    def "test - getSettingsByKey: Should delegate to system setting service and wrap result"() {
        given: "A key"
        String key = "Fee"
        List<SystemSettingsDto> dtoList = [new SystemSettingsDto(systemSettingsID: 1, settingKey: key)]

        when: "The target method executed"
        ResponseEntity<GlobalResponse<List<SystemSettingsDto>>> result = systemSettingsController.getSettingsByKey(key)

        then: "The expected calls are made"
        1 * systemSettingService.getSettingsByKey(key) >> dtoList
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.OK
        result.body.responseData.size() == 1
        result.body.responseData[0].settingKey == key
        noExceptionThrown()
    }
}