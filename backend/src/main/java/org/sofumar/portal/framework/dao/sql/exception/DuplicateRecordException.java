package org.sofumar.portal.framework.dao.sql.exception;

import lombok.Getter;
import org.sofumar.portal.framework.vo.ValueObject;

@Getter
public class DuplicateRecordException extends RuntimeException {

    private final ValueObject vo;

    public DuplicateRecordException(ValueObject vo) {
        super("Duplicate record detected in " + vo.getTableName() + " table."); // optional summary
        this.vo = vo;
    }

//    private final String tableName;
//    private final List<String> fieldNames;
//    private final Map<String, Object> fieldValues;
//
//    public DuplicateRecordException(String tableName, List<String> fieldNames, Map<String, Object> fieldValues) {
//        super(buildMessage(tableName, fieldNames, fieldValues));
//        this.tableName = tableName;
//        this.fieldNames = fieldNames;
//        this.fieldValues = fieldValues;
//    }
//
//    private static String buildMessage(String tableName, List<String> fieldNames, Map<String, Object> fieldValues) {
//        StringBuilder sb = new StringBuilder(CaseUtils.toCamelCase(tableName, true) + " already exists with ");
//        for (String field : fieldNames) {
//            sb.append(field).append(": ").append(fieldValues.get(field)).append(" ");
//        }
//        return sb.toString().trim();
//    }

}