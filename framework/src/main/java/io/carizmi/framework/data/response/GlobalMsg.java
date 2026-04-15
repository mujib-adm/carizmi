package io.carizmi.framework.data.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import io.carizmi.framework.message.MessageType;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Global-level message")
public class GlobalMsg {
    @Schema(description = "Message severity type")
    private MessageType type;
    @Schema(description = "Message text", example = "Operation completed successfully.")
    private String message;
}