package org.sofumar.portal.framework.data.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.sofumar.portal.framework.data.msg.Message;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GlobalResponse<T> {
    private int statusCode; // HTTP status code
    private String statusDesc;
    private List<GlobalMsg> globalMessages;
    private List<FieldMsg> fieldMessages;
    private Map<String, String> map;
    private T responseData;
    private PaginationMeta meta;

    public static <T> GlobalResponse<T> getInstance() {
        return new GlobalResponse<>();
    }

    public void setOkStatus() {
        this.statusCode = 200;
        this.statusDesc = "OK";
    }

    public static <T> GlobalResponse<T> ok(String msg) {
        GlobalResponse<T> response = new GlobalResponse<>();
        response.setOkStatus();
        response.setGlobalMessages(List.of(new GlobalMsg(Message.Type.SUCCESS, msg)));
        return response;
    }

    public static <T> GlobalResponse<T> error(String msg) {
        GlobalResponse<T> response = new GlobalResponse<>();
        response.setStatusCode(500);
        response.setStatusDesc("Internal Server Error");
        response.setGlobalMessages(List.of(new GlobalMsg(Message.Type.ERROR, msg)));
        return response;
    }

    public static <T> GlobalResponse<T> withResponseData(T data) {
        GlobalResponse<T> response = new GlobalResponse<>();
        response.setOkStatus();
        response.setResponseData(data);
        return response;
    }

    public static <T> GlobalResponse<T> withResponseDataPageable(T data, PaginationMeta meta) {
        GlobalResponse<T> response = new GlobalResponse<>();
        response.setOkStatus();
        response.setResponseData(data);
        response.setMeta(meta);
        return response;
    }
}
