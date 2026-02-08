package org.sofumar.portal.core.repo.jpaspec;

import org.sofumar.portal.constants.FieldConstants;
import org.sofumar.portal.core.vo.SystemSettingsVO;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;

public class SystemSettingsSpecifications {

    @NonNull
    public static Specification<SystemSettingsVO> hasSystemSettingsID(Integer id) {
        return (root, query, cb) -> id == null ? null : cb.equal(root.get(FieldConstants.SYSTEM_SETTINGS_ID), id);
    }

    @NonNull
    public static Specification<SystemSettingsVO> hasSettingName(String settingName) {
        return (root, query, cb) -> settingName == null ? null
                : cb.like(cb.lower(root.get(FieldConstants.SETTING_NAME)), "%" + settingName.toLowerCase() + "%");
    }

    @NonNull
    public static Specification<SystemSettingsVO> hasSettingKey(String settingKey) {
        return (root, query, cb) -> settingKey == null ? null
                : cb.like(cb.lower(root.get(FieldConstants.SETTING_KEY)), "%" + settingKey.toLowerCase() + "%");
    }

    @NonNull
    public static Specification<SystemSettingsVO> hasSettingValue(String settingValue) {
        return (root, query, cb) -> settingValue == null ? null
                : cb.like(cb.lower(root.get(FieldConstants.SETTING_VALUE)), "%" + settingValue.toLowerCase() + "%");
    }

    @NonNull
    public static Specification<SystemSettingsVO> isActive(Boolean active) {
        return (root, query, cb) -> active == null ? null : cb.equal(root.get(FieldConstants.ACTIVE), active);
    }

    @NonNull
    public static Specification<SystemSettingsVO> lookup(String query) {
        return (root, queryBuilder, cb) -> {
            if (query == null || query.trim().isEmpty()) {
                return null;
            }
            String likePattern = "%" + query.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get(FieldConstants.SETTING_NAME)), likePattern),
                    cb.like(cb.lower(root.get(FieldConstants.SETTING_KEY)), likePattern),
                    cb.like(cb.lower(root.get(FieldConstants.SETTING_VALUE)), likePattern));
        };
    }

    @NonNull
    public static Specification<SystemSettingsVO> withSettingKey(String settingKey) {
        return (root, query, cb) -> settingKey == null ? null
                : cb.equal(cb.lower(root.get(FieldConstants.SETTING_KEY)), settingKey.toLowerCase());
    }

    @NonNull
    public static Specification<SystemSettingsVO> withSettingName(String settingName) {
        return (root, query, cb) -> settingName == null ? null
                : cb.equal(cb.lower(root.get(FieldConstants.SETTING_NAME)), settingName.toLowerCase());
    }
}