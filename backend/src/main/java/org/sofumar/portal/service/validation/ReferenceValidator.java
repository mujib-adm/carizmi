package org.sofumar.portal.service.validation;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.sofumar.portal.message.ValidationMessages;
import org.sofumar.portal.framework.vo.ValueObject;
import org.sofumar.portal.core.businesslogic.Reference;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReferenceValidator {

    private final Reference reference;

    public void validate(ValueObject vo, String fieldName, String referenceName, String referenceCode) {
        if (StringUtils.isNotBlank(referenceCode)) {
            if (!reference.isValidReference(referenceName, referenceCode)) {
                vo.addFieldMessage(fieldName, ValidationMessages.INVALID_VALUE);
            }
        }
    }
}