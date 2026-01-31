package org.sofumar.portal.framework.constants;

import org.sofumar.portal.framework.data.msg.ErrMessage;
import org.sofumar.portal.framework.data.msg.StsMessage;

public class GlobalMessageConstants {

    // Status Messages
    public static final StsMessage STS_RECORD_ADDED = new StsMessage("STS0001", "Record(s) added successfully.");
    public static final StsMessage STS_RECORD_UPDATED = new StsMessage("STS0002", "Record(s) updated successfully.");
    public static final StsMessage STS_RECORD_DELETED = new StsMessage("STS0003", "Record(s) deleted successfully.");

    // Error Messages
    public static final ErrMessage ERR_FAILED_TO_CREATE_RECORD = new ErrMessage("ERR0001", "Failed to create record(s) for {0}.");
    public static final ErrMessage ERR_FAILED_TO_UPDATE_RECORD = new ErrMessage("ERR0002", "Failed to update record(s) for {0}.");
    public static final ErrMessage ERR_FAILED_TO_DELETE_RECORD = new ErrMessage("ERR0003", "Failed to delete record(s) for {0}.");
    public static final ErrMessage ERR_RECORD_ALREADY_EXISTS = new ErrMessage("ERR0004", "{0} {1} already exists.");
    public static final ErrMessage ERR_RECORD_STALE_DETECTED = new ErrMessage("ERR0005", "Record was updated by another transaction, table: {0}.");
    public static final ErrMessage ERR_DELETE_RECORD_NOT_FOUND = new ErrMessage("ERR0006", "Delete failed, record not found in {0}.");
}