package org.sofumar.portal.service.validation;

import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.sofumar.portal.constants.MessagesConstants;
import org.sofumar.portal.framework.vo.ValueObject;
import org.sofumar.portal.repo.ReferenceRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReferenceValidator {

    private final ReferenceRepository referenceRepository;

    public void validate(ValueObject vo, String fieldName, String referenceName, String referenceCode) {
        if (StringUtils.isNotBlank(referenceCode)) {
            boolean exists = referenceRepository.existsByReferenceNameAndReferenceCodeAndActiveTrue(referenceName,
                    referenceCode);
            if (!exists) {
                vo.addFieldMessage(fieldName, MessagesConstants.INVALID_VALUE);
            }
        }
    }
}