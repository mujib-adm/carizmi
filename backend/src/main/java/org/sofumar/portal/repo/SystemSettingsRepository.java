package org.sofumar.portal.repo;

import org.sofumar.portal.data.vo.SystemSettingsVO;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SystemSettingsRepository
        extends JpaRepository<SystemSettingsVO, Integer>, JpaSpecificationExecutor<SystemSettingsVO> {
    java.util.List<SystemSettingsVO> findBySettingKey(String settingKey);

    java.util.List<SystemSettingsVO> findBySettingType(String settingType);
}