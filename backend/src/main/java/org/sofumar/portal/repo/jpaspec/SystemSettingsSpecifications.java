package org.sofumar.portal.repo.jpaspec;

import org.sofumar.portal.constants.FieldConstants;
import org.sofumar.portal.data.vo.SystemSettingsVO;
import org.springframework.data.jpa.domain.Specification;

public class SystemSettingsSpecifications {

    public static Specification<SystemSettingsVO> hasSystemSettingsID(Integer id) {
        return (root, query, cb) -> id == null ? null : cb.equal(root.get(FieldConstants.SYSTEM_SETTINGS_ID), id);
    }

    public static Specification<SystemSettingsVO> hasSettingType(String settingType) {
        return (root, query, cb) -> settingType == null ? null
                : cb.like(cb.lower(root.get(FieldConstants.SETTING_TYPE)), "%" + settingType.toLowerCase() + "%");
    }

    public static Specification<SystemSettingsVO> hasSettingKey(String settingKey) {
        return (root, query, cb) -> settingKey == null ? null
                : cb.like(cb.lower(root.get(FieldConstants.SETTING_KEY)), "%" + settingKey.toLowerCase() + "%");
    }

    public static Specification<SystemSettingsVO> hasSettingValue(String settingValue) {
        return (root, query, cb) -> settingValue == null ? null
                : cb.like(cb.lower(root.get(FieldConstants.SETTING_VALUE)), "%" + settingValue.toLowerCase() + "%");
    }

    public static Specification<SystemSettingsVO> isActive(Boolean active) {
        return (root, query, cb) -> active == null ? null : cb.equal(root.get(FieldConstants.ACTIVE), active);
    }

    public static Specification<SystemSettingsVO> lookup(String query) {
        return (root, queryBuilder, cb) -> {
            if (query == null || query.trim().isEmpty()) {
                return null;
            }
            String likePattern = "%" + query.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get(FieldConstants.SETTING_TYPE)), likePattern),
                    cb.like(cb.lower(root.get(FieldConstants.SETTING_KEY)), likePattern),
                    cb.like(cb.lower(root.get(FieldConstants.SETTING_VALUE)), likePattern));
        };
    }
}