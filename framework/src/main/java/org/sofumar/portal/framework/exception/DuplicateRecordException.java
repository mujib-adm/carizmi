package org.sofumar.portal.framework.exception;

import lombok.Getter;
import org.sofumar.portal.framework.vo.ValueObject;

@Getter
public class DuplicateRecordException extends RuntimeException {

    private final ValueObject vo;

    public DuplicateRecordException(ValueObject vo) {
        super("Duplicate record detected in " + vo.getTableName() + " table.");
        this.vo = vo;
    }

}