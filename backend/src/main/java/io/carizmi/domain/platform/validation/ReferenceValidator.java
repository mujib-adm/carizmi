package io.carizmi.domain.platform.validation;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import io.carizmi.shared.message.ValidationMessages;
import io.carizmi.framework.vo.ValueObject;
import io.carizmi.domain.platform.service.Reference;
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