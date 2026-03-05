package org.sofumar.portal.core.repo.jpaspec

import org.sofumar.portal.constants.FieldConstants
import org.sofumar.portal.core.vo.SystemSettingsVO
import org.sofumar.portal.testbase.BaseSpecification
import org.springframework.data.jpa.domain.Specification as JpaSpecification
import spock.lang.Unroll

class SystemSettingsSpecificationsSpec extends BaseSpecification {

    @Unroll
    def "test - hasSystemSettingsID: Should filter by ID [id: #id]"() {
        given: "An ID"
        Integer testID = id

        when: "The specification is created and inspected"
        JpaSpecification<SystemSettingsVO> spec = SystemSettingsSpecifications.hasSystemSettingsID(testID)
        Map<String, List> inspection = inspectSpecification(spec)

        then: "Strict interaction check"
        0 * _

        and: "The expected filters and values are captured"
        if (testID) {
            inspection.filters.contains(FieldConstants.SYSTEM_SETTINGS_ID)
            inspection.values.contains(testID)
        } else {
            inspection.filters.isEmpty()
            inspection.values.isEmpty()
        }
        noExceptionThrown()

        where:
        id << [1, null]
    }

    @Unroll
    def "test - hasSettingName: Should filter by setting name [settingName: #settingName]"() {
        given: "A setting name"
        String name = settingName

        when: "The specification is created and inspected"
        JpaSpecification<SystemSettingsVO> spec = SystemSettingsSpecifications.hasSettingName(name)
        Map<String, List> inspection = inspectSpecification(spec)

        then: "Strict interaction check"
        0 * _

        and: "The expected filters and values are captured"
        if (name) {
            inspection.filters.contains(FieldConstants.SETTING_NAME)
            inspection.values.contains("%" + name.toLowerCase() + "%")
        } else {
            inspection.filters.isEmpty()
            inspection.values.isEmpty()
        }
        noExceptionThrown()

        where:
        settingName << ["FEE", null]
    }

    @Unroll
    def "test - hasSettingKey: Should filter by setting key [settingKey: #settingKey]"() {
        given: "A setting key"
        String key = settingKey

        when: "The specification is created and inspected"
        JpaSpecification<SystemSettingsVO> spec = SystemSettingsSpecifications.hasSettingKey(key)
        Map<String, List> inspection = inspectSpecification(spec)

        then: "Strict interaction check"
        0 * _

        and: "The expected filters and values are captured"
        if (key) {
            inspection.filters.contains(FieldConstants.SETTING_KEY)
            inspection.values.contains("%" + key.toLowerCase() + "%")
        } else {
            inspection.filters.isEmpty()
            inspection.values.isEmpty()
        }
        noExceptionThrown()

        where:
        settingKey << ["Quarterly", null]
    }

    @Unroll
    def "test - hasSettingValue: Should filter by setting value [settingValue: #settingValue]"() {
        given: "A setting value"
        String value = settingValue

        when: "The specification is created and inspected"
        JpaSpecification<SystemSettingsVO> spec = SystemSettingsSpecifications.hasSettingValue(value)
        Map<String, List> inspection = inspectSpecification(spec)

        then: "Strict interaction check"
        0 * _

        and: "The expected filters and values are captured"
        if (value) {
            inspection.filters.contains(FieldConstants.SETTING_VALUE)
            inspection.values.contains("%" + value.toLowerCase() + "%")
        } else {
            inspection.filters.isEmpty()
            inspection.values.isEmpty()
        }
        noExceptionThrown()

        where:
        settingValue << ["60", null]
    }

    @Unroll
    def "test - isActive: Should filter by active status [active: #active]"() {
        given: "An active status"
        Boolean testActive = active

        when: "The specification is created and inspected"
        JpaSpecification<SystemSettingsVO> spec = SystemSettingsSpecifications.isActive(testActive)
        Map<String, List> inspection = inspectSpecification(spec)

        then: "Strict interaction check"
        0 * _

        and: "The expected filters and values are captured"
        if (testActive != null) {
            inspection.filters.contains(FieldConstants.ACTIVE)
            inspection.values.contains(testActive)
        } else {
            inspection.filters.isEmpty()
            inspection.values.isEmpty()
        }
        noExceptionThrown()

        where:
        active << [true, false, null]
    }

    @Unroll
    def "test - lookup: Should perform fuzzy search [query: #query]"() {
        given: "A search query"
        String testQuery = query

        when: "The specification is created and inspected"
        JpaSpecification<SystemSettingsVO> spec = SystemSettingsSpecifications.lookup(testQuery)
        Map<String, List> inspection = inspectSpecification(spec)

        then: "Strict interaction check"
        0 * _

        and: "The expected filters and values are captured"
        if (testQuery && !testQuery.trim().isEmpty()) {
            String pattern = "%" + testQuery.trim().toLowerCase() + "%"
            inspection.filters.containsAll([FieldConstants.SETTING_NAME, FieldConstants.SETTING_KEY, FieldConstants.SETTING_VALUE])
            inspection.values.containsAll([pattern, pattern, pattern])
        } else {
            inspection.filters.isEmpty()
            inspection.values.isEmpty()
        }
        noExceptionThrown()

        where:
        query << ["fee", "  ", null]
    }

    @Unroll
    def "test - withSettingKey: Should filter strictly by setting key [settingKey: #settingKey]"() {
        given: "A setting key"
        String key = settingKey

        when: "The specification is created and inspected"
        JpaSpecification<SystemSettingsVO> spec = SystemSettingsSpecifications.withSettingKey(key)
        Map<String, List> inspection = inspectSpecification(spec)

        then: "Strict interaction check"
        0 * _

        and: "The expected filters and values are captured"
        if (key) {
            inspection.filters.contains(FieldConstants.SETTING_KEY)
            inspection.values.contains(key.toLowerCase())
        } else {
            inspection.filters.isEmpty()
            inspection.values.isEmpty()
        }
        noExceptionThrown()

        where:
        settingKey << ["KEY", null]
    }

    @Unroll
    def "test - withSettingName: Should filter strictly by setting name [settingName: #settingName]"() {
        given: "A setting name"
        String name = settingName

        when: "The specification is created and inspected"
        JpaSpecification<SystemSettingsVO> spec = SystemSettingsSpecifications.withSettingName(name)
        Map<String, List> inspection = inspectSpecification(spec)

        then: "Strict interaction check"
        0 * _

        and: "The expected filters and values are captured"
        if (name) {
            inspection.filters.contains(FieldConstants.SETTING_NAME)
            inspection.values.contains(name.toLowerCase())
        } else {
            inspection.filters.isEmpty()
            inspection.values.isEmpty()
        }
        noExceptionThrown()

        where:
        settingName << ["NAME", null]
    }
}