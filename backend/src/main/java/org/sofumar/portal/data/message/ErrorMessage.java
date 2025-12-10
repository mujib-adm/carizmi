package org.sofumar.portal.data.message;

import org.sofumar.portal.framework.data.msg.Message;
import org.springframework.http.HttpStatus;

public class ErrorMessage extends Message {

    public ErrorMessage(String messageID, String messageText) {
        super(Message.Type.ERROR, messageID, messageText);
    }

}
