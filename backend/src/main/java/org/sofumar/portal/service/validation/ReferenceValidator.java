package org.sofumar.portal.service.validation;

import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.sofumar.portal.constants.MessagesConstants;
import org.sofumar.portal.data.vo.ReferenceVO;
import org.sofumar.portal.framework.vo.ValueObject;
import org.sofumar.portal.repo.ReferenceRepository;
import org.sofumar.portal.repo.jpaspec.ReferenceSpecifications;
import org.springframework.stereotype.Service;

import org.springframework.data.jpa.domain.Specification;

@Service
@RequiredArgsConstructor
public class ReferenceValidator {

    private final ReferenceRepository referenceRepository;

    public void validate(ValueObject vo, String fieldName, String referenceName, String referenceCode) {
        if (StringUtils.isNotBlank(referenceCode)) {
            Specification<ReferenceVO> spec = ReferenceSpecifications.hasReferenceName(referenceName)
                    .and(ReferenceSpecifications.hasReferenceCode(referenceCode))
                    .and(ReferenceSpecifications.isActive(true));

            if (!referenceRepository.exists(spec)) {
                vo.addFieldMessage(fieldName, MessagesConstants.INVALID_VALUE);
            }
        }
    }
}