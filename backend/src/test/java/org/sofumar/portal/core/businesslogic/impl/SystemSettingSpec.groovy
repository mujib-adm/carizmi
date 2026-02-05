package org.sofumar.portal.core.businesslogic.impl

import org.sofumar.portal.constants.FieldConstants

import org.sofumar.portal.data.dto.SystemSettingsDto
import org.sofumar.portal.data.dto.request.SystemSettingsSearchRequestDto
import org.sofumar.portal.data.transformer.SystemSettingsDtoTransformer
import org.sofumar.portal.core.vo.SystemSettingsVO
import org.sofumar.portal.framework.data.response.GlobalResponse
import org.sofumar.portal.framework.exception.DuplicateRecordException
import org.sofumar.portal.framework.exception.RecordNotFoundException
import org.sofumar.portal.framework.util.MySQLConstraintResolver
import org.sofumar.portal.core.repo.SystemSettingRepository
import org.sofumar.portal.service.validation.SystemSettingsValidator
import org.sofumar.portal.testsupport.BaseSpecification
import org.springframework.dao.DataAccessException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification as JpaSpecification
import org.springframework.http.ResponseEntity
import org.springframework.test.util.ReflectionTestUtils
import spock.lang.Subject
import spock.lang.Unroll

class SystemSettingSpec extends BaseSpecification {

    SystemSettingRepository settingsRepo = Mock()
    SystemSettingsDtoTransformer dtoTransformer = Mock()
    SystemSettingsValidator validator = Mock()
    MySQLConstraintResolver constraintResolver = Mock()

    @Subject
    SystemSettingImpl systemSetting = new SystemSettingImpl(settingsRepo, dtoTransformer, validator)

    void setup() {
        ReflectionTestUtils.setField(systemSetting, "constraintResolver", constraintResolver)
    }

    def "test - updateSystemSetting: Success"() {
        given: "A valid update request and existing VO"
        Integer id = 1
        String value = "V"
        String type = "T"
        String key = "K"
        SystemSettingsDto dto = new SystemSettingsDto(systemSettingsID: id, settingValue: value)
        SystemSettingsVO vo = new SystemSettingsVO(systemSettingsID: id, settingType: type, settingKey: key)
        ResponseEntity<GlobalResponse<Void>> response

        when: "The target method executed"
        response = systemSetting.updateSystemSetting(dto)

        then: "The expected calls are made"
        1 * settingsRepo.findById(1) >> Optional.of(vo)
        1 * validator.validateForUpdate(vo)
        1 * settingsRepo.save(vo) >> vo
        0 * _

        and: "The expected result"
        response.statusCode.value() == 200
        vo.getSettingValue() == "V"
        noExceptionThrown()
    }

    def "test - updateSystemSetting: Duplicate Handling"() {
        given: "A duplicate key scenario"
        SystemSettingsVO vo = new SystemSettingsVO(systemSettingsID: 1)
        SystemSettingsDto dto = new SystemSettingsDto(systemSettingsID: 1, settingValue: "V")

        when: "The target method executed"
        systemSetting.updateSystemSetting(dto)

        then: "The expected calls are made"
        1 * settingsRepo.findById(1) >> Optional.of(vo)
        1 * validator.validateForUpdate(vo)
        1 * settingsRepo.save(vo) >> { throw new DataIntegrityViolationException("Dup", new RuntimeException("Duplicate entry 'K' for key 'UK_setting_key'")) }
        1 * constraintResolver.resolveFields(_) >> ["settingKey"]
        0 * _

        and: "The expected result"
        thrown(DuplicateRecordException)
    }

    def "test - updateSystemSetting: General DB Error"() {
        given: "A DB error scenario during update"
        SystemSettingsVO vo = new SystemSettingsVO(systemSettingsID: 1)
        SystemSettingsDto dto = new SystemSettingsDto(systemSettingsID: 1, settingValue: "V")

        when: "The target method executed"
        systemSetting.updateSystemSetting(dto)

        then: "The expected calls are made"
        1 * settingsRepo.findById(1) >> Optional.of(vo)
        1 * validator.validateForUpdate(vo)
        1 * settingsRepo.save(vo) >> { throw new DataAccessException("error", new RuntimeException("root")) {} }
        0 * _

        and: "The expected result"
        vo.hasErrors()
        noExceptionThrown()
    }

    def "test - updateSystemSetting: Not Found"() {
        given: "A missing system setting ID"
        Integer id = 99
        SystemSettingsDto dto = new SystemSettingsDto(systemSettingsID: id)

        when: "The target method executed"
        systemSetting.updateSystemSetting(dto)

        then: "The expected calls are made"
        1 * settingsRepo.findById(99) >> Optional.empty()
        0 * _

        and: "The expected result"
        thrown(RecordNotFoundException)
    }

    def "test - getSystemSetting: Success"() {
        given: "An existing system setting"
        Integer id = 1
        SystemSettingsVO vo = new SystemSettingsVO(systemSettingsID: id)
        ResponseEntity<GlobalResponse<SystemSettingsDto>> response

        when: "The target method executed"
        response = systemSetting.getSystemSetting(id)

        then: "The expected calls are made"
        1 * settingsRepo.findById(1) >> Optional.of(vo)
        1 * dtoTransformer.transform(vo) >> new SystemSettingsDto()
        0 * _

        and: "The expected result"
        response != null
        noExceptionThrown()
    }

    def "test - getSystemSetting: Not Found"() {
        given: "A missing ID for retrieval"
        Integer id = 99

        when: "The target method executed"
        systemSetting.getSystemSetting(id)

        then: "The expected calls are made"
        1 * settingsRepo.findById(99) >> Optional.empty()
        0 * _

        and: "The expected result"
        thrown(RecordNotFoundException)
    }

    @Unroll
    def "test - searchSystemSettings: Coverage for #desc"() {
        given: "Mock page and search permutations"
        Page<SystemSettingsVO> mockPage = Mock(Page)
        JpaSpecification capturedSpec

        SystemSettingsSearchRequestDto request = new SystemSettingsSearchRequestDto(settingType: type)
        request.setPage(0)
        request.setSize(10)
        request.setSortField(FieldConstants.SETTING_KEY)
        request.setSortOrder("DESC")

        when: "The target method executed"
        systemSetting.searchSystemSettings(request)

        then: "The expected calls are made"
        1 * settingsRepo.findAll(_ as JpaSpecification, _ as PageRequest) >> { JpaSpecification spec, PageRequest page ->
            capturedSpec = spec
            return mockPage
        }
        1 * mockPage.toList() >> []
        1 * dtoTransformer.transformList([]) >> []
        // Metadata calls - use wildcards effectively but strictly
        _ * mockPage.getNumber() >> 0
        _ * mockPage.getSize() >> 10
        _ * mockPage.getTotalElements() >> 0
        _ * mockPage.getTotalPages() >> 0
        0 * _

        and: "The expected result"
        noExceptionThrown()
        capturedSpec != null
        Map<String, List> inspection = inspectSpecification(capturedSpec)
        if (expectedFilters) {
            inspection.filters.containsAll(expectedFilters)
            inspection.values.containsAll(expectedValues)
        } else {
            inspection.filters.isEmpty()
        }

        where:
        desc         | type   || expectedFilters               | expectedValues
        "Type only"  | "Type" || [FieldConstants.SETTING_TYPE] | ["type"]
        "No filters" | null   || []                            | []
    }

    def "test - getSettingsByKey: Should return settings matching the key"() {
        given: "A settings key"
        String key = "KEY"
        JpaSpecification capturedSpec

        when: "The target method executed"
        systemSetting.getSettingsByKey(key)

        then: "The expected calls are made"
        1 * settingsRepo.findAll(_ as JpaSpecification) >> { JpaSpecification spec ->
            capturedSpec = spec
            return []
        }
        1 * dtoTransformer.transformList([]) >> []
        0 * _

        and: "The expected result"
        noExceptionThrown()
        capturedSpec != null
        Map<String, List> inspection = inspectSpecification(capturedSpec)
        inspection.filters.contains(FieldConstants.SETTING_KEY)
        inspection.values.contains(key.toLowerCase())
    }

    def "test - findBySettingKey: Should return Optional of VO"() {
        given: "A key"
        String key = "KEY"
        SystemSettingsVO vo = new SystemSettingsVO(settingKey: key)
        JpaSpecification capturedSpec = null

        when: "The target method executed"
        Optional<SystemSettingsVO> result = systemSetting.findBySettingKey(key)

        then: "The expected calls are made"
        1 * settingsRepo.findOne(_ as JpaSpecification) >> { JpaSpecification spec -> capturedSpec = spec; Optional.of(vo) }
        0 * _

        and: "The expected result"
        result.isPresent()
        result.get() == vo
        Map<String, List> inspection = inspectSpecification(capturedSpec)
        inspection.filters.contains(FieldConstants.SETTING_KEY)
        inspection.values.contains(key.toLowerCase())
        noExceptionThrown()
    }

    def "test - findByTypeAndKey: Should return Optional of VO"() {
        given: "Type and Key"
        String type = "TYPE"
        String key = "KEY"
        SystemSettingsVO vo = new SystemSettingsVO(settingType: type, settingKey: key)
        JpaSpecification capturedSpec = null

        when: "The target method executed"
        Optional<SystemSettingsVO> result = systemSetting.findByTypeAndKey(type, key)

        then: "The expected calls are made"
        1 * settingsRepo.findOne(_ as JpaSpecification) >> { JpaSpecification spec -> capturedSpec = spec; Optional.of(vo) }
        0 * _

        and: "The expected result"
        result.isPresent()
        result.get() == vo
        Map<String, List> inspection = inspectSpecification(capturedSpec)
        inspection.filters.containsAll([FieldConstants.SETTING_TYPE, FieldConstants.SETTING_KEY])
        inspection.values.containsAll([type.toLowerCase(), key.toLowerCase()])
        noExceptionThrown()
    }
}