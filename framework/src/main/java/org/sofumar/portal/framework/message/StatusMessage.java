package org.sofumar.portal.framework.message;

/**
 * Represents a status message.
 */
public class StatusMessage extends Message {

    public StatusMessage(String messageID, String messageText) {
        super(MessageType.STATUS, messageID, messageText);
    }

    protected StatusMessage(StatusMessage other) {
        super(other);
    }

    @Override
    protected StatusMessage createClone() {
        return new StatusMessage(this);
    }

    @Override
    public StatusMessage addMessageArgs(Object... args) {
        return (StatusMessage) super.addMessageArgs(args);
    }
}