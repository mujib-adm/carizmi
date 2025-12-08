package org.sofumar.portal.framework.data.msg;

import lombok.Getter;

@Getter
public class FieldMessage {
    public String fieldName;
    public Message message;

    public FieldMessage(String fieldName, Message message) {
        this.fieldName = fieldName;
        this.message = message;
    }

    public String getMessageString() {
        return this.fieldName + ": " + this.message.getMessageString();
    }
}
