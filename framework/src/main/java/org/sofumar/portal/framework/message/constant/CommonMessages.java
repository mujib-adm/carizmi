package org.sofumar.portal.framework.message.constant;

import org.sofumar.portal.framework.message.ErrorMessage;

/**
 * Standard validation messages for the framework.
 */
public final class CommonMessages {

    private CommonMessages() {
        // Private constructor to prevent instantiation
    }

    public static final ErrorMessage REQUIRED_FIELD = new ErrorMessage("ERR0001", "{0} is required.");
    public static final ErrorMessage INVALID_VALUE = new ErrorMessage("ERR0002", "Invalid value.");
    public static final ErrorMessage ALREADY_EXISTS = new ErrorMessage("ERR0003", "{0} already exists.");

}