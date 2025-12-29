package org.sofumar.portal.repo.jpaspec;

import org.sofumar.portal.constants.FieldConstants;
import org.sofumar.portal.data.vo.ReferenceVO;
import org.springframework.data.jpa.domain.Specification;

public class ReferenceSpecifications {

    public static Specification<ReferenceVO> hasReferenceName(String referenceName) {
        return (root, query, cb) -> cb.equal(root.get(FieldConstants.REFERENCE_NAME), referenceName);
    }

    public static Specification<ReferenceVO> hasReferenceCode(String referenceCode) {
        return (root, query, cb) -> cb.equal(root.get(FieldConstants.REFERENCE_CODE), referenceCode);
    }

    public static Specification<ReferenceVO> isActive(Boolean active) {
        return (root, query, cb) -> cb.equal(root.get(FieldConstants.ACTIVE), active);
    }
}