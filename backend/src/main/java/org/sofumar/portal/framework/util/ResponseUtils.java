package org.sofumar.portal.framework.util;

import org.sofumar.portal.framework.data.msg.FieldMessage;
import org.sofumar.portal.framework.data.msg.Message;
import org.sofumar.portal.framework.data.response.FieldMsg;
import org.sofumar.portal.framework.data.response.GlobalMsg;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.sofumar.portal.framework.vo.ValueObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public class ResponseUtils {

    public static void populateResponseFromVO(GlobalResponse response, ValueObject vo) {
        List<GlobalMsg> globalMessages = new java.util.ArrayList<>();
        List<FieldMsg> fieldMessages = new java.util.ArrayList<>();

        // Populate global messages
        for (Message msg : vo.getGlobalMessages()) {
            globalMessages.add(new GlobalMsg(
                    msg.getType(),
                    msg.getMessageString()
            ));
        }
        // Populate field messages
        for (Map.Entry<String, List<FieldMessage>> entry : vo.getFieldMessages().entrySet()) {
            String fieldName = entry.getKey();
            for (FieldMessage fieldMsg : entry.getValue()) {
                fieldMessages.add(new FieldMsg(
                        fieldMsg.getMessage().getType(),
                        fieldMsg.getMessage().getMessageString(),
                        fieldName
                ));
            }
        }
        response.setGlobalMessages(globalMessages);
        response.setFieldMessages(fieldMessages);
    }

    public static ResponseEntity<GlobalResponse> ok(Message.Type type, String msg) {
        GlobalResponse response = new GlobalResponse();
        response.setGlobalMessages(List.of(new GlobalMsg(type, msg)));
        return ResponseEntity.ok().body(response);
    }

    public static ResponseEntity<GlobalResponse> withStatus(HttpStatus status, GlobalResponse response) {
        return ResponseEntity.status(status).body(response);
    }

    public static ResponseEntity<GlobalResponse> withStatus(HttpStatus status, Message.Type type, String msg) {
        GlobalResponse response = new GlobalResponse();
        response.setStatusCode(status.value());
        response.setStatusDesc(status.getReasonPhrase());
        response.setGlobalMessages(List.of(new GlobalMsg(type, msg)));
        return ResponseEntity.status(status).body(response);
    }

    public static ResponseEntity<GlobalResponse> withMap(Map<String, String> map) {
        GlobalResponse response = new GlobalResponse();
        response.setStatusCode(HttpStatus.OK.value());
        response.setStatusDesc(HttpStatus.OK.getReasonPhrase());
        response.setMap(map);
        return ResponseEntity.ok(response);
    }
}
