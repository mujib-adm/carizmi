package io.carizmi.framework.message;

import lombok.Getter;

/**
 * Associates a field name with a message.
 */
@Getter
public class FieldMessage {
    private final String field;
    private final Message message;

    public FieldMessage(String field, Message message) {
        this.field = field;
        this.message = message;
    }

    public String getMessageString() {
        return this.field + ": " + this.message.getMessageString();
    }
}