package org.sofumar.portal.constants;

import org.sofumar.portal.data.message.ErrorMessage;

import org.springframework.http.HttpStatus;

public final class MessagesConstants {

    private MessagesConstants() {
        // Private constructor to prevent instantiation
    }

    // Error Messages
    public static final ErrorMessage USERNAME_EXIST = new ErrorMessage("ERR0001", "Username already exists.", HttpStatus.CONFLICT);
    public static final ErrorMessage INVALID_USERNAME = new ErrorMessage("ERR0002", "Invalid Username. Must start with a letter and be 4 characters long minimum, containing only letters, digits, and underscores.", HttpStatus.BAD_REQUEST);
    public static final ErrorMessage INVALID_PASSWORD = new ErrorMessage("ERR0002", "Weak password. Must include lowercase, uppercase, and digit/special character.", HttpStatus.BAD_REQUEST);

}
