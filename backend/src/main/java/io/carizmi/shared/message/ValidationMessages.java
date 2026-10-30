package io.carizmi.shared.message;

import io.carizmi.framework.message.constant.CommonMessages;
import io.carizmi.framework.message.ErrorMessage;
import io.carizmi.framework.message.StatusMessage;

public final class ValidationMessages {

    private ValidationMessages() {
        // Private constructor to prevent instantiation
    }

    // Status Messages
    public static final StatusMessage RECORD_ADDED = new StatusMessage("STS0004", "{0} added successfully.");
    public static final StatusMessage RECORD_UPDATED = new StatusMessage("STS0005", "{0} updated successfully.");
    public static final StatusMessage RECORD_DELETED = new StatusMessage("STS0006", "{0} deleted successfully.");
    public static final StatusMessage FISCAL_YEAR_CLOSED_SUCCESS = new StatusMessage("STS0007", "Fiscal year {0} has been officially closed and frozen.");

    // Error Messages
    public static final ErrorMessage INVALID_VALUE = CommonMessages.INVALID_VALUE;
    public static final ErrorMessage ALREADY_EXISTS = CommonMessages.ALREADY_EXISTS;
    public static final ErrorMessage RECORD_NOT_FOUND = new ErrorMessage("ERR0015", "Record not found.");
    public static final ErrorMessage INVALID_USERNAME = new ErrorMessage("ERR0016", "Invalid Username. Must start with a letter and be 4 characters long minimum, containing only letters, digits, and underscores.");
    public static final ErrorMessage INVALID_PASSWORD = new ErrorMessage("ERR0017", "Weak password. Must include lowercase, uppercase, and digit/special character.");
    public static final ErrorMessage INVALID_ROLE = new ErrorMessage("ERR0018", "The assigned role is invalid.");
    public static final ErrorMessage ERR_PAYMENT_ALREADY_EXISTS = new ErrorMessage("ERR0019", "Payment for {0}-Q{1} already exists.");
    public static final ErrorMessage ERR_LAST_ADMIN_ROLE = new ErrorMessage("ERR0020", "Cannot update role for the last active ADMIN.");
    public static final ErrorMessage ERR_LAST_ADMIN_DEACTIVATE = new ErrorMessage("ERR0021", "Cannot deactivate the last active ADMIN.");
    public static final ErrorMessage ERR_FISCAL_YEAR_CLOSED = new ErrorMessage("ERR0022", "Fiscal year {0} is closed. Backdating transactions into a closed fiscal year is prohibited.");
    public static final ErrorMessage ERR_MEMBER_HISTORICAL_STANDING = new ErrorMessage("ERR0023", "Cannot delete member. Member has recorded payments or was active during a closed fiscal year. Please update status to Inactive instead.");
    public static final ErrorMessage ERR_TRANSACTION_CLOSED_YEAR_DELETE = new ErrorMessage("ERR0024", "Cannot delete transaction. Fiscal year {0} is closed.");
    public static final ErrorMessage ERR_TRANSACTION_CLOSED_YEAR_MUTATION = new ErrorMessage("ERR0025", "Cannot modify transaction. Fiscal year {0} is closed.");
    public static final ErrorMessage ERR_LEAVE_BEFORE_JOIN = new ErrorMessage("ERR0026", "Leave date cannot be before join date.");
    public static final ErrorMessage ERR_PAYMENT_MEMBER_NOT_ACTIVE_IN_PERIOD = new ErrorMessage("ERR0027", "Member was not active during {0} Q{1}. Cannot record membership fee for an inactive period.");
    public static final ErrorMessage ERR_PAYMENT_MEMBER_PENDING = new ErrorMessage("ERR0028", "Member is in PENDING status. Cannot record payments for pending members.");
    public static final ErrorMessage ERR_REGISTRATION_FEE_ALREADY_EXISTS = new ErrorMessage("ERR0029", "Registration fee has already been recorded for this member.");
    public static final ErrorMessage ERR_FISCAL_YEAR_NOT_ENDED = new ErrorMessage("ERR0030", "Cannot close fiscal year {0} before the year has ended.");
    public static final ErrorMessage ERR_FISCAL_YEAR_NO_BASELINE = new ErrorMessage("ERR0031", "Cannot close fiscal year {0} because no opening baseline exists for that year.");

}