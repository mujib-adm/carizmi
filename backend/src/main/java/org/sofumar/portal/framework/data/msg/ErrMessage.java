package org.sofumar.portal.framework.data.msg;

//import org.springframework.http.HttpStatus;

public class ErrMessage extends Message {

    public ErrMessage(String messageID, String messageText) {
        super(Message.Type.ERROR, messageID, messageText);
    }

//    public ErrMessage(String messageID, String messageText, HttpStatus httpStatus) {
//        super(Message.Type.ERROR, messageID, messageText, httpStatus);
//    }
}
