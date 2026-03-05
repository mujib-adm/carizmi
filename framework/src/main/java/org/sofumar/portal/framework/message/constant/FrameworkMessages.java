package org.sofumar.portal.framework.message.constant;

import org.sofumar.portal.framework.message.ErrorMessage;
import org.sofumar.portal.framework.message.StatusMessage;

/**
 * Standard status and error messages used in framework.
 */
public final class FrameworkMessages {

    private FrameworkMessages() {
        // Private constructor to prevent instantiation
    }

    // Status Messages
    public static final StatusMessage STS_RECORD_ADDED = new StatusMessage("STS0001", "Record(s) added successfully.");
    public static final StatusMessage STS_RECORD_UPDATED = new StatusMessage("STS0002", "Record(s) updated successfully.");
    public static final StatusMessage STS_RECORD_DELETED = new StatusMessage("STS0003", "Record(s) deleted successfully.");

    // Error Messages
    public static final ErrorMessage ERR_FAILED_TO_CREATE_RECORD = new ErrorMessage("ERR0001", "Failed to create record(s) for {0}.");
    public static final ErrorMessage ERR_FAILED_TO_UPDATE_RECORD = new ErrorMessage("ERR0002", "Failed to update record(s) for {0}.");
    public static final ErrorMessage ERR_FAILED_TO_DELETE_RECORD = new ErrorMessage("ERR0003", "Failed to delete record(s) for {0}.");
    public static final ErrorMessage ERR_RECORD_ALREADY_EXISTS = new ErrorMessage("ERR0004", "{0} {1} already exists.");
    public static final ErrorMessage ERR_RECORD_STALE_DETECTED = new ErrorMessage("ERR0005", "Record was updated by another transaction, table: {0}.");
    public static final ErrorMessage ERR_DELETE_RECORD_NOT_FOUND = new ErrorMessage("ERR0006", "Delete failed, record not found in {0}.");
}