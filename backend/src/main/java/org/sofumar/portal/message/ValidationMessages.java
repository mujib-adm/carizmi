package org.sofumar.portal.message;

import org.sofumar.portal.framework.message.constant.CommonMessages;
import org.sofumar.portal.framework.message.ErrorMessage;
import org.sofumar.portal.framework.message.StatusMessage;

public final class ValidationMessages {

    private ValidationMessages() {
        // Private constructor to prevent instantiation
    }

    // Status Messages
    public static final StatusMessage RECORD_ADDED = new StatusMessage("STS0001", "{0} added successfully.");
    public static final StatusMessage RECORD_UPDATED = new StatusMessage("STS0002", "{0} updated successfully.");
    public static final StatusMessage RECORD_DELETED = new StatusMessage("STS0003", "{0} deleted successfully.");

    // Error Messages
    public static final ErrorMessage REQUIRED_FIELD = CommonMessages.REQUIRED_FIELD;
    public static final ErrorMessage INVALID_VALUE = CommonMessages.INVALID_VALUE;
    public static final ErrorMessage ALREADY_EXISTS = CommonMessages.ALREADY_EXISTS;
    public static final ErrorMessage INVALID_USERNAME = new ErrorMessage("ERR0004", "Invalid Username. Must start with a letter and be 4 characters long minimum, containing only letters, digits, and underscores.");
    public static final ErrorMessage INVALID_PASSWORD = new ErrorMessage("ERR0005", "Weak password. Must include lowercase, uppercase, and digit/special character.");
    public static final ErrorMessage INVALID_ROLE = new ErrorMessage("ERR0006", "The assigned role is invalid.");
    public static final ErrorMessage ERR_PAYMENT_ALREADY_EXISTS = new ErrorMessage("ERR0007", "Payment for {0}-Q{1} already exists.");
    public static final ErrorMessage ERR_LAST_ADMIN_ROLE = new ErrorMessage("ERR0008", "Cannot update role for the last active ADMIN.");
    public static final ErrorMessage ERR_LAST_ADMIN_DEACTIVATE = new ErrorMessage("ERR0009", "Cannot deactivate the last active ADMIN.");

}