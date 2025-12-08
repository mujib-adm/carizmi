package org.sofumar.portal.framework.dao.sql.exception;

public class RecordStaleException extends RuntimeException {
    public RecordStaleException(String message) {
        super(message);
    }
}