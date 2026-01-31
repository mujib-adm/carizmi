package org.sofumar.portal.core.repo;

import org.sofumar.portal.core.vo.SystemSettingsVO;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SystemSettingRepository extends JpaRepository<SystemSettingsVO, Integer>, JpaSpecificationExecutor<SystemSettingsVO> {
}