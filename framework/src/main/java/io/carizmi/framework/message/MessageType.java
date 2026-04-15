package io.carizmi.framework.message;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Message severity type", enumAsRef = true)
public enum MessageType {
    ERROR,
    WARNING,
    INFO,
    STATUS,
    SUCCESS,
    CONFIRMATION
}