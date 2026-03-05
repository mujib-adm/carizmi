package org.sofumar.portal.framework.bl;

import org.sofumar.portal.framework.vo.ValueObject;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;

import java.util.List;

/**
 * Standardized interface for Business Logic (BL) components.
 * <p>
 * Implementations of this interface manage the lifecycle of a specific {@link ValueObject},
 * providing a consistent API for existence checks and CRUD operations.
 * </p>
 *
 * @param <V> the ValueObject type this BL manages
 */
public interface BusinessLogic<V extends ValueObject> {

    /**
     * Checks if a record exists matching the given specification.
     *
     * @param spec search criteria
     * @return true if matches found
     */
    boolean exists(@NonNull Specification<V> spec);

    /**
     * Adds a new record.
     * <p>
     * Follows the lifecycle: {@code beforeAdd()} -> {@code performDomainValidation()} -> {@code repo.save()}.
     * Throws {@link org.sofumar.portal.framework.exception.ValidationException} if validation fails.
     * </p>
     *
     * @param vo the record to add
     * @return the saved record
     */
    V add(@NonNull V vo);

    /**
     * Updates an existing record.
     * <p>
     * Follows the lifecycle: {@code beforeUpdate()} -> {@code performDomainValidation(true)} -> {@code repo.save()}.
     * Throws {@link org.sofumar.portal.framework.exception.ValidationException} if validation fails.
     * </p>
     *
     * @param vo the record to update
     * @return the saved record
     */
    V update(@NonNull V vo);

    /**
     * Deletes a record.
     *
     * @param vo the record to delete
     */
    void delete(@NonNull V vo);

    /**
     * Deletes multiple records.
     *
     * @param vo list of records to delete
     */
    void delete(@NonNull List<V> vo);
}