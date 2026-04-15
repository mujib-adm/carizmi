package io.carizmi.domain.platform.repository.spec;

import io.carizmi.shared.constants.FieldConstants;
import io.carizmi.domain.platform.model.ReferenceVO;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;

public class ReferenceSpecifications {

    @NonNull
    public static Specification<ReferenceVO> hasReferenceName(String referenceName) {
        return (root, query, cb) -> cb.equal(root.get(FieldConstants.REFERENCE_NAME), referenceName);
    }

    @NonNull
    public static Specification<ReferenceVO> hasReferenceCode(String referenceCode) {
        return (root, query, cb) -> cb.equal(root.get(FieldConstants.REFERENCE_CODE), referenceCode);
    }

    @NonNull
    public static Specification<ReferenceVO> isActive(Boolean active) {
        return (root, query, cb) -> cb.equal(root.get(FieldConstants.ACTIVE), active);
    }
}