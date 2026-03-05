package org.sofumar.portal.framework.bl;

import org.sofumar.portal.framework.vo.ValueObject;

/**
 * Standardized interface for domain-level validators.
 * <p>
 * Validators implementing this interface act as pure "message accumulators" —
 * they add error/warning messages to the {@link ValueObject} but do NOT throw exceptions.
 * The framework ({@link AbstractBusinessLogic}) is responsible for checking
 * {@code vo.hasErrors()} after validation and throwing
 * {@link org.sofumar.portal.framework.exception.ValidationException} if needed.
 * </p>
 *
 * @param <V> the ValueObject type this validator applies to
 */
public interface DomainValidator<V extends ValueObject> {

    /**
     * Validates the given VO, adding field/global messages for any issues found.
     * Used for both add and update operations via {@code performDomainValidation()}.
     *
     * @param vo the value object to validate
     */
    void validate(V vo);

    /**
     * Validates the given VO for update operations.
     * Default implementation delegates to {@link #validate(V)}.
     * Override to add update-specific checks (e.g., ID required).
     *
     * @param vo the value object to validate for update
     */
    default void validateForUpdate(V vo) {
        validate(vo);
    }
}