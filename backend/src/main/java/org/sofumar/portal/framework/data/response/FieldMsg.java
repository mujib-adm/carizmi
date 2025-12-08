package org.sofumar.portal.framework.data.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.sofumar.portal.framework.data.msg.Message;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FieldMsg {
    private Message.Type type;
    private String message;
    private String fieldName;
}
