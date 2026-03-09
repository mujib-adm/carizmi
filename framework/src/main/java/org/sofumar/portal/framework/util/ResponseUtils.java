package org.sofumar.portal.framework.util;

import org.sofumar.portal.framework.message.FieldMessage;
import org.sofumar.portal.framework.message.Message;
import org.sofumar.portal.framework.data.response.FieldMsg;
import org.sofumar.portal.framework.data.response.GlobalMsg;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.sofumar.portal.framework.data.response.PaginationMeta;
import org.sofumar.portal.framework.message.constant.CommonMessages;
import org.sofumar.portal.framework.vo.ValueObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public class ResponseUtils {

    public static void populateResponseFromVO(GlobalResponse<Void> response, ValueObject vo) {
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
            String field = entry.getKey();
            for (FieldMessage fieldMsg : entry.getValue()) {
                fieldMessages.add(new FieldMsg(
                        fieldMsg.getMessage().getType(),
                        field,
                        fieldMsg.getMessage().getMessageString()
                ));
            }
        }
        response.setGlobalMessages(globalMessages);
        response.setFieldMessages(fieldMessages);
    }

    public static ResponseEntity<GlobalResponse<Void>> ok(String msg) {
        return ResponseEntity.ok().body(GlobalResponse.ok(msg));
    }

    public static ResponseEntity<GlobalResponse<Void>> badRequest(String msg) {
        return ResponseEntity.badRequest().body(GlobalResponse.error(msg));
    }

    public static ResponseEntity<GlobalResponse<Void>> notFound(String msg) {
        return withStatus(HttpStatus.NOT_FOUND, GlobalResponse.error(msg));
    }

    public static ResponseEntity<GlobalResponse<Void>> withStatus(HttpStatus status, GlobalResponse<Void> response) {
        if (response.getStatusCode() == 0) {
            response.setStatusCode(status.value());
            response.setStatusDesc(status.getReasonPhrase());
        }
        return ResponseEntity.status(status).body(response);
    }

    public static ResponseEntity<GlobalResponse<Void>> withStatus(HttpStatus status, Message.Type type, String msg) {
        return withStatusAndData(status, type, msg);
    }

    public static <T> ResponseEntity<GlobalResponse<T>> withStatusAndData(HttpStatus status, Message.Type type, String msg) {
        GlobalResponse<T> response = GlobalResponse.getInstance();
        response.setStatusCode(status.value());
        response.setStatusDesc(status.getReasonPhrase());
        response.setGlobalMessages(List.of(new GlobalMsg(type, msg)));
        return ResponseEntity.status(status).body(response);
    }

    public static ResponseEntity<GlobalResponse<Void>> withMap(Map<String, String> map) {
        GlobalResponse<Void> response = GlobalResponse.getInstance();
        response.setStatusCode(HttpStatus.OK.value());
        response.setStatusDesc(HttpStatus.OK.getReasonPhrase());
        response.setMap(map);
        return ResponseEntity.ok(response);
    }

    public static <T> ResponseEntity<GlobalResponse<T>> okWithData(T data) {
        GlobalResponse<T> response = GlobalResponse.withResponseData(data);
        return ResponseEntity.ok(response);
    }

    public static <T> ResponseEntity<GlobalResponse<T>> okWithDataPageable(T data, PaginationMeta meta) {
        GlobalResponse<T> response = GlobalResponse.withResponseDataPageable(data, meta);
        return ResponseEntity.ok(response);
    }

    public static <T> ResponseEntity<GlobalResponse<T>> okWithData(T data, String msg) {
        GlobalResponse<T> response = GlobalResponse.withResponseData(data);
        response.setGlobalMessages(List.of(new GlobalMsg(Message.Type.SUCCESS, msg)));
        return ResponseEntity.ok(response);
    }

    public static <T> ResponseEntity<GlobalResponse<T>> badRequestWithData(String msg) {
        return ResponseEntity.badRequest().body(GlobalResponse.error(msg));
    }

    public static ResponseEntity<GlobalResponse<Void>> accessDenied() {
        return withStatus(HttpStatus.FORBIDDEN, accessDeniedResp());
    }

    public static GlobalResponse<Void> accessDeniedResp() {
        GlobalResponse<Void> response = GlobalResponse.getInstance();
        response.setStatusCode(HttpStatus.FORBIDDEN.value());
        response.setStatusDesc(HttpStatus.FORBIDDEN.getReasonPhrase());
        response.setGlobalMessages(List.of(new GlobalMsg(Message.Type.ERROR, CommonMessages.ACCESS_DENIED.getMessageString())));
        return response;
    }

    public static ResponseEntity<GlobalResponse<Void>> unauthenticated() {
        return withStatus(HttpStatus.UNAUTHORIZED, unauthenticatedResp());
    }

    public static GlobalResponse<Void> unauthenticatedResp() {
        GlobalResponse<Void> response = GlobalResponse.getInstance();
        response.setStatusCode(HttpStatus.UNAUTHORIZED.value());
        response.setStatusDesc(HttpStatus.UNAUTHORIZED.getReasonPhrase());
        response.setGlobalMessages(List.of(new GlobalMsg(Message.Type.ERROR, CommonMessages.AUTHENTICATION_FAILED.getMessageString())));
        return response;
    }

    public static GlobalResponse<Void> unauthenticatedResp(String customMessage) {
        GlobalResponse<Void> response = GlobalResponse.getInstance();
        response.setStatusCode(HttpStatus.UNAUTHORIZED.value());
        response.setStatusDesc(HttpStatus.UNAUTHORIZED.getReasonPhrase());
        response.setGlobalMessages(List.of(new GlobalMsg(Message.Type.ERROR, customMessage)));
        return response;
    }
}