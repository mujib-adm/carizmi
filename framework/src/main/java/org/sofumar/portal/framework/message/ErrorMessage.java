package org.sofumar.portal.framework.message;

/**
 * Represents an error message.
 */
public class ErrorMessage extends Message {

    public ErrorMessage(String messageID, String messageText) {
        super(MessageType.ERROR, messageID, messageText);
    }

    protected ErrorMessage(ErrorMessage other) {
        super(other);
    }

    @Override
    protected ErrorMessage createClone() {
        return new ErrorMessage(this);
    }

    @Override
    public ErrorMessage addMessageArgs(Object... args) {
        return (ErrorMessage) super.addMessageArgs(args);
    }
}