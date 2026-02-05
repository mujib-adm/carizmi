package org.sofumar.portal.controller

import org.sofumar.portal.core.businesslogic.SystemSetting
import org.sofumar.portal.data.dto.SystemSettingsDto
import org.sofumar.portal.data.dto.request.SystemSettingsSearchRequestDto
import org.sofumar.portal.framework.data.response.GlobalResponse
import org.sofumar.portal.testsupport.BaseSpecification
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Subject

class SystemSettingsControllerSpec extends BaseSpecification {

    SystemSetting systemSettingService = Mock()

    @Subject
    SystemSettingsController systemSettingsController = new SystemSettingsController(systemSettingService)

    def "test - getSystemSetting: Should delegate to system setting service"() {
        given: "An ID"
        Integer id = 1
        SystemSettingsDto dto = new SystemSettingsDto(systemSettingsID: id)
        GlobalResponse<SystemSettingsDto> responseBody = GlobalResponse.withResponseData(dto)
        ResponseEntity<GlobalResponse<SystemSettingsDto>> expectedResponse = new ResponseEntity<>(responseBody, HttpStatus.OK)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<SystemSettingsDto>> result = systemSettingsController.getSystemSetting(id)

        then: "The expected calls are made"
        1 * systemSettingService.getSystemSetting(id) >> expectedResponse
        0 * _

        and: "The expected result"
        result == expectedResponse
        result.body.responseData.systemSettingsID == id
        noExceptionThrown()
    }

    def "test - updateSystemSetting: Should delegate to system setting service"() {
        given: "A system settings DTO"
        Integer id = 1
        String value = "value"
        SystemSettingsDto dto = new SystemSettingsDto(systemSettingsID: id, settingValue: value)
        ResponseEntity<GlobalResponse<Void>> expectedResponse = new ResponseEntity<>(HttpStatus.OK)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<Void>> result = systemSettingsController.updateSystemSetting(dto)

        then: "The expected calls are made"
        1 * systemSettingService.updateSystemSetting(dto) >> expectedResponse
        0 * _

        and: "The expected result"
        result == expectedResponse
        result.statusCode == HttpStatus.OK
        noExceptionThrown()
    }

    def "test - searchSystemSettings: Should delegate to system setting service"() {
        given: "A search request"
        SystemSettingsSearchRequestDto request = new SystemSettingsSearchRequestDto()
        Integer settingID = 1
        Integer expectedSize = 1
        List<SystemSettingsDto> dtoList = [new SystemSettingsDto(systemSettingsID: settingID)]
        GlobalResponse<List<SystemSettingsDto>> responseBody = GlobalResponse.withResponseData(dtoList)
        ResponseEntity<GlobalResponse<List<SystemSettingsDto>>> expectedResponse = new ResponseEntity<>(responseBody, HttpStatus.OK)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<List<SystemSettingsDto>>> result = systemSettingsController.searchSystemSettings(request)

        then: "The expected calls are made"
        1 * systemSettingService.searchSystemSettings(request) >> expectedResponse
        0 * _

        and: "The expected result"
        result == expectedResponse
        result.body.responseData.size() == expectedSize
        result.body.responseData[0].systemSettingsID == settingID
        noExceptionThrown()
    }

    def "test - getSettingsByKey: Should delegate to system setting service"() {
        given: "A key"
        String key = "Fee"
        Integer settingID = 1
        Integer expectedSize = 1
        List<SystemSettingsDto> dtoList = [new SystemSettingsDto(systemSettingsID: settingID, settingKey: key)]
        GlobalResponse<List<SystemSettingsDto>> responseBody = GlobalResponse.withResponseData(dtoList)
        ResponseEntity<GlobalResponse<List<SystemSettingsDto>>> expectedResponse = new ResponseEntity<>(responseBody, HttpStatus.OK)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<List<SystemSettingsDto>>> result = systemSettingsController.getSettingsByKey(key)

        then: "The expected calls are made"
        1 * systemSettingService.getSettingsByKey(key) >> expectedResponse
        0 * _

        and: "The expected result"
        result == expectedResponse
        result.body.responseData.size() == expectedSize
        result.body.responseData[0].settingKey == key
        noExceptionThrown()
    }
}