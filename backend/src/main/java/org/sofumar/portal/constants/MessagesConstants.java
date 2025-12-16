package org.sofumar.portal.constants;

import org.sofumar.portal.data.message.ErrorMessage;
import org.sofumar.portal.data.message.StatusMessage;

public final class MessagesConstants {

    private MessagesConstants() {
        // Private constructor to prevent instantiation
    }

    // Status Messages
    public static final StatusMessage RECORD_ADDED = new StatusMessage("STS0001", "{0} added successfully.");
    public static final StatusMessage RECORD_UPDATED = new StatusMessage("STS0002", "{0} updated successfully.");

    // Error Messages
    public static final ErrorMessage REQUIRED_FIELD = new ErrorMessage("ERR0001", "{0} is required.");
    public static final ErrorMessage INVALID_VALUE = new ErrorMessage("ERR0002", "Invalid value.");
    public static final ErrorMessage INVALID_USERNAME = new ErrorMessage("ERR0003", "Invalid Username. Must start with a letter and be 4 characters long minimum, containing only letters, digits, and underscores.");
    public static final ErrorMessage INVALID_PASSWORD = new ErrorMessage("ERR0004", "Weak password. Must include lowercase, uppercase, and digit/special character.");

}
