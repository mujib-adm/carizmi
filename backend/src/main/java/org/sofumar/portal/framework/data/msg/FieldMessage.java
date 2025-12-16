package org.sofumar.portal.framework.data.msg;

import lombok.Getter;

@Getter
public class FieldMessage {
    public String field;
    public Message message;

    public FieldMessage(String field, Message message) {
        this.field = field;
        this.message = message;
    }

    public String getMessageString() {
        return this.field + ": " + this.message.getMessageString();
    }
}
