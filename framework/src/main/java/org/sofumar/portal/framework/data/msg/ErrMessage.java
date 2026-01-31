package org.sofumar.portal.framework.data.msg;

public class ErrMessage extends Message {

    public ErrMessage(String messageID, String messageText) {
        super(Message.Type.ERROR, messageID, messageText);
    }

}