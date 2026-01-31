package org.sofumar.portal.framework.bl;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.sofumar.portal.framework.constants.GlobalMessageConstants;
import org.sofumar.portal.framework.exception.DuplicateRecordException;
import org.sofumar.portal.framework.util.ExceptionParserUtils;
import org.sofumar.portal.framework.util.LabelUtils;
import org.sofumar.portal.framework.util.MySQLConstraintResolver;
import org.sofumar.portal.framework.vo.ValueObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.GenericTypeResolver;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractBusinessLogic<V extends ValueObject, R extends JpaRepository<V, Integer>> implements InitializingBean {

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

    protected abstract Logger getLogger();

    protected void validateInternal(V vo, boolean update) {
    }

    protected void beforeAdd(V vo) {
    }

    protected void beforeUpdate(V vo) {
    }

    protected void afterExecuteAdd(V vo) {
    }

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

    public V add(V vo) {
        beforeAdd(vo);
        if (!vo.hasErrors()) {
            validateInternal(vo, false);
            if (!vo.hasErrors()) {
                try {
                    vo = getRepo().save(vo);
                    vo.addGlobalMessage(GlobalMessageConstants.STS_RECORD_ADDED);
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
                    vo.addGlobalMessage(GlobalMessageConstants.ERR_FAILED_TO_CREATE_RECORD.addMessageArgs(LabelUtils.toLabel(vo.getTableName())));
                }
            }
        }
        return vo;
    }

    public V update(V vo) {
        beforeUpdate(vo);
        if (!vo.hasErrors()) {
            validateInternal(vo, true);
            if (!vo.hasErrors()) {
                try {
                    vo = getRepo().save(vo);
                    vo.addGlobalMessage(GlobalMessageConstants.STS_RECORD_UPDATED);
                    afterExecuteUpdate(vo);
                } catch (ObjectOptimisticLockingFailureException e) {
                    getLogger().error("Record was updated by another transaction, table: {}.", vo.getTableName(), e);
                    vo.addGlobalMessage(GlobalMessageConstants.ERR_RECORD_STALE_DETECTED.addMessageArgs(vo.getTableName()));
                } catch (DataIntegrityViolationException e) {
                    Throwable rootError = e.getRootCause();
                    addErrorMessages(vo, rootError);
                    getLogger().error("Duplicate record detected in {} table.", vo.getTableName(), e);
                    throw new DuplicateRecordException(vo);
                } catch (DataAccessException e) {
                    getLogger().error("Failed to update record(s) for {}.", vo.getTableName(), e);
                    vo.addGlobalMessage(GlobalMessageConstants.ERR_FAILED_TO_UPDATE_RECORD.addMessageArgs(vo.getTableName()));
                }
            }
        }
        return vo;
    }

    public void delete(V vo) {
        try {
            getRepo().delete(vo);
            vo.addGlobalMessage(GlobalMessageConstants.STS_RECORD_DELETED);
        } catch (EmptyResultDataAccessException e) {
            getLogger().error("Delete failed, record not found in {}.", vo.getTableName(), e);
            vo.addGlobalMessage(GlobalMessageConstants.ERR_DELETE_RECORD_NOT_FOUND.addMessageArgs(vo.getTableName()));
        } catch (DataAccessException e) {
            getLogger().error("Failed to delete record(s) from {}.", vo.getTableName(), e);
            vo.addGlobalMessage(GlobalMessageConstants.ERR_FAILED_TO_DELETE_RECORD.addMessageArgs(vo.getTableName()));
        }
    }

    public void delete(List<V> vos) {
        if (CollectionUtils.isEmpty(vos)) {
            return;
        }
        try {
            getRepo().deleteAll(vos);
            for (V vo : vos) {
                vo.addGlobalMessage(GlobalMessageConstants.STS_RECORD_DELETED);
            }
        } catch (DataAccessException e) {
            V firstVo = vos.getFirst();
            getLogger().error("Failed to delete record(s) from {}.", firstVo.getTableName(), e);
            firstVo.addGlobalMessage(GlobalMessageConstants.ERR_FAILED_TO_DELETE_RECORD.addMessageArgs(firstVo.getTableName()));
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
                vo.addFieldMessage(field, GlobalMessageConstants.ERR_RECORD_ALREADY_EXISTS.addMessageArgs(
                        LabelUtils.toLabel(field), vo.getFieldValue(field)
                ));
            }
        } else {
            // Fallback to a general error message if constraint name is not available
            vo.addGlobalMessage(GlobalMessageConstants.ERR_FAILED_TO_CREATE_RECORD.addMessageArgs(LabelUtils.toLabel(vo.getTableName())));
        }
    }
}