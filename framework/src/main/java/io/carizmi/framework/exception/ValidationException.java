package io.carizmi.framework.exception;

import lombok.Getter;
import io.carizmi.framework.vo.ValueObject;

@Getter
public class ValidationException extends RuntimeException {

    private final ValueObject vo;

    public ValidationException(ValueObject vo) {
        super("Validation failed for " + vo.getTableName());
        this.vo = vo;
    }

    public ValidationException(String message) {
        super(message);
        this.vo = null;
    }

}