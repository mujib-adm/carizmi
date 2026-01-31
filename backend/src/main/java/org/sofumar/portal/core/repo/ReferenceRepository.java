package org.sofumar.portal.core.repo;

import org.sofumar.portal.core.vo.ReferenceVO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ReferenceRepository
        extends JpaRepository<ReferenceVO, Integer>, JpaSpecificationExecutor<ReferenceVO> {
}