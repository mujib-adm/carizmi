package org.sofumar.portal.framework.data.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.sofumar.portal.framework.message.MessageType;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Unified API response envelope")
public class GlobalResponse<T> {
    @Schema(description = "HTTP status code", example = "200")
    private int statusCode;
    @Schema(description = "HTTP status description", example = "OK")
    private String statusDesc;
    @Schema(description = "Global-level messages (errors, warnings, info)")
    private List<GlobalMsg> globalMessages;
    @Schema(description = "Field-level validation messages")
    private List<FieldMsg> fieldMessages;
    @Schema(description = "Response payload")
    private T responseData;
    @Schema(description = "Pagination metadata")
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
        response.setGlobalMessages(List.of(new GlobalMsg(MessageType.SUCCESS, msg)));
        return response;
    }

    public static <T> GlobalResponse<T> error(String msg) {
        GlobalResponse<T> response = new GlobalResponse<>();
        response.setStatusCode(500);
        response.setStatusDesc("Internal Server Error");
        response.setGlobalMessages(List.of(new GlobalMsg(MessageType.ERROR, msg)));
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