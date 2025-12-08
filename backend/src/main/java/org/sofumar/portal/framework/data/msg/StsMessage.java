package org.sofumar.portal.framework.data.msg;

public class StsMessage extends Message {

    public StsMessage(String messageID, String messageText) {
        super(Message.Type.STATUS, messageID, messageText);
    }

}
