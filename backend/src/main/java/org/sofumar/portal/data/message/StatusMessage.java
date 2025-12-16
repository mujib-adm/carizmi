package org.sofumar.portal.data.message;

import org.sofumar.portal.framework.data.msg.Message;

public class StatusMessage extends Message {

    public StatusMessage(String messageID, String messageText) {
        super(Type.STATUS, messageID, messageText);
    }

}