package io.carizmi.framework.exception;

public class AuthenticationException extends RuntimeException {
    public AuthenticationException() {
        super("Authentication check failed");
    }
}