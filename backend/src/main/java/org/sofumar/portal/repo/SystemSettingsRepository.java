package org.sofumar.portal.repo;

import org.sofumar.portal.data.vo.SystemSettingsVO;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemSettingsRepository extends JpaRepository<SystemSettingsVO, Integer> {
//    Optional<SystemSettings> findBySettingKey(String settingKey);
}