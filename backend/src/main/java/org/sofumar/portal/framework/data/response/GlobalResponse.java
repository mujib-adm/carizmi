package org.sofumar.portal.framework.data.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GlobalResponse {
    private int statusCode; // HTTP status code
    private String statusDesc;
    private List<GlobalMsg> globalMessages;
    private List<FieldMsg> fieldMessages;
    private Map<String, String> map;
}
