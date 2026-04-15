package io.carizmi.domain.platform.repository;

import io.carizmi.domain.platform.model.ReferenceVO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ReferenceRepository
        extends JpaRepository<ReferenceVO, Integer>, JpaSpecificationExecutor<ReferenceVO> {
}