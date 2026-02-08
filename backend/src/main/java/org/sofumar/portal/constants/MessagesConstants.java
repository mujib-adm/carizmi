package org.sofumar.portal.constants;

import org.sofumar.portal.framework.data.msg.ErrMessage;
import org.sofumar.portal.framework.data.msg.StsMessage;

public final class MessagesConstants {

    private MessagesConstants() {
        // Private constructor to prevent instantiation
    }

    // Status Messages
    public static final StsMessage RECORD_ADDED = new StsMessage("STS0001", "{0} added successfully.");
    public static final StsMessage RECORD_UPDATED = new StsMessage("STS0002", "{0} updated successfully.");
    public static final StsMessage RECORD_DELETED = new StsMessage("STS0003", "{0} deleted successfully.");

    // Error Messages
    public static final ErrMessage REQUIRED_FIELD = new ErrMessage("ERR0001", "{0} is required.");
    public static final ErrMessage INVALID_VALUE = new ErrMessage("ERR0002", "Invalid value.");
    public static final ErrMessage ALREADY_EXISTS = new ErrMessage("ERR0003", "{0} already exists.");
    public static final ErrMessage INVALID_USERNAME = new ErrMessage("ERR0004", "Invalid Username. Must start with a letter and be 4 characters long minimum, containing only letters, digits, and underscores.");
    public static final ErrMessage INVALID_PASSWORD = new ErrMessage("ERR0005", "Weak password. Must include lowercase, uppercase, and digit/special character.");
    public static final ErrMessage INVALID_ROLE = new ErrMessage("ERR0006", "The assigned role is invalid.");
    public static final ErrMessage ERR_PAYMENT_ALREADY_EXISTS = new ErrMessage("ERR0007", "Payment for {0}-Q{1} already exists.");

}
