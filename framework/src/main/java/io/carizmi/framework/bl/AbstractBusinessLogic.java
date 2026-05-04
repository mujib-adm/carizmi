package io.carizmi.framework.bl;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import io.carizmi.framework.annotation.DomainLogicFor;
import io.carizmi.framework.event.DomainEventPublisher;
import io.carizmi.framework.message.constant.FrameworkMessages;
import io.carizmi.framework.exception.DuplicateRecordException;
import io.carizmi.framework.exception.ValidationException;
import io.carizmi.framework.util.ExceptionParserUtils;
import io.carizmi.framework.util.LabelUtils;
import io.carizmi.framework.util.MySQLConstraintResolver;
import io.carizmi.framework.vo.ValueObject;
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
 * Write-side lifecycle engine for all domain entities.
 *
 * <p>This class implements the Template Method pattern for CRUD operations,
 * ensuring that validation, error handling, lifecycle hooks, and domain event
 * publishing are executed consistently across every domain entity.</p>
 *
 * <h2>Architecture Overview</h2>
 * <p>
 * This is the <b>write-side counterpart</b> to {@link io.carizmi.framework.projection.AbstractProjector},
 * which governs read-side event projections. Together they form the two halves of the
 * CQRS (Command Query Responsibility Segregation) architecture:
 * </p>
 * <ul>
 *   <li><b>Write-Side</b> ({@code AbstractBusinessLogic}): Controller → BL → Validator → Repository → DomainEvent</li>
 *   <li><b>Read-Side</b> ({@code AbstractProjector}): DomainEvent → Projector → ReadModel Repository → Service</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <p>Subclasses must implement three abstract methods:</p>
 * <ul>
 *   <li>{@link #getLogger()} — provides the logger for the concrete BL subclass</li>
 *   <li>{@link #getId(ValueObject)} — extracts the primary key from the entity for event publishing</li>
 *   <li>{@link #performDomainValidation(ValueObject, boolean)} — domain-specific validation rules</li>
 * </ul>
 *
 * <p>Subclasses may optionally override lifecycle hooks:</p>
 * <ul>
 *   <li>{@link #beforeAdd(ValueObject)} — called before validation and save on ADD</li>
 *   <li>{@link #beforeUpdate(ValueObject)} — called before validation and save on UPDATE</li>
 *   <li>{@link #afterExecuteAdd(ValueObject)} — called after successful save on ADD</li>
 *   <li>{@link #afterExecuteUpdate(ValueObject)} — called after successful save on UPDATE</li>
 * </ul>
 *
 * <h2>Singleton Enforcement</h2>
 * <p>The framework enforces a strict 1-to-1 relationship between a ValueObject type
 * and its Business Logic implementation. If two BL classes attempt to register for
 * the same VO type, an {@link IllegalStateException} is thrown at startup.</p>
 *
 * @param <V> the ValueObject type
 * @param <R> the Repository type, must extend {@link JpaRepository} and {@link JpaSpecificationExecutor}
 */
public abstract class AbstractBusinessLogic<V extends ValueObject, R extends JpaRepository<V, Integer> & JpaSpecificationExecutor<V>> implements BusinessLogic<V>, InitializingBean {

    private static final Map<Class<?>, Class<?>> registry = new ConcurrentHashMap<>();

    protected final R repo;

    @Autowired
    private MySQLConstraintResolver constraintResolver;

    @Autowired
    private DomainEventPublisher domainEventPublisher;

    protected AbstractBusinessLogic(R repo) {
        this.repo = Objects.requireNonNull(repo, "repo must not be null");
    }

    protected final R getRepo() {
        return repo;
    }

    // ─── Abstract Contract (subclass MUST implement) ───────────────────

    /**
     * Returns the logger for the concrete BL subclass.
     *
     * @return the SLF4J logger
     */
    protected abstract Logger getLogger();

    /**
     * Extracts the primary key from the ValueObject.
     *
     * <p>Used by the domain event infrastructure to include the aggregate ID
     * in published {@link io.carizmi.framework.event.DomainEvent} records.
     * Each sealed abstract BL must implement this to return the entity's ID
     * (e.g., {@code return vo.getPaymentID()}).</p>
     *
     * @param vo the value object
     * @return the primary key, or null if not yet persisted
     */
    protected abstract Integer getId(V vo);

    /**
     * Performs domain-specific validation for the given entity.
     *
     * <p>Implementation should delegate to a {@link DomainValidator} or perform
     * checks directly, adding error messages to the VO using
     * {@code vo.addFieldMessage()} or {@code vo.addGlobalMessage()}.
     * The framework checks {@code vo.hasErrors()} after this method returns
     * and throws a {@link ValidationException} if errors were found.</p>
     *
     * @param vo       the value object to validate
     * @param isUpdate true if this is an update operation, false for add
     */
    protected abstract void performDomainValidation(V vo, boolean isUpdate);

    // ─── Lifecycle Hooks (subclass MAY override) ───────────────────────

    /**
     * Hook called before validation and save in an ADD operation.
     * Override to set defaults, enrich data, or perform pre-processing.
     *
     * @param vo the value object being added
     */
    protected void beforeAdd(V vo) {
        // No-op by default
    }

    /**
     * Hook called before validation and save in an UPDATE operation.
     * Override to set defaults, enrich data, or perform pre-processing.
     *
     * @param vo the value object being updated
     */
    protected void beforeUpdate(V vo) {
        // No-op by default
    }

    /**
     * Hook called after a successful save in an ADD operation.
     * Override to trigger side effects that depend on the persisted entity.
     *
     * @param vo the saved value object (with generated ID populated)
     */
    protected void afterExecuteAdd(V vo) {
        // No-op by default
    }

    /**
     * Hook called after a successful save in an UPDATE operation.
     * Override to trigger side effects that depend on the updated entity.
     *
     * @param vo the saved value object
     */
    protected void afterExecuteUpdate(V vo) {
        // No-op by default
    }

    /**
     * Controls whether the domain entity participates in event-driven flows.
     *
     * <p>Returns {@code false} by default — event publishing is opt-in.
     * Override to return {@code true} for domain entities whose lifecycle changes
     * should propagate to projectors, the outbox, and external consumers.</p>
     */
    protected boolean publishesDomainEvents() {
        return false;
    }

    // ─── Runtime Enforcement (framework controls registration) ───────

    /**
     * Enforces critical architectural invariants at Spring context startup.
     *
     * <p>Two rules are validated per BL instance:</p>
     * <ol>
     *   <li><b>VO → BL Uniqueness:</b> Only one BL implementation may exist per
     *       ValueObject type. Duplicates throw {@link IllegalStateException},
     *       preventing ambiguous routing.</li>
     *   <li><b>Annotation ↔ Generic Cross-Validation:</b> If {@link DomainLogicFor}
     *       is present, the declared VO class must match the generic type parameter
     *       {@code <V>}. A mismatch indicates a copy-paste error that no other
     *       enforcement layer can detect.</li>
     * </ol>
     */
    @Override
    public void afterPropertiesSet() {
        Class<?>[] typeArgs = GenericTypeResolver.resolveTypeArguments(getClass(), AbstractBusinessLogic.class);
        if (typeArgs != null && typeArgs.length > 0) {
            Class<?> voClass = typeArgs[0];

            // ── Rule 1: VO → BL uniqueness ─────────────────────────────────
            synchronized (registry) {
                if (registry.containsKey(voClass)) {
                    throw new IllegalStateException("Duplicate Business Logic detected for VO: " + voClass.getName() +
                            ". Existing=" + registry.get(voClass).getName() + ", New=" + getClass().getName());
                }
                registry.put(voClass, getClass());
            }

            // ── Rule 2: @DomainLogicFor ↔ Generic <V> cross-validation ───
            DomainLogicFor annotation = getClass().getAnnotation(DomainLogicFor.class);
            if (annotation == null) {
                Class<?> parent = getClass().getSuperclass();
                if (parent != null) {
                    annotation = parent.getAnnotation(DomainLogicFor.class);
                }
            }
            if (annotation != null && !annotation.value().equals(voClass)) {
                throw new IllegalStateException(
                        "@DomainLogicFor mismatch in " + getClass().getName() +
                        ": annotation declares " + annotation.value().getName() +
                        " but generic type parameter is " + voClass.getName());
            }
        }
    }

    // ─── Template Methods (framework controls the flow) ────────────────

    @Override
    public boolean exists(@NonNull Specification<V> spec) {
        return getRepo().exists(spec);
    }

    /**
     * Creates a new entity.
     *
     * <p>Lifecycle: {@code beforeAdd → performDomainValidation → repo.save
     * → afterExecuteAdd → publish CREATED event}</p>
     *
     * @param vo the value object to persist
     * @return the saved value object with generated ID and status messages
     * @throws ValidationException if domain validation fails
     * @throws DuplicateRecordException if a unique constraint is violated
     */
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
                    if (publishesDomainEvents()) {
                        domainEventPublisher.publish("CREATED", vo, getId(vo));
                    }
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

    /**
     * Updates an existing entity.
     *
     * <p>Lifecycle: {@code beforeUpdate → performDomainValidation → repo.save
     * → afterExecuteUpdate → publish UPDATED event}</p>
     *
     * @param vo the value object to update
     * @return the saved value object with status messages
     * @throws ValidationException if domain validation fails
     * @throws DuplicateRecordException if a unique constraint is violated
     */
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
                    if (publishesDomainEvents()) {
                        domainEventPublisher.publish("UPDATED", vo, getId(vo));
                    }
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

    /**
     * Deletes a single entity and publishes a DELETED domain event.
     *
     * @param vo the value object to delete
     */
    @Override
    public void delete(@NonNull V vo) {
        try {
            getRepo().delete(vo);

            vo.addGlobalMessage(FrameworkMessages.STS_RECORD_DELETED);
            if (publishesDomainEvents()) {
                domainEventPublisher.publish("DELETED", vo, getId(vo));
            }
        } catch (EmptyResultDataAccessException e) {
            getLogger().error("Delete failed, record not found in {}.", vo.getTableName(), e);
            vo.addGlobalMessage(FrameworkMessages.ERR_DELETE_RECORD_NOT_FOUND.addMessageArgs(vo.getTableName()));
        } catch (DataAccessException e) {
            getLogger().error("Failed to delete record(s) from {}.", vo.getTableName(), e);
            vo.addGlobalMessage(FrameworkMessages.ERR_FAILED_TO_DELETE_RECORD.addMessageArgs(vo.getTableName()));
        }
    }

    /**
     * Deletes multiple entities in a single batch operation.
     *
     * @param vos the list of value objects to delete
     */
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

    // ─── Internal Helpers (framework-private) ──────────────────────────

    /**
     * Resolves constraint violation details from a database exception and
     * adds field-level or global error messages to the ValueObject.
     *
     * <p>If the root cause is a duplicate entry violation, this method
     * identifies the specific fields involved via {@link MySQLConstraintResolver}
     * and attaches targeted error messages. Otherwise, a generic creation
     * failure message is added.</p>
     */
    private void addErrorMessages(V vo, Throwable rootError) {
        String constraintName = null;
        if (rootError != null && ExceptionParserUtils.isDuplicateEntry(rootError.getMessage())) {
            Optional<ExceptionParserUtils.DuplicateEntryInfo> duplicateEntryInfo = ExceptionParserUtils.parseDuplicateEntry(rootError.getMessage());
            constraintName = duplicateEntryInfo.map(ExceptionParserUtils.DuplicateEntryInfo::constraint).orElse(null);
        }

        if (constraintName != null) {
            List<String> fields = constraintResolver.resolveFields(constraintName);
            for (String field : fields) {
                vo.addFieldMessage(field, FrameworkMessages.ERR_RECORD_ALREADY_EXISTS.addMessageArgs(
                        LabelUtils.toLabel(field), vo.getFieldValue(field)
                ));
            }
        } else {
            vo.addGlobalMessage(FrameworkMessages.ERR_FAILED_TO_CREATE_RECORD.addMessageArgs(LabelUtils.toLabel(vo.getTableName())));
        }
    }
}