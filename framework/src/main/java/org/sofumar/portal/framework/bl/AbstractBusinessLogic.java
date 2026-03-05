package org.sofumar.portal.framework.bl;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.sofumar.portal.framework.message.constant.FrameworkMessages;
import org.sofumar.portal.framework.exception.DuplicateRecordException;
import org.sofumar.portal.framework.exception.ValidationException;
import org.sofumar.portal.framework.util.ExceptionParserUtils;
import org.sofumar.portal.framework.util.LabelUtils;
import org.sofumar.portal.framework.util.MySQLConstraintResolver;
import org.sofumar.portal.framework.vo.ValueObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.GenericTypeResolver;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.lang.NonNull;


/**
 * Base abstract class for Business Logic (BL) implementations providing the core lifecycle engine.
 * <p>
 * This class implements the template method pattern for CRUD operations, ensuring that
 * validation, error handling, and lifecycle hooks (before/after) are executed consistently.
 * </p>
 * <h2>Architecture Overview</h2>
 * <p>
 * 1. <b>Validation Pattern</b>: Logic should accumulate messages in the {@link ValueObject}
 * via {@code performDomainValidation()}. The framework check {@code vo.hasErrors()} and
 * throws {@link ValidationException} if needed.
 * </p>
 * <p>
 * 2. <b>Lifecycle Hooks</b>: Concrete implementations can override hooks like {@link #beforeAdd(ValueObject)}
 * or {@link #afterExecuteAdd(ValueObject)} to add custom logic at specific points in the execution.
 * </p>
 *
 * @param <V> the ValueObject type
 * @param <R> the Repository type, must extend {@link JpaRepository} and {@link JpaSpecificationExecutor}
 */
public abstract class AbstractBusinessLogic<V extends ValueObject, R extends JpaRepository<V, Integer> & JpaSpecificationExecutor<V>> implements BusinessLogic<V>, InitializingBean {

    private static final Map<Class<?>, Class<?>> registry = new ConcurrentHashMap<>();

    protected final R repo;

    @Autowired
    private MySQLConstraintResolver constraintResolver;

    protected AbstractBusinessLogic(R repo) {
        this.repo = Objects.requireNonNull(repo, "repo must not be null");
    }

    protected final R getRepo() {
        return repo;
    }

    /**
     * Standardized logger helper. Implementation should return a logger for the subclass.
     *
     * @return the logger
     */
    protected abstract Logger getLogger();

    /**
     * Standardized hook for domain-level validations.
     * <p>
     * Implementation should call a {@link DomainValidator} or perform checks directly,
     * adding error/warning messages to the VO using {@code vo.addFieldMessage()} or {@code vo.addGlobalMessage()}.
     * </p>
     *
     * @param vo       the value object to validate
     * @param isUpdate true if this is an update operation
     */
    protected abstract void performDomainValidation(V vo, boolean isUpdate);

    /**
     * Hook called before {@code repo.save()} in an ADD operation.
     * Note: This is called BEFORE {@code performDomainValidation()}.
     *
     * @param vo the value object being added
     */
    protected void beforeAdd(V vo) {
    }

    /**
     * Hook called before {@code repo.save()} in an UPDATE operation.
     * Note: This is called BEFORE {@code performDomainValidation()}.
     *
     * @param vo the value object being updated
     */
    protected void beforeUpdate(V vo) {
    }

    /**
     * Hook called after successful {@code repo.save()} in an ADD operation.
     *
     * @param vo the saved value object
     */
    protected void afterExecuteAdd(V vo) {
    }

    /**
     * Hook called after successful {@code repo.save()} in an UPDATE operation.
     *
     * @param vo the saved value object
     */
    protected void afterExecuteUpdate(V vo) {
    }

    @Override
    public void afterPropertiesSet() {
        Class<?>[] typeArgs = GenericTypeResolver.resolveTypeArguments(getClass(), AbstractBusinessLogic.class);
        if (typeArgs != null && typeArgs.length > 0) {
            Class<?> voClass = typeArgs[0];
            synchronized (registry) {
                if (registry.containsKey(voClass)) {
                    throw new IllegalStateException("Duplicate Business Logic detected for VO: " + voClass.getName() +
                            ". Existing=" + registry.get(voClass).getName() + ", New=" + getClass().getName());
                }
                registry.put(voClass, getClass());
            }
        }
    }

    @Override
    public boolean exists(@NonNull Specification<V> spec) {
        return getRepo().exists(spec);
    }

    @Override
    public V add(@NonNull V vo) {
        beforeAdd(vo);
        if (!vo.hasErrors()) {
            performDomainValidation(vo, false);
            if (!vo.hasErrors()) {
                try {
                    vo = getRepo().save(vo);
                    vo.addGlobalMessage(FrameworkMessages.STS_RECORD_ADDED);
                    afterExecuteAdd(vo);
                } catch (DataIntegrityViolationException e) {
                    Throwable rootError = e.getRootCause();
                    addErrorMessages(vo, rootError);
                    getLogger().error("Duplicate record detected in {} table.", vo.getTableName(), e);
                    throw new DuplicateRecordException(vo);

                } catch (DataAccessException e) {
                    Throwable rootError = e.getRootCause();
                    addErrorMessages(vo, rootError);
                    getLogger().error("Failed to create record(s) for {}.", vo.getTableName(), e);
                    vo.addGlobalMessage(FrameworkMessages.ERR_FAILED_TO_CREATE_RECORD.addMessageArgs(LabelUtils.toLabel(vo.getTableName())));
                }
            } else {
                throw new ValidationException(vo);
            }
        } else {
            throw new ValidationException(vo);
        }
        return vo;
    }

    @Override
    public V update(@NonNull V vo) {
        beforeUpdate(vo);
        if (!vo.hasErrors()) {
            performDomainValidation(vo, true);
            if (!vo.hasErrors()) {
                try {
                    vo = getRepo().save(vo);
                    vo.addGlobalMessage(FrameworkMessages.STS_RECORD_UPDATED);
                    afterExecuteUpdate(vo);
                } catch (ObjectOptimisticLockingFailureException e) {
                    getLogger().error("Record was updated by another transaction, table: {}.", vo.getTableName(), e);
                    vo.addGlobalMessage(FrameworkMessages.ERR_RECORD_STALE_DETECTED.addMessageArgs(vo.getTableName()));
                } catch (DataIntegrityViolationException e) {
                    Throwable rootError = e.getRootCause();
                    addErrorMessages(vo, rootError);
                    getLogger().error("Duplicate record detected in {} table.", vo.getTableName(), e);
                    throw new DuplicateRecordException(vo);
                } catch (DataAccessException e) {
                    getLogger().error("Failed to update record(s) for {}.", vo.getTableName(), e);
                    vo.addGlobalMessage(FrameworkMessages.ERR_FAILED_TO_UPDATE_RECORD.addMessageArgs(vo.getTableName()));
                }
            } else {
                throw new ValidationException(vo);
            }
        } else {
            throw new ValidationException(vo);
        }
        return vo;
    }

    @Override
    public void delete(@NonNull V vo) {
        try {
            getRepo().delete(vo);

            vo.addGlobalMessage(FrameworkMessages.STS_RECORD_DELETED);
        } catch (EmptyResultDataAccessException e) {
            getLogger().error("Delete failed, record not found in {}.", vo.getTableName(), e);
            vo.addGlobalMessage(FrameworkMessages.ERR_DELETE_RECORD_NOT_FOUND.addMessageArgs(vo.getTableName()));
        } catch (DataAccessException e) {
            getLogger().error("Failed to delete record(s) from {}.", vo.getTableName(), e);
            vo.addGlobalMessage(FrameworkMessages.ERR_FAILED_TO_DELETE_RECORD.addMessageArgs(vo.getTableName()));
        }
    }

    @Override
    public void delete(@NonNull List<V> vos) {
        if (CollectionUtils.isEmpty(vos)) {
            return;
        }
        try {
            getRepo().deleteAll(vos);
            for (V vo : vos) {
                vo.addGlobalMessage(FrameworkMessages.STS_RECORD_DELETED);
            }
        } catch (DataAccessException e) {
            V firstVo = vos.getFirst();
            getLogger().error("Failed to delete record(s) from {}.", firstVo.getTableName(), e);
            firstVo.addGlobalMessage(FrameworkMessages.ERR_FAILED_TO_DELETE_RECORD.addMessageArgs(firstVo.getTableName()));
        }
    }

    private void addErrorMessages(V vo, Throwable rootError) {
        String constraintName = null;
        if (rootError != null && ExceptionParserUtils.isDuplicateEntry(rootError.getMessage())) {
            Optional<ExceptionParserUtils.DuplicateEntryInfo> duplicateEntryInfo = ExceptionParserUtils.parseDuplicateEntry(rootError.getMessage());
            constraintName = duplicateEntryInfo.map(ExceptionParserUtils.DuplicateEntryInfo::constraint).orElse(null);
        }

        if (constraintName != null) {
            // Resolve the fields involved in the constraint violation
            List<String> fields = constraintResolver.resolveFields(constraintName);
            // Add field-specific error messages
            for (String field : fields) {
                vo.addFieldMessage(field, FrameworkMessages.ERR_RECORD_ALREADY_EXISTS.addMessageArgs(
                        LabelUtils.toLabel(field), vo.getFieldValue(field)
                ));
            }
        } else {
            // Fallback to a general error message if constraint name is not available
            vo.addGlobalMessage(FrameworkMessages.ERR_FAILED_TO_CREATE_RECORD.addMessageArgs(LabelUtils.toLabel(vo.getTableName())));
        }
    }
}