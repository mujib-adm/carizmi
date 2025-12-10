package org.sofumar.portal.framework.exception;

import lombok.Getter;
import org.sofumar.portal.framework.vo.ValueObject;

@Getter
public class ValidationException extends RuntimeException {

    private final ValueObject vo;

    public ValidationException(ValueObject vo) {
        super("Validation failed for " + vo.getTableName());
        this.vo = vo;
    }

}
