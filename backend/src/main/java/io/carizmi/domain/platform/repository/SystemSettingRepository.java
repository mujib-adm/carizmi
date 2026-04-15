package io.carizmi.domain.platform.repository;

import io.carizmi.domain.platform.model.SystemSettingsVO;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SystemSettingRepository extends JpaRepository<SystemSettingsVO, Integer>, JpaSpecificationExecutor<SystemSettingsVO> {
}