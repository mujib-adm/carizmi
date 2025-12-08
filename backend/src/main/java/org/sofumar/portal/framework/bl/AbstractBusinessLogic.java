package org.sofumar.portal.framework.bl;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.sofumar.portal.framework.constants.GlobalMessageConstants;
import org.sofumar.portal.framework.dao.sql.exception.DuplicateRecordException;
import org.sofumar.portal.framework.util.ExceptionParserUtils;
import org.sofumar.portal.framework.util.LabelUtils;
import org.sofumar.portal.framework.util.MySQLConstraintResolverUtils;
import org.sofumar.portal.framework.vo.ValueObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.List;
import java.util.Optional;

public abstract class AbstractBusinessLogic<V extends ValueObject, R extends JpaRepository<V, Integer>> {

    //    @Autowired
//    private ConstraintRegistry constraintRegistry;
    @Autowired
    private MySQLConstraintResolverUtils constraintResolver;

    protected abstract R getRepository();

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

    public V add(V vo) {
        beforeAdd(vo);
        if (!vo.hasErrors()) {
            validateInternal(vo, false);
            if (!vo.hasErrors()) {
                try {
                    vo = getRepository().save(vo);
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
                    //throw new DataAccessException("Failed to create record(s) for " + vo.getTableName(), e) {};
                }
//                catch (DataIntegrityViolationException e) {
//                    getLogger().error("Duplicate record detected in {} table.", vo.getTableName(), e);
//                    vo.addGlobalMessage(GlobalMessageConstants.ERR_RECORD_ALREADY_EXISTS.addMessageArgs(vo.getTableName()));
//                }
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
                    vo = getRepository().save(vo);
                    vo.addGlobalMessage(GlobalMessageConstants.STS_RECORD_UPDATED);
                    afterExecuteUpdate(vo);
                } catch (ObjectOptimisticLockingFailureException e) {
                    getLogger().error("Record was updated by another transaction, table: {}.", vo.getTableName(), e);
                    vo.addGlobalMessage(GlobalMessageConstants.ERR_RECORD_STALE_DETECTED.addMessageArgs(vo.getTableName()));
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
            getRepository().delete(vo);
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
            getRepository().deleteAll(vos);
            for (V vo : vos) {
                vo.addGlobalMessage(GlobalMessageConstants.STS_RECORD_DELETED);
            }
        } catch (DataAccessException e) {
            V firstVo = vos.getFirst();
            getLogger().error("Failed to delete record(s) from {}.", firstVo.getTableName(), e);
            firstVo.addGlobalMessage(GlobalMessageConstants.ERR_FAILED_TO_DELETE_RECORD.addMessageArgs(firstVo.getTableName()));
        }
    }

//    private List<String> resolveFieldsFromConstraint(ValueObject vo, String constraintName) {
//        if (constraintName == null) return List.of("unknown");
//
//        Table tableAnnotation = vo.getClass().getAnnotation(Table.class);
//        if (tableAnnotation != null) {
//            for (UniqueConstraint uc : tableAnnotation.uniqueConstraints()) {
//                if (uc.name().equalsIgnoreCase(constraintName)) {
//                    return Arrays.asList(uc.columnNames());
//                }
//            }
//        }
//        return List.of("unknown");
//    }

    private void addErrorMessages(V vo, Throwable rootError) {
        String constraintName = null;
        if (ExceptionParserUtils.isDuplicateEntry(rootError.getMessage())) {
            Optional<ExceptionParserUtils.DuplicateEntryInfo> duplicateEntryInfo = ExceptionParserUtils.parseDuplicateEntry(rootError.getMessage());
            constraintName = duplicateEntryInfo.map(ExceptionParserUtils.DuplicateEntryInfo::constraint).orElse(null);
        }

        if (constraintName != null) {
            // Resolve the fields involved in the constraint violation using ConstraintRegistry
            List<String> fields = constraintResolver.resolveFields(vo.getTableName(), constraintName);
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