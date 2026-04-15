package io.carizmi.framework.exception;

import lombok.Getter;
import io.carizmi.framework.vo.ValueObject;

@Getter
public class DuplicateRecordException extends RuntimeException {

    private final ValueObject vo;

    public DuplicateRecordException(ValueObject vo) {
        super("Duplicate record detected in " + vo.getTableName() + " table.");
        this.vo = vo;
    }

}