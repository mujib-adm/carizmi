package org.sofumar.portal.framework.data.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.sofumar.portal.framework.message.MessageType;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Field-level validation message")
public class FieldMsg {
    @Schema(description = "Message severity type")
    private MessageType type;
    @Schema(description = "Field name that the message applies to", example = "email")
    private String field;
    @Schema(description = "Validation message text", example = "Email is required.")
    private String message;
}