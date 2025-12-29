package org.sofumar.portal.repo;

import org.sofumar.portal.data.vo.ReferenceVO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ReferenceRepository
        extends JpaRepository<ReferenceVO, Integer>, JpaSpecificationExecutor<ReferenceVO> {
    boolean existsByReferenceNameAndReferenceCodeAndActiveTrue(String referenceName, String referenceCode);

    ReferenceVO findByReferenceNameAndReferenceCode(String referenceName, String referenceCode);
}