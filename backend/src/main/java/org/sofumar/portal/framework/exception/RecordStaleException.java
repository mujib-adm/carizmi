package org.sofumar.portal.framework.exception;

public class RecordStaleException extends RuntimeException {
    public RecordStaleException(String message) {
        super(message);
    }
}