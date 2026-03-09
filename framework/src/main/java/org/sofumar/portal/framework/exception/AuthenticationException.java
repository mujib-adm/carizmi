package org.sofumar.portal.framework.exception;

public class AuthenticationException extends RuntimeException {
    public AuthenticationException() {
        super("Authentication check failed");
    }
}