package org.sofumar.portal.framework.message;

import lombok.Getter;
import java.text.MessageFormat;

/**
 * Base class for messages.
 */
@Getter
public class Message {

    private final Type type;
    private final String messageID;
    private final String messageText;
    private Object[] messageArgs;

    protected Message(Message other) {
        this.type = other.type;
        this.messageID = other.messageID;
        this.messageText = other.messageText;
        if (other.messageArgs != null) {
            this.messageArgs = new Object[other.messageArgs.length];
            System.arraycopy(other.messageArgs, 0, this.messageArgs, 0, other.messageArgs.length);
        }
    }

    protected Message(Type type, String messageID, String messageText) {
        this.type = type;
        this.messageID = messageID;
        this.messageText = messageText;
    }

    /**
     * Creates a clone of this message.
     * Override in subclasses to return the correct type.
     */
    protected Message createClone() {
        return new Message(this);
    }

    public Message addMessageArgs(Object... args) {
        Message cloneMessage = createClone();
        int currentArgsLength = (cloneMessage.messageArgs != null) ? cloneMessage.messageArgs.length : 0;
        int incomingArgsLength = (args != null) ? args.length : 0;
        Object[] newArgs = new Object[currentArgsLength + incomingArgsLength];

        if (cloneMessage.messageArgs != null) {
            System.arraycopy(cloneMessage.messageArgs, 0, newArgs, 0, cloneMessage.messageArgs.length);
        }

        if (args != null) {
            System.arraycopy(args, 0, newArgs, currentArgsLength, incomingArgsLength);
        }

        cloneMessage.messageArgs = newArgs;
        return cloneMessage;
    }

    public String getMessageString() {
        return this.messageArgs != null && this.messageArgs.length > 0
                ? MessageFormat.format(this.messageText, this.messageArgs)
                : this.messageText;
    }

    public enum Type {
        ERROR,
        WARNING,
        INFO,
        STATUS,
        SUCCESS,
        CONFIRMATION
    }
}