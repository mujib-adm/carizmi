package org.sofumar.portal.framework.bl;

import org.apache.commons.lang3.StringUtils;
import org.sofumar.portal.framework.message.constant.CommonMessages;
import org.sofumar.portal.framework.message.Message;
import org.sofumar.portal.framework.util.LabelUtils;
import org.sofumar.portal.framework.vo.ValueObject;

/**
 * Base abstract class for domain validators providing common validation utilities.
 *
 * @param <V> the ValueObject type
 */
public abstract class AbstractDomainValidator<V extends ValueObject> implements DomainValidator<V> {

    /**
     * Validates if a field is required (not null or blank if it's a string).
     *
     * @param vo        the value object
     * @param fieldName the name of the field (from FieldConstants)
     * @param value     the value to check
     */
    protected void validateRequired(V vo, String fieldName, Object value) {
        if (value == null || (value instanceof String s && StringUtils.isBlank(s))) {
            vo.addFieldMessage(fieldName, CommonMessages.REQUIRED_FIELD.addMessageArgs(LabelUtils.toLabel(fieldName)));
        }
    }

    /**
     * Validates a field against a regex pattern.
     *
     * @param vo        the value object
     * @param fieldName the name of the field
     * @param value     the value to check
     * @param regex     the regex pattern
     * @param message   the error message to add if validation fails
     */
    protected void validateRegex(V vo, String fieldName, String value, String regex, Message message) {
        if (isNotMatchRegex(value, regex)) {
            vo.addFieldMessage(fieldName, message);
        }
    }

    /**
     * Helper to check if a value matches a regex.
     *
     * @param value the value to check
     * @param regex the regex pattern
     * @return true if it DOES NOT match or is blank
     */
    protected boolean isNotMatchRegex(String value, String regex) {
        if (StringUtils.isBlank(value)) {
            return true;
        }
        return !value.matches(regex);
    }
}