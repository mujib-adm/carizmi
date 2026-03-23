package org.sofumar.portal.framework.message.constant;

import org.sofumar.portal.framework.message.ErrorMessage;

/**
 * Standard validation messages for the framework.
 */
public final class CommonMessages {

    private CommonMessages() {
        // Private constructor to prevent instantiation
    }

    public static final ErrorMessage REQUIRED_FIELD = new ErrorMessage("ERR0007", "{0} is required.");
    public static final ErrorMessage INVALID_VALUE = new ErrorMessage("ERR0008", "Invalid value.");
    public static final ErrorMessage ALREADY_EXISTS = new ErrorMessage("ERR0009", "{0} already exists.");

    public static  final ErrorMessage INVALID_CREDENTIALS = new ErrorMessage("ERR0010", "Invalid username or password.");
    public static  final ErrorMessage ACCOUNT_DISABLED = new ErrorMessage("ERR0011", "Your account is inactive.");
    public static  final ErrorMessage ACCOUNT_TEMP_LOCKED = new ErrorMessage("ERR0012", "Your account is temporarily locked after several sign‑in attempts. You may try again later or reset password.");
    public static final ErrorMessage AUTHENTICATION_FAILED = new ErrorMessage("ERR0013", "Authentication failed. Please sign in again to continue.");
    public static final ErrorMessage ACCESS_DENIED = new ErrorMessage("ERR0014", "You do not have the required permissions to access this resource or to perform this action.");
    public static final ErrorMessage TOO_MANY_REQUESTS = new ErrorMessage("ERR0015", "Too many requests. Please try again later.");

}