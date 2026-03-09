package org.sofumar.portal.framework.bl;

import org.apache.commons.lang3.StringUtils;
import org.sofumar.portal.framework.message.constant.CommonMessages;
import org.sofumar.portal.framework.message.Message;
import org.sofumar.portal.framework.util.LabelUtils;
import org.sofumar.portal.framework.vo.ValueObject;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Base abstract class for domain validators providing common validation utilities.
 *
 * @param <V> the ValueObject type
 */
public abstract class AbstractDomainValidator<V extends ValueObject> implements DomainValidator<V> {

    /**
     * Validates required field.
     *
     * @param vo        the value object
     * @param fieldName the name of the field
     * @param value     the value to check
     */
    protected void validateRequired(V vo, String fieldName, Object value) {
        boolean invalid = switch (value) {
            case null -> true;
            case String s -> s.isBlank();
            case BigDecimal bd -> bd.compareTo(BigDecimal.ZERO) == 0;
            case BigInteger bi -> bi.signum() == 0;
            // All other Number types (Integer, Long, Double, Float, Short, Byte)
            case Number n -> n.doubleValue() == 0d;
            default -> false;
        };

        if (!invalid) {
            return;
        }
        vo.addFieldMessage(fieldName, CommonMessages.REQUIRED_FIELD.addMessageArgs(LabelUtils.toLabel(fieldName)));
    }

    /**
     * Validates a field against a regex pattern.
     *
     * @param vo        the value object
     * @param fieldName the name of the field
     * @param value     the value to check
     * @param regex     the regex pattern
     */
    protected void validateRegex(V vo, String fieldName, String value, String regex) {
        if (isNotMatchRegex(value, regex)) {
            vo.addFieldMessage(fieldName, CommonMessages.INVALID_VALUE);
        }
    }

    /**
     * This version supports specific error message with 'param message' instead of default "Invalid value".
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